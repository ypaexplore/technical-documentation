Here is an **Automated Health Check & Performance Audit Script**. Instead of sending an email, this script generates a clean, structured **Terminal Output** and saves a **JSON Report** locally for your records.

---

# 1. Elasticsearch Automated Audit Script (`es_audit.sh`)

This script connects to your cluster, runs all the "Dev Tools" queries we discussed, and evaluates them against best-practice thresholds (e.g., Heap > 85%, Disk > 85%).

```bash
#!/bin/bash

# --- CONFIGURATION ---
ES_HOST="http://localhost:9200"
USER="elastic"
PASS="your_password"
OUTPUT_FILE="es_audit_$(date +%Y%m%d_%H%M).json"

# Colors for Output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}============================================================${NC}"
echo -e "${BLUE}          ELASTICSEARCH CLUSTER AUDIT REPORT                ${NC}"
echo -e "${BLUE}============================================================${NC}"

# 1. Cluster Health
echo -n "Checking Cluster Health: "
HEALTH_JSON=$(curl -s -u "$USER:$PASS" "$ES_HOST/_cluster/health")
STATUS=$(echo $HEALTH_JSON | jq -r '.status')

if [[ "$STATUS" == "green" ]]; then echo -e "${GREEN}GREEN${NC}";
elif [[ "$STATUS" == "yellow" ]]; then echo -e "${YELLOW}YELLOW${NC}";
else echo -e "${RED}RED${NC}"; fi

# 2. Node Resource Audit (Disk, CPU, Heap)
echo -e "\n${BLUE}--- Node Resource Audit ---${NC}"
printf "%-20s %-10s %-10s %-10s %-10s\n" "NODE_NAME" "CPU %" "HEAP %" "DISK %" "SHARDS"
curl -s -u "$USER:$PASS" "$ES_HOST/_cat/nodes?h=n,cpu,hp,dp,s&format=json" | jq -c '.[]' | while read -r node; do
    NAME=$(echo $node | jq -r '.n')
    CPU=$(echo $node | jq -r '.cpu')
    HEAP=$(echo $node | jq -r '.hp')
    DISK=$(echo $node | jq -r '.dp')
    SHARDS=$(echo $node | jq -r '.s')

    # Color logic for Heap/Disk thresholds
    if [ "$HEAP" -gt 85 ] || [ "$DISK" -gt 85 ]; then COLOR=$RED; else COLOR=$GREEN; fi
    printf "${COLOR}%-20s %-10s %-10s %-10s %-10s${NC}\n" "$NAME" "$CPU%" "$HEAP%" "$DISK%" "$SHARDS"
done

# 3. Performance Rejections (Thread Pools)
echo -e "\n${BLUE}--- Thread Pool Rejections (Search & Write) ---${NC}"
REJECTIONS=$(curl -s -u "$USER:$PASS" "$ES_HOST/_cat/thread_pool/search,write?h=n,name,rejected&format=json")
echo "$REJECTIONS" | jq -c '.[]' | while read -r pool; do
    N=$(echo $pool | jq -r '.n')
    PN=$(echo $pool | jq -r '.name')
    REJ=$(echo $pool | jq -r '.rejected')
    if [ "$REJ" -gt 0 ]; then
        echo -e "${RED}[WARN] Node $N has $REJ rejected $PN tasks!${NC}"
    fi
done

# 4. ILM & Shard Troubleshooting
echo -e "\n${BLUE}--- Unassigned Shards ---${NC}"
UNASSIGNED=$(echo $HEALTH_JSON | jq -r '.unassigned_shards')
if [ "$UNASSIGNED" -gt 0 ]; then
    echo -e "${RED}Detected $UNASSIGNED unassigned shards.${NC}"
    echo "Running Allocation Explain for the first issue..."
    curl -s -u "$USER:$PASS" "$ES_HOST/_cluster/allocation/explain?pretty" | head -n 20
else
    echo -e "${GREEN}All shards allocated.${NC}"
fi

# Save raw data to JSON for history
echo "$HEALTH_JSON" > "$OUTPUT_FILE"
echo -e "\n${BLUE}Audit Complete. Full health JSON saved to: $OUTPUT_FILE${NC}"

```

---

# 2. Master Documentation: Metrics & Red Flags

This guide explains **exactly** what the script output means and what thresholds indicate a server issue.

### Cluster State Indicators

* **Green:** All primary and replica shards are allocated.
* **Yellow:** All primary shards are active, but at least one replica is missing (High risk of data loss if a node fails).
* **Red:** At least one primary shard is missing. **Queries will return partial data.**

---

### Critical Hardware Thresholds (RAM & Disk)

Elasticsearch performance depends on the **filesystem cache** and **JVM Heap**.

| Metric | Target | Red Flag (> Critical) | Action |
| --- | --- | --- | --- |
| **JVM Heap %** | 50% - 70% | **85%** | Run `GET _nodes/stats/jvm` and check GC times. |
| **Disk Usage** | < 75% | **85% (Low Watermark)** | ES will stop moving new shards to this node. |
| **Disk Usage** | < 75% | **95% (Flood Stage)** | **Cluster becomes Read-Only.** Delete old indices immediately. |
| **CPU Load** | < 2.0 | **> 10.0** | Check `GET _nodes/hot_threads` for expensive queries. |

---

### Performance Bottlenecks (The "Thread Pool")

If your `api_perf_test.sh` (from earlier) starts showing errors, check the **Thread Pool Rejections**.

1. **Search Pool Rejections:** Your queries are too slow or too frequent for the number of CPU cores.
2. **Write (Bulk) Pool Rejections:** You are indexing data faster than the disks can write. Reduce bulk size or increase the `refresh_interval`.

---

### Troubleshooting Checklist

* **RAM Issues?** Check for **Oversharding**. You should have no more than 20 shards per GB of Heap.
* **Disk Issues?** Check your **ILM Policies**. Ensure indices are moving to `delete` phase after their retention period.
* **Kibana Charts Blank?** Check if the `.kibana` index is Red or if the disk has hit 95% (Read-Only).

**Would you like me to help you create an ILM (Index Lifecycle Management) policy that automatically deletes logs older than 30 days to prevent these disk issues?**
