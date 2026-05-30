FROM golang:1.22-alpine AS build
WORKDIR /build
COPY src/agent/go.mod src/agent/go.sum ./
RUN go mod download
COPY src/agent/ .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o node-gateway ./node-gateway/

FROM alpine:3.20
RUN apk add --no-cache ca-certificates curl
RUN adduser -D -g '' node
COPY --from=build /build/node-gateway /app/node-gateway
USER node
EXPOSE 8090
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=10s \
  CMD curl -sf http://localhost:8090/health || exit 1
ENTRYPOINT ["/app/node-gateway"]
