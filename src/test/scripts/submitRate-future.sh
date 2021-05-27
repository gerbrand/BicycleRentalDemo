#!/bin/bash
sourcedir=$(dirname ${BASH_SOURCE[0]})
echo $sourcedir
testdir=$sourcedir/..
$sourcedir/submitRate.sh ${testdir}/resources/rental-rate-future.json

