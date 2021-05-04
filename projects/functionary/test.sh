#!/bin/sh
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"taskName":"e16c","taskSemanticName":"E16C Compilation"}' \
  http://localhost:8080/commit_task
