#!/bin/bash
echo Submitting rate $1
curl -o- -XPUT --user admin:passsword123 --data @$1  http://127.0.0.1:8085/rates -H "Content-Type: application/json"
