#!/bin/bash
echo Querying rate $1
curl -o- --user admin:passsword123  http://127.0.0.1:8085/rates\?date=2019-12-06T12:17:20.905+01:00\&rateType=R -H "Content-Type: application/json"
