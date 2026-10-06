# syntax=docker/dockerfile:1

FROM debian:bookworm-20260824-slim AS build

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        cmake \
        g++ \
        git \
        make \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

COPY strategy-zscore ./strategy-zscore

RUN cmake \
        -S strategy-zscore \
        -B strategy-zscore/build \
        -DCMAKE_BUILD_TYPE=Release \
        -DAUTOTRADER_STRATEGY_BUILD_TESTS=OFF \
    && cmake \
        --build strategy-zscore/build \
        --target zscore_server \
        --config Release \
        --parallel 2

FROM debian:bookworm-20260824-slim AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
        ca-certificates \
        curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 autotrader \
    && useradd --system --uid 10001 --gid autotrader --no-create-home --home-dir /nonexistent --shell /usr/sbin/nologin autotrader

COPY --from=build /workspace/strategy-zscore/build/zscore_server /usr/local/bin/zscore_server

USER 10001:10001

EXPOSE 9000

HEALTHCHECK --interval=5s --timeout=2s --start-period=2s --retries=10 \
    CMD ["curl", "--fail", "--silent", "--show-error", "--max-time", "1", "http://127.0.0.1:9000/health"]

ENTRYPOINT ["/usr/local/bin/zscore_server"]
