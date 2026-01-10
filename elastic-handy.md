This guide provides a comprehensive set of **Elasticsearch Dev Tools queries** categorized by troubleshooting domain. Each section includes the query, an explanation of the output, and common "Red Flags" to look for.

---

# 1. Cluster Health & Overall Availability

Start here to determine if the cluster is in a stable state.

| Query | Purpose | What to look for |
| --- | --- | --- |
| `GET _cluster/health` | Overall status summary. | **Status**: Red (primary missing), Yellow (replica missing). |
| `GET _cat/nodes?v&h=n,role,m,cpu,load_1m,hp,rp` | Real-time node performance. | **hp** (Heap %) > 85% or **cpu** > 90% consistently. |
| `GET _cluster/stats?human&pretty` | Hardware & Software overview. | Check `total_in_bytes` for RAM/Disk vs. used. |

```json
// Detailed explanation of WHY a shard is unassigned
GET _cluster/allocation/explain
{
  "index": "my_logs_index",
  "shard": 0,
  "primary": true
}

```

---

# 2. JVM & RAM Troubleshooting

Elasticsearch runs on the Java Virtual Machine (JVM). Memory issues here cause the "Circuit Breaker" to trip, which rejects queries.

### Monitor JVM Heap Pressure

```bash
# Detailed JVM stats for all nodes
GET _nodes/stats/jvm?pretty

```

**Key Fields to Audit:**

* `jvm.mem.heap_used_percent`: Should follow a "sawtooth" pattern (up to 70%, then down). Constant 85%+ indicates a need for more RAM or fewer shards.
* `jvm.gc.collectors.old.collection_count`: If this increases rapidly, the node is struggling with "Stop-the-World" garbage collection.

### Check Circuit Breakers

Circuit breakers prevent a node from crashing by stopping expensive requests.

```bash
# Check if any breakers have tripped
GET _nodes/stats/breaker?pretty

```

> **Red Flag:** If `tripped` is > 0 for `fielddata` or `request`, your queries are too memory-intensive for your current heap size.

---

# 3. Disk Space & Watermarks

Elasticsearch has three critical "Watermark" stages for disk usage.

| Watermark | Default | Result |
| --- | --- | --- |
| **Low** | 85% | No new shards allocated to the node. |
| **High** | 90% | Shards start relocating away from the node. |
| **Flood Stage** | 95% | **All indices become Read-Only.** |

### Check Allocation & Disk

```bash
# Shows available disk and shard count per node
GET _cat/allocation?v&s=disk.avail:desc

```

### Fix "Read-Only" Indices (Flood Stage)

If a node hits 95%, you must free space and then manually unlock the indices:

```json
PUT */_settings
{
  "index.blocks.read_only_allow_delete": null
}

```

---

# 4. ILM (Index Lifecycle Management) Performance

Use these to verify that data is correctly moving from "Hot" (SSD) to "Cold" (HDD) tiers.

### Troubleshoot Stuck Indices

```bash
# Explain why an index hasn't moved to the next phase
GET /my_index_name/_ilm/explain

```

**Common Errors in Explain:**

* `step_info`: Shows the specific error (e.g., "target index already exists" or "node does not have correct attributes").

### Audit All Managed Indices

```bash
# List all indices managed by ILM and check for errors only
GET _all/_ilm/explain?only_errors=true

```

---

# 5. Performance Issues & Thread Pool Rejections

When searches are slow, it's often due to "Thread Pool Rejections."

### Check Thread Pool Queues

```bash
# Look for 'rejected' count in search and write pools
GET _cat/thread_pool/search,write?v&h=id,name,active,rejected,queue

```

* **Active:** Threads currently working.
* **Queue:** Tasks waiting (usually max 100-1000).
* **Rejected:** Requests dropped because the queue was full. **This is a major performance red flag.**

### Find "Hot" Threads

This tells you exactly what the CPU is doing *right now* at a code level.

```bash
# Identify if CPU is busy with 'search', 'bulk' (indexing), or 'gc'
GET _nodes/hot_threads

```

---

# 6. Index & Shard Health

Oversharding (too many small shards) is the #1 cause of cluster instability.

### Shard Sizing Audit

```bash
# List shards and their size on disk
GET _cat/shards?v&s=store:desc

```

> **Best Practice:** Keep shard sizes between **10GB and 50GB**. Shards < 1GB are "Small Shard Noise" and waste JVM heap.

### Check Index Templates

Ensure your new indices are getting the correct settings (shards, replicas, ILM policy).

```bash
# View all index templates
GET _index_template

```

---

### Summary Checklist for Daily Monitoring

1. `GET _cluster/health` -> Is it Green?
2. `GET _cat/nodes?h=n,hp,cpu` -> Is Heap < 80% and CPU < 70%?
3. `GET _cat/allocation?v` -> Is disk usage < 85%?
4. `GET _cat/thread_pool?v` -> Are rejections = 0?

**Would you like me to help you write a script to automate these checks and send a notification to Slack or Email if any limits are exceeded?**
