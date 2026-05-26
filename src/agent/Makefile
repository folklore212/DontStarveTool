.PHONY: build test lint clean

BUILD_DIR = build

build:
	@mkdir -p $(BUILD_DIR)
	go build -ldflags="-s -w" -o $(BUILD_DIR)/agent-agent ./agent-agent/
	go build -ldflags="-s -w" -o $(BUILD_DIR)/agent-gateway ./agent-gateway/
	@echo "Built:"
	@ls -lh $(BUILD_DIR)/

test:
	go test ./...

lint:
	golangci-lint run ./...

clean:
	rm -rf $(BUILD_DIR)
