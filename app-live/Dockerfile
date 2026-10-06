# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY trading/build.gradle.kts ./trading/build.gradle.kts
COPY adapter-kis/build.gradle.kts ./adapter-kis/build.gradle.kts
COPY adapter-postgres/build.gradle.kts ./adapter-postgres/build.gradle.kts
COPY adapter-strategy/build.gradle.kts ./adapter-strategy/build.gradle.kts
COPY app-live/build.gradle.kts ./app-live/build.gradle.kts
COPY app-backtest/build.gradle.kts ./app-backtest/build.gradle.kts

RUN chmod +x gradlew

COPY trading/src ./trading/src
COPY adapter-kis/src ./adapter-kis/src
COPY adapter-postgres/src ./adapter-postgres/src
COPY adapter-strategy/src ./adapter-strategy/src
COPY app-live/src ./app-live/src

RUN ./gradlew :app-live:bootJar --no-daemon \
    && artifact="$(find app-live/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" \
    && test -n "${artifact}" \
    && cp "${artifact}" /workspace/app-live.jar

FROM eclipse-temurin:25-jre-noble AS runtime

RUN groupadd --system --gid 10001 autotrader \
    && useradd --system --uid 10001 --gid autotrader --home-dir /app --shell /usr/sbin/nologin autotrader \
    && mkdir -p /app /var/lib/autotrader/kis \
    && chown -R autotrader:autotrader /app /var/lib/autotrader

WORKDIR /app

COPY --from=build --chown=autotrader:autotrader /workspace/app-live.jar ./app-live.jar

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app-live.jar"]

