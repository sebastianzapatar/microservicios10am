# syntax=docker/dockerfile:1

# --- Etapa 1: compilación del binario Go ---------------------------------
FROM golang:1.27-alpine AS build
WORKDIR /src

# go.mod/go.sum primero: las dependencias se cachean aparte del código.
COPY go.mod go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod,id=go-mod-client \
    go mod download

COPY cmd cmd
COPY internal internal
RUN --mount=type=cache,target=/go/pkg/mod,id=go-mod-client \
    --mount=type=cache,target=/root/.cache/go-build,id=go-build-client \
    CGO_ENABLED=0 go build -trimpath -ldflags="-s -w" -o /out/client-service ./cmd/server

# --- Etapa 2: imagen de ejecución ----------------------------------------
FROM alpine:3.23 AS runtime
RUN apk add --no-cache ca-certificates curl \
 && adduser -D -u 10001 client
COPY --from=build /out/client-service /usr/local/bin/client-service
USER client
EXPOSE 8083
ENTRYPOINT ["/usr/local/bin/client-service"]
