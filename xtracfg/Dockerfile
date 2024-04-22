FROM ghcr.io/ldproxy/ni-base:11
ARG TARGETOS
ARG TARGETARCH
COPY --chmod 0777 ./$TARGETOS/$TARGETARCH/xtracfg /xtracfg
WORKDIR /src
ENTRYPOINT ["/xtracfg"]
