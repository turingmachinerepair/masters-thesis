#!/bin/sh
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"taskName":"sample_project","taskSemanticName":"test ticket","footnote":"footnote"}' \
  http://localhost:8080/commit_task
