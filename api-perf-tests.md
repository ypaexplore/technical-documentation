Creating a performance test script in Bash requires careful handling of concurrency and token lifecycles. Below is an exhaustive **Performance & Load Testing Script** that automates the OAuth2 token renewal, generates random dynamic data for each request, and tracks basic performance metrics (response times and status codes).

---

# 1. API Performance & Load Test Script (`api_perf_test.sh`)

This script uses a **Background Token Renewer** to ensure that no matter how long your test runs, the `ACCESS_TOKEN` is always valid.

```bash
#!/bin/bash

# ==============================================================================
# 1. CONFIGURATION
# ==============================================================================
TOKEN_URL="https://auth.yourdomain.com/oauth2/token"
API_URL="https://api.yourdomain.com/v1/resource"
CLIENT_ID="your_client_id"
CLIENT_SECRET="your_client_secret"

# Test Parameters
TOTAL_REQUESTS=100
CONCURRENCY=10 # Number of parallel processes
TOKEN_RENEW_INTERVAL=300 # Renew token every 5 minutes (300s)

# File to store the shared token
TOKEN_FILE="/tmp/api_access_token"

# ==============================================================================
# 2. AUTOMATED TOKEN RENEWAL (BACKGROUND PROCESS)
# ==============================================================================
renew_token() {
    while true; do
        RESPONSE=$(curl -s -X POST "$TOKEN_URL" \
            -u "$CLIENT_ID:$CLIENT_SECRET" \
            -d "grant_type=client_credentials")
        
        NEW_TOKEN=$(echo "$RESPONSE" | jq -r '.access_token')
        
        if [[ "$NEW_TOKEN" != "null" ]]; then
            echo "$NEW_TOKEN" > "$TOKEN_FILE"
            # echo "[$(date +%T)] Token Renewed." # Debug
        else
            echo "Error: Token renewal failed!"
        fi
        sleep "$TOKEN_RENEW_INTERVAL"
    done
}

# Start token renewal in the background
renew_token &
RENEW_PID=$!

# Trap to kill the background renewal process on script exit
trap "kill $RENEW_PID; exit" SIGINT SIGTERM EXIT

# Wait a moment for the first token to be generated
sleep 2

# ==============================================================================
# 3. DYNAMIC DATA GENERATOR
# ==============================================================================
generate_data() {
    # Generates a random JSON payload for each call
    local id=$1
    local random_val=$((RANDOM % 1000))
    local timestamp=$(date +%s)
    
    cat <<EOF
{
    "request_id": "$id",
    "user_id": "user_$random_val",
    "payload": "perf_test_data_$timestamp",
    "priority": $(( (RANDOM % 5) + 1 ))
}
EOF
}

# ==============================================================================
# 4. EXECUTION LOOP
# ==============================================================================
echo -e "\nStarting Performance Test: $TOTAL_REQUESTS requests ($CONCURRENCY parallel)"
echo "------------------------------------------------------------"

start_time=$(date +%s.%N)

# Function to perform a single API call
make_call() {
    local id=$1
    local token=$(cat "$TOKEN_FILE")
    local data=$(generate_data "$id")
    
    # Capture HTTP status and time taken
    res=$(curl -s -o /dev/null -w "%{http_code} %{time_total}" -X POST "$API_URL" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$data")
    
    echo "$res" >> /tmp/perf_results.txt
}

# Run requests in parallel using xargs or a simple loop
# Here we use a loop with background processes for simplicity
for ((i=1; i<=TOTAL_REQUESTS; i++)); do
    make_call "$i" &
    
    # Control concurrency
    if [[ $(jobs -r | wc -l) -ge $CONCURRENCY ]]; then
        wait -n
    fi
done

wait # Wait for final background jobs to finish

end_time=$(date +%s.%N)

# ==============================================================================
# 5. PERFORMANCE SUMMARY
# ==============================================================================
duration=$(echo "$end_time - start_time" | bc)
success_count=$(grep -c "200" /tmp/perf_results.txt || echo 0)
error_count=$(grep -v "200" /tmp/perf_results.txt | wc -l)
avg_latency=$(awk '{ sum += $2; n++ } END { if (n > 0) print sum / n; else print 0 }' /tmp/perf_results.txt)

echo "Test Results:"
echo "Total Time:       $duration seconds"
echo "Success (200 OK): $success_count"
echo "Errors/Others:    $error_count"
echo "Avg Latency:      $avg_latency seconds"

rm /tmp/perf_results.txt

```

---

# 2. Performance Testing Guide (Detailed)

### A. How Automated Renewal Works

The script uses a **Subshell** (`renew_token &`). This process runs independently in the background. It writes the latest token to a temporary file (`/tmp/api_access_token`).
Every time the `Phone` function runs, it reads the **most current** token from that file. This is crucial for performance tests that run for hours.

### B. Automated Data Generation

Instead of sending the same "static" data (which might be cached by the server and give false performance results), the `generate_data` function uses:

* `$RANDOM`: To generate different `user_id` values.
* `date +%s`: To ensure the payload content is unique per second.
* Arithmetic expansion `$((...))`: To vary numeric fields like "priority."

### C. Concurrency Control

The script manages load using `jobs -r | wc -l`. If the number of running background tasks reaches your `$CONCURRENCY` limit, the script waits for one to finish (`wait -n`) before starting the next. This prevents your local machine from crashing while trying to hammer the server.

---

### D. Key Metrics Explained

| Metric | Description |
| --- | --- |
| **Throughput** | Calculated as `Total Requests / Total Time`. It tells you how many requests your API handles per second (RPS). |
| **Latency** | The `%{time_total}` from `curl`. This is the time from the start of the request until the last byte was received. |
| **Http Code** | Essential for identifying when the server starts failing (e.g., 504 Gateway Timeout or 429 Too Many Requests). |

### E. Prerequisites

To run this script, ensure you have these installed:

1. `jq`: For parsing the OAuth2 JSON response.
2. `bc`: For performing floating-point math for the time calculations.
3. `curl`: The engine for the API calls.

**Next Step:** Would you like me to show you how to output these results into a **CSV format** so you can easily import them into Excel or Grafana for charting?
