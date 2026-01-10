Here is the exhaustive guide to **Elasticsearch Dev Tools queries**. These commands are essential for any administrator to monitor, troubleshoot, and optimize a cluster.

---

# 1. Cluster Health & Overall Status

These queries give you the "30,000-foot view" of your cluster.

| Goal | Query | Explanation |
| --- | --- | --- |
| **Quick Health** | `GET _cluster/health` | Shows status (**Green**, **Yellow**, **Red**), node count, and active shards. |
| **Node List** | `GET _cat/nodes?v&h=name,role,heap.percent,cpu,load_1m,ram.percent` | Tabular view of all nodes, their roles, and current CPU/RAM/Heap usage. |
| **Pending Tasks** | `GET _cat/pending_tasks?v` | Shows tasks (like shard rebalancing) waiting in the queue. High numbers indicate a bottleneck. |
| **Cluster Stats** | `GET _cluster/stats?human&pretty` | Detailed hardware info: total disk, total RAM, and OS versions across the cluster. |

---

# 2. Shard & Allocation Troubleshooting

If your cluster is **Yellow** or **Red**, use these to find out why.

### Identify the "Why"

```json
// Tells you exactly why a specific shard is not allocated (e.g., "node has too much data")
GET _cluster/allocation/explain
{
  "index": "my_index_name",
  "shard": 0,
  "primary": true
}

```

### Check Shard Distribution

```bash
# List all shards, their state (STARTED, INITIALIZING, UNASSIGNED), and which node they sit on
GET _cat/shards?v&s=state,index

```

---

# 3. Disk & RAM Issues (The "Watermark" Problems)

Elasticsearch stops writing to indices if disks reach certain "Watermarks" (Default: 85% for low, 90% for high, 95% for flood stage).

### Disk Stats

```bash
# Detailed view of disk usage per node
GET _cat/allocation?v&s=disk.avail

```

### RAM & JVM Monitoring

Elasticsearch relies heavily on the **JVM Heap**. If Heap usage is consistently >85%, you will experience slow queries or OOM (Out of Memory) crashes.

```bash
# Check JVM settings and Heap usage per node
GET _nodes/stats/jvm?pretty

```

> **Tip:** If `heap_used_percent` is high, check if you have "Oversharding" (too many tiny shards). Aim for **20GB–50GB** per shard.

---

# 4. Performance & Slow Query Debugging

Use these when users complain that "Kibana is slow" or "the API is lagging."

### Hot Threads

```bash
# Shows what the CPU is doing RIGHT NOW. 
# Look for "search" or "bulk" threads taking high CPU.
GET _nodes/hot_threads

```

### Enabling Slow Logs

You can force Elasticsearch to log any query that takes longer than a certain threshold.

```json
PUT /my_index/_settings
{
  "index.search.slowlog.threshold.query.warn": "2s",
  "index.search.slowlog.threshold.fetch.warn": "1s"
}

```

---

# 5. ILM (Index Lifecycle Management) & Policies

ILM automates the movement of data from **Hot** nodes (Fast SSD) to **Cold** nodes (Cheap HDD).

### Check ILM Status

```bash
# Is the ILM service actually running?
GET _ilm/status

# Explain why an index is stuck in a specific ILM phase
GET my_index/_ilm/explain

```

### View/Update Policies

```bash
# List all lifecycle policies
GET _ilm/policy

# Check the template that attaches the policy to new indices
GET _index_template

```

---

# 6. Master Troubleshooting Summary Table

| Issue | First Query to Run | What to look for |
| --- | --- | --- |
| **Red/Yellow Health** | `GET _cluster/allocation/explain` | Look for `decider` reasons (e.g., "disk threshold exceeded"). |
| **Slow Searches** | `GET _nodes/hot_threads` | Check if threads are stuck in `lucene` or `search`. |
| **High RAM Usage** | `GET _cat/nodes?h=n,hp,rp` | If `hp` (Heap %) is >90%, increase heap or add nodes. |
| **Disk is Full** | `GET _cat/allocation?v` | If `disk.percent` >95%, indices become read-only. |
| **ILM Not Moving** | `GET <index>/_ilm/explain` | Look for `step_info` showing errors in the transition. |

---

### Conclusion & Next Step

This toolkit allows you to diagnose 90% of common Elasticsearch issues directly from the Dev Tools console.

**Would you like me to create a "Health Dashboard" JSON for you that you can import into Kibana to visualize these metrics (CPU, Disk, Heap) automatically?**
