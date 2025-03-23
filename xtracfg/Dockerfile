FROM cgr.dev/chainguard/glibc-dynamic:latest
ARG TARGETOS
ARG TARGETARCH
COPY --chmod=0777 ./xtracfg-$TARGETOS-$TARGETARCH/xtracfg /xtracfg
WORKDIR /src
ENTRYPOINT ["/xtracfg"]
