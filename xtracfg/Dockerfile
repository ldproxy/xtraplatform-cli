FROM ghcr.io/ldproxy/ni-base:11
ARG TARGETOS
ARG TARGETARCH
COPY ./$TARGETOS/$TARGETARCH/xtracfg /xtracfg
WORKDIR /src
ENTRYPOINT ["/xtracfg"]
