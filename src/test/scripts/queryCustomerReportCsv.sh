#!/bin/bash
echo Getting report for a cyclist
curl -o- http://127.0.0.1:8085/reports\?cyclist=e51b918c-7f35-42c9-a6dc-be7c967f82a2\&interval=2019-11-01T00:00Z/2019-12-31T23:59:59Z
