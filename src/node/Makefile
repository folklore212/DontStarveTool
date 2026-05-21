.PHONY: build test lint clean

BUILD_DIR = build

build:
	@mkdir -p $(BUILD_DIR)
	go build -ldflags="-s -w" -o $(BUILD_DIR)/node-agent ./node-agent/
	go build -ldflags="-s -w" -o $(BUILD_DIR)/node-gateway ./node-gateway/
	@echo "Built:"
	@ls -lh $(BUILD_DIR)/

test:
	go test ./...

lint:
	golangci-lint run ./...

clean:
	rm -rf $(BUILD_DIR)
