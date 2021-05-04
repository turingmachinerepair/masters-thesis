#!/bin/sh
curl --header "Content-Type: application/json" \
  --request GET \
  http://localhost:8080/list_tasks
