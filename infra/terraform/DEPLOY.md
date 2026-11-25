# AWS 배포 가이드

AutoTrader를 AWS에 처음 배포하고, 이후 새 버전을 배포할 때 사용하는 절차입니다.
Terraform 작업 디렉터리는 `infra/terraform`, 리전은 `ap-northeast-2`입니다. GitHub Actions는
이미지를 게시하고, Terraform은 Task Definition과 AWS 인프라를 관리합니다. ECS Fargate
Task는 EventBridge Scheduler가 실행합니다.

첫 배포에서는 Scheduler를 비활성 상태로 생성합니다. 이미지와 비밀값을 준비하고 거래
캘린더를 한 번 갱신한 뒤에만 Scheduler를 활성화합니다.

## 1. 로컬 도구와 AWS 인증

```bash
brew install awscli
brew tap hashicorp/tap
brew install hashicorp/tap/terraform

aws configure sso
aws sts get-caller-identity
```

`get-caller-identity` 결과의 AWS 계정과 배포 대상 계정이 같은지 확인합니다. 장기 Access
Key는 사용하지 않습니다.

GitHub CLI도 인증합니다.

```bash
gh auth login -h github.com
```

## 2. Terraform state bucket 생성

운영 state를 저장할 S3 bucket을 먼저 만듭니다. 이 작업은 최초 배포에서 한 번만 합니다.

```bash
cd infra/state-bootstrap

terraform init
terraform fmt -check
terraform validate
terraform plan -out=state-backend.tfplan
terraform apply state-backend.tfplan
terraform output -raw state_bucket_name
```

생성되는 bucket에는 public access 차단, TLS가 아닌 요청 거부, 서버 측 암호화, versioning,
Terraform 삭제 방지가 적용됩니다.

`infra/state-bootstrap/terraform.tfstate`는 state bucket 자체를 관리하므로 삭제하지 않습니다.
파일을 잃어버린 경우 bucket을 다시 만들지 말고 Terraform import로 복구합니다.

운영 Terraform의 backend 설정 파일을 준비합니다.

```bash
cd ../terraform
cp -n backend.hcl.example backend.hcl
chmod 600 backend.hcl
```

`backend.hcl`의 bucket 값을 앞에서 출력된 이름으로 바꾼 후 초기화합니다.

```bash
terraform init -migrate-state -backend-config=backend.hcl
```

운영 state는 `autotrader/prod/terraform.tfstate`에 저장됩니다. S3 lock file을 사용하므로
동시에 두 번 apply하는 상황도 방지합니다.

## 3. Terraform 변수 준비

```bash
cp -n terraform.tfvars.example terraform.tfvars
chmod 600 terraform.tfvars
```

GitHub 저장소 ID와 소유자 ID를 조회해 OIDC subject를 만듭니다.

```bash
gh api repos/hamm0000/AutoTrader \
  --jq '"repo:\(.owner.login)@\(.owner.id)/\(.name)@\(.id):environment:production"'
```

출력값을 `terraform.tfvars`의 `github_oidc_subject`에 입력합니다. 기존 GitHub OIDC provider를
사용하는 계정이라면 ARN도 확인합니다.

```bash
aws iam list-open-id-connect-providers
```

최초 배포에서는 다음 상태를 유지합니다.

```hcl
schedules_enabled = false

# app_image_digest와 strategy_image_digest는 아직 설정하지 않습니다.
```

이 단계의 Task Definition은 임시 `bootstrap` tag를 가리키지만 Scheduler가 비활성 상태이므로
실행되지 않습니다.

## 4. 배포 계획 확인

```bash
terraform fmt -check -recursive
terraform validate
terraform test
terraform plan -out=bootstrap.tfplan
```

`fmt`는 파일 형식, `validate`는 Terraform 참조와 provider 설정, `test`는 Task별 Security
Group, KIS 포트, 실행 역할과 배포 안전장치를 확인합니다. `plan`은 실제 AWS 계정에서
생성되거나 변경되는 자원을 보여 줍니다.

`bootstrap.tfplan`에서 두 Scheduler가 `DISABLED`인지, RDS가 `db.t4g.micro`, Single-AZ,
20GB인지, paper Task와 calendar Task가 서로 다른 Security Group과 execution role을
사용하는지, 예상하지 않은 삭제 또는 교체가 없는지 확인합니다.

하나라도 다르면 apply하지 않습니다.

## 5. 초기 인프라 생성

```bash
terraform apply bootstrap.tfplan
```

