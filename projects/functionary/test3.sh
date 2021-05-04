#!/bin/sh
curl --header "Content-Type: application/json" \
  --request POST \
  --data "12-e16c-[FUjHCdsFtW-xmu-14, FUjHCdsFtW-hmu-15, FUjHCdsFtW-xmu_com-16, FUjHCdsFtW-tile-13, FUjHCdsFtW-eioh-17]" \
  http://localhost:8080/task_status
