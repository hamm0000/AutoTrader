output "state_bucket_name" {
  description = "Bucket to set in infra/terraform/backend.hcl."
  value       = aws_s3_bucket.terraform_state.id
}