VPC, RDS, ECR, ECS, IAM, CloudWatch Logs, Secrets Manager secret과 비활성 Scheduler가
생성됩니다. 이때부터 RDS 비용이 발생할 수 있습니다.

Fargate Task는 실행할 때만 public IP를 받습니다. Task Security Group에는 inbound 규칙이
없고, RDS는 public IP가 없는 private subnet에 배치됩니다.

## 6. 비밀값 입력

AWS Console의 Secrets Manager에서 아래 두 secret을 찾아 JSON key/value로 값을 입력합니다.
비밀값은 Terraform 변수나 명령행 인자로 전달하지 않습니다.

`autotrader-prod/paper-trading`:

```json
{
  "KIS_PAPER_APP_KEY": "...",
  "KIS_PAPER_APP_SECRET": "...",
  "KIS_PAPER_ACCOUNT_NUMBER": "...",
  "KIS_PAPER_ACCOUNT_PRODUCT_CODE": "01"
}
```

`autotrader-prod/calendar-refresh`:

```json
{
  "KIS_PROD_APP_KEY": "...",
  "KIS_PROD_APP_SECRET": "..."
}
```

calendar secret에는 계좌번호와 주문 관련 값을 넣지 않습니다. RDS 비밀번호는 RDS가 별도
secret으로 생성하고 관리합니다.

## 7. GitHub production 환경 설정

GitHub 저장소의 `Settings > Environments`에서 `production` 환경을 만들고 배포 승인 규칙을
설정합니다. Repository variable에는 다음 값을 등록합니다.

```text
AWS_RELEASE_ROLE_ARN     = terraform output -raw github_release_role_arn
AWS_REGION               = terraform output -raw aws_region
ECR_APP_REPOSITORY       = terraform output -raw ecr_app_repository_name
ECR_STRATEGY_REPOSITORY  = terraform output -raw ecr_strategy_repository_name
```

GitHub Actions 역할에는 두 ECR repository에 이미지를 게시하는 권한만 있습니다. ECS Task
Definition은 변경할 수 없습니다.

## 8. 첫 이미지 게시

변경 사항을 `main`에 병합한 후 `main` 커밋에 버전 태그를 push합니다.

```bash
git switch main
git pull --ff-only
git tag -a v0.1.0-rc.1 -m "v0.1.0-rc.1"
git push origin v0.1.0-rc.1
```

태그가 push되면 `.github/workflows/release.yml`이 태그가 `main` 이력에 포함됐는지 확인하고,
app-live와 zscore-strategy 이미지를 빌드합니다. 이어서 ECR에 release tag와 Git SHA tag를
게시하고, 두 이미지의 digest를 job summary에 출력합니다.

워크플로는 이미지만 게시합니다. Task Definition 변경은 Terraform에서 수행합니다.

## 9. 이미지 digest 반영

GitHub Actions job summary에 출력된 값을 `terraform.tfvars`에 입력합니다.

```hcl
schedules_enabled     = false
app_image_digest      = "sha256:<app-live digest>"
strategy_image_digest = "sha256:<zscore-strategy digest>"
```

두 digest는 항상 함께 변경해야 합니다.

```bash
terraform plan -out=release-images.tfplan
terraform apply release-images.tfplan
```

plan에서 paper Task Definition의 app-live와 zscore-strategy 이미지, calendar Task
Definition의 app-live 이미지만 변경되는지 확인합니다. Scheduler는 계속 `DISABLED`여야 합니다.

## 10. 거래 캘린더 최초 갱신

초기 RDS에는 거래 캘린더가 없습니다. Scheduler를 켜기 전에 calendar Task를 AWS에서 한 번
직접 실행합니다. 로컬 PostgreSQL의 데이터는 RDS로 복사되지 않습니다.

```bash
CLUSTER="$(terraform output -raw ecs_cluster_name)"
CALENDAR_FAMILY="$(terraform output -raw calendar_task_family)"
PUBLIC_SUBNETS="$(terraform output -json public_subnet_ids | jq -r 'join(",")')"
CALENDAR_SECURITY_GROUP="$(terraform output -raw calendar_task_security_group_id)"

CALENDAR_TASK_ARN="$(aws ecs run-task \
  --cluster "$CLUSTER" \
  --task-definition "$CALENDAR_FAMILY" \
  --launch-type FARGATE \
  --platform-version 1.4.0 \
  --network-configuration \
    "awsvpcConfiguration={subnets=[$PUBLIC_SUBNETS],securityGroups=[$CALENDAR_SECURITY_GROUP],assignPublicIp=ENABLED}" \
  --query 'tasks[0].taskArn' \
  --output text)"
```

