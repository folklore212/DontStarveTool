FROM golang:1.22-alpine AS builder
WORKDIR /build
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o node-gateway ./node-gateway/

FROM alpine:3.20
RUN apk add --no-cache ca-certificates
RUN adduser -D -g '' node
COPY --from=builder /build/node-gateway /app/node-gateway
USER node
EXPOSE 8090
ENTRYPOINT ["/app/node-gateway"]
