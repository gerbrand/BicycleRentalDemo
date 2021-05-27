#!/bin/bash
echo Submitting session $1
curl -o- -XPUT  --user bookkeeper:welkom01 --data @$1  http://127.0.0.1:8085/sessions -H "Content-Type: application/json"