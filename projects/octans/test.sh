#!/bin/sh
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"taskName":"e16c","semanticTaskName":"E16C Compilation"}' \
  http://localhost:8080/commit_task
