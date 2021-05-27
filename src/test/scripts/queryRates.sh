#!/bin/bash
echo Retrieving all rates
curl -o- --user admin:passsword123 http://127.0.0.1:8085/rates?rateType=E -H "Content-Type: application/json"
echo
