#!/bin/bash
echo Getting all invoicing-data
curl -o- --user accountant:welkom01 http://127.0.0.1:8085/invoices\?interval=2019-11-01T00:00Z/2019-12-27T23:59:59Z -H "Content-Type: application/json"
