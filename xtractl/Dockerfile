FROM alpine
# FROM ghcr.io/ldproxy/ni-base:11
ARG TARGETOS
ARG TARGETARCH
COPY ./$TARGETOS-$TARGETARCH/xtractl /xtractl
WORKDIR /src
RUN ls -lR /
ENTRYPOINT ["/xtractl"]
