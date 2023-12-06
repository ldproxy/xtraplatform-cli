FROM ghcr.io/ldproxy/ni-base:11
ARG TARGETOS
ARG TARGETARCH
COPY ./$TARGETOS-$TARGETARCH/xtractl /xtractl
WORKDIR /src
ENTRYPOINT ["/xtractl"]
