FROM golang:1.27-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o /engram-server ./cmd/engram-server

FROM gcr.io/distroless/static-debian12
COPY --from=build /engram-server /engram-server
EXPOSE 8080
ENTRYPOINT ["/engram-server"]
