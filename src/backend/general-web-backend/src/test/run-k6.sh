#!/bin/bash
# run-k6.sh — Helper script to execute the k6 performance test
k6 run --out json=results.json test-perf.js
