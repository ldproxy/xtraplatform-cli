FROM cgr.dev/chainguard/glibc-dynamic:latest
ARG TARGETOS
ARG TARGETARCH
COPY ./$TARGETOS-$TARGETARCH/xtractl /xtractl
WORKDIR /src
ENTRYPOINT ["/xtractl"]
