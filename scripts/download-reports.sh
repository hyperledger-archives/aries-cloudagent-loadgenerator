#!/bin/bash

FROM_TIMESTAMP=$1
TO_TIMESTAMP=$2

REPORT_FOLDER=./reports

rm -rf $REPORT_FOLDER
mkdir $REPORT_FOLDER

MAX_DOWNLOAD_INTERVAL_IN_HOURS=6
MAX_DOWNLOAD_INTERVAL_IN_MS=$(expr $MAX_DOWNLOAD_INTERVAL_IN_HOURS \* 1000 \* 60 \* 60)

FROM_TIMESTAMPS=()
TO_TIMESTAMPS=()
FILE_POSTFIX=()

FROM_TIMESTAMPS+=($FROM_TIMESTAMP)

while [ $(expr $TO_TIMESTAMP - ${FROM_TIMESTAMPS[${#FROM_TIMESTAMPS[@]} - 1]}) -gt $MAX_DOWNLOAD_INTERVAL_IN_MS ]
do
    TIMESTAMP=$(expr ${FROM_TIMESTAMPS[${#FROM_TIMESTAMPS[@]} - 1]} + $MAX_DOWNLOAD_INTERVAL_IN_MS)
    TO_TIMESTAMPS+=($TIMESTAMP)
    FROM_TIMESTAMPS+=($TIMESTAMP)
    FILE_POSTFIX+=("$(expr $(expr ${#FROM_TIMESTAMPS[@]} - 2) \* $MAX_DOWNLOAD_INTERVAL_IN_HOURS)-$(expr $(expr ${#FROM_TIMESTAMPS[@]} - 1) \* $MAX_DOWNLOAD_INTERVAL_IN_HOURS)")
done

TO_TIMESTAMPS+=($TO_TIMESTAMP)
FILE_POSTFIX+=("$(expr $(expr ${#FROM_TIMESTAMPS[@]} - 1) \* $MAX_DOWNLOAD_INTERVAL_IN_HOURS)-END")

echo Download split into ${#FILE_POSTFIX[@]} parts...

for i in $(seq 0 $(expr ${#FILE_POSTFIX[@]} - 1))
do
    echo Downloading Part $(expr $i + 1): ${FILE_POSTFIX[$i]} hours FROM=${FROM_TIMESTAMPS[$i]} TO=${TO_TIMESTAMPS[$i]}

    echo ">> Exporting Test Results Dashboard..."
    wget -q -o /dev/null -O "$REPORT_FOLDER/report-test-results-${FILE_POSTFIX[$i]}.pdf" "http://localhost:8686/api/v5/report/0Pe9llbnz?from=${FROM_TIMESTAMPS[$i]}&to=${TO_TIMESTAMPS[$i]}"

    echo ">> Exporting Issuer/Verifier Postgres Metrics Dashboard..."
    wget -q -o /dev/null -O "$REPORT_FOLDER/report-issuer-verifier-postgres-metrics-${FILE_POSTFIX[$i]}.pdf" "http://localhost:8686/api/v5/report/5ZAfbNf7k?from=${FROM_TIMESTAMPS[$i]}&to=${TO_TIMESTAMPS[$i]}"

    echo ">> Exporting Full Docker Container Metrics Dashboard..."
    wget -q -o /dev/null -O "$REPORT_FOLDER/report-docker-container-metrics-${FILE_POSTFIX[$i]}.pdf" "http://localhost:8686/api/v5/report/pMEd7m0Mz?from=${FROM_TIMESTAMPS[$i]}&to=${TO_TIMESTAMPS[$i]}"

    echo ">> Exporting Host Metrics Dashboard..."
    wget -q -o /dev/null -O "$REPORT_FOLDER/report-host-metrics-${FILE_POSTFIX[$i]}.pdf" "http://localhost:8686/api/v5/report/rYdddlPWk?from=${FROM_TIMESTAMPS[$i]}&to=${TO_TIMESTAMPS[$i]}"

done

echo "Please find all downloaded dashboard in $REPORT_FOLDER"