`CALENDAR_TASK_ARN`이 `None`이면 Scheduler를 활성화하지 않습니다. 같은 명령에 `--query`를
제거하고 다시 실행해 `failures` 내용을 확인합니다.

Task가 끝날 때까지 기다린 뒤 종료 코드와 로그를 확인합니다.

```bash
aws ecs wait tasks-stopped \
  --cluster "$CLUSTER" \
  --tasks "$CALENDAR_TASK_ARN"

aws ecs describe-tasks \
  --cluster "$CLUSTER" \
  --tasks "$CALENDAR_TASK_ARN" \
  --query 'tasks[0].containers[].{name:name,exitCode:exitCode,reason:reason}'

aws logs tail "$(terraform output -raw calendar_log_group_name)" --since 10m
```

컨테이너 exit code가 `0`인지, 로그에 캘린더 갱신 완료가 기록됐는지, 인증 오류나 DB 오류가
없는지 확인합니다.

현재 RDS는 외부에서 직접 접속할 수 없으므로 첫 배포 확인은 Task 종료 코드와 로그를
기준으로 합니다. 실패하면 Scheduler를 활성화하지 않습니다.

## 11. Scheduler 활성화

캘린더 최초 갱신이 성공한 후 `terraform.tfvars`를 변경합니다.

```hcl
schedules_enabled = true
```

```bash
terraform plan -out=enable-schedules.tfplan
terraform apply enable-schedules.tfplan
terraform output -raw scheduler_state
```

plan에는 두 Scheduler가 `DISABLED`에서 `ENABLED`로 바뀌는 내용만 나타나야 합니다. 마지막
출력이 `ENABLED`인지 확인합니다. paper trading은 평일 09:10 KST, calendar refresh는
목요일 16:10 KST에 실행됩니다.

이미지 digest가 빠져 있으면 Terraform이 Scheduler 활성화를 거부합니다.

## 12. 배포 후 확인

```bash
aws logs tail "$(terraform output -raw paper_log_group_name)" --since 1h

aws sqs get-queue-attributes \
  --queue-url "$(aws sqs get-queue-url \
    --queue-name autotrader-prod-scheduler-dlq \
    --query QueueUrl \
    --output text)" \
  --attribute-names ApproximateNumberOfMessages
```

첫 명령은 paper Task 로그를 확인합니다. 두 번째 명령의
`ApproximateNumberOfMessages`가 `0`이면 Scheduler가 전달하지 못한 요청이 없는 상태입니다.

DLQ에는 Scheduler가 ECS `RunTask` 요청을 전달하지 못한 경우만 기록됩니다. 컨테이너 시작
실패나 애플리케이션 오류는 ECS Task 상태와 CloudWatch Logs에서 확인합니다.

### Task 종료 이벤트 수집 최초 확인

Task 종료 이벤트 수집을 처음 구성한 뒤에는 다음 예약 실행이 끝나면 로그가 기록되는지
확인합니다. 정상 실행의 컨테이너별 종료 코드를 확인하고 SNS 알림 조건을 정하기 위한
절차입니다. 확인을 위해 paper Task를 별도로 실행하지 않습니다.

```bash
aws logs tail "$(terraform output -raw task_stopped_log_group_name)" --since 24h
```

Task ARN으로 실행을 구분하고 `detail.containers`에서 컨테이너 이름과 종료 코드를
확인합니다. paper Task는 `app-live`와 `zscore-strategy`, calendar Task는
`market-calendar-refresh`를 확인합니다.

컨테이너가 시작하지 못한 경우 종료 코드가 없을 수 있습니다. 이때는 0으로 간주하지 않고
`detail.stoppedReason`과 컨테이너의 `reason`을 확인합니다. 앱 로그는 Task ARN 끝의
Task ID로 `ecs/<컨테이너 이름>/<Task ID>` 스트림을 찾아 확인합니다.

## 이후 버전 배포

최초 배포가 끝난 뒤에는 state bucket, secret, 캘린더 bootstrap을 다시 만들 필요가 없습니다.
`main` 커밋에 새 버전 태그를 push하고, GitHub Actions에서 이미지 게시 성공을 확인합니다.
이후 job summary의 두 digest를 `terraform.tfvars`에 입력하고, `terraform plan`에서 Task
Definition 변경만 확인한 뒤 apply합니다.

Scheduler는 revision 없는 Task Definition family ARN을 사용하므로 다음 실행부터 새 revision을
사용합니다.

이전 버전으로 되돌릴 때는 이전 배포의 두 digest를 `terraform.tfvars`에 다시 입력하고
Terraform plan과 apply를 실행합니다.
