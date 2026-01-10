Here is the updated guide including the **Linux Troubleshooting Toolset**. This section connects the jargon to the actual commands you would run on your Postgres or K8s nodes to see what is happening at each layer of the OSI model.

---

# Cybersecurity & Infrastructure Reference Guide

## 1. Network & Infrastructure Glossary

| Term | OSI Layer | Definition |
| --- | --- | --- |
| **VLAN** | Layer 2 | A "Virtual Room" on a switch that isolates traffic at the physical/data link level. |
| **Subnet** | Layer 3 | A logical range of IP addresses (e.g., `10.0.1.0/24`). Usually, 1 Subnet lives inside 1 VLAN. |
| **Switch** | Layer 2 | Hardware that connects devices in the **same** VLAN using MAC Addresses. |
| **Router** | Layer 3 | Hardware that connects **different** VLANs/Subnets using IP Addresses. |
| **Default Gateway** | Layer 3 | The "Exit Door" IP a server uses to send traffic to other networks. |
| **NIC** | Layer 1/2 | Network Interface Card; the hardware that turns data into electrical or light pulses. |
| **OSI Model** | 1 - 7 | A 7-layer framework explaining how data moves from a physical wire to an application. |

---

## 2. The Defense Toolkit (Defense in Depth)

**Defense in Depth** is the strategy of layering different security tools so that if one fails, others are there to stop the attack.

### Core Tools

* **Firewall (Perimeter):** The "Gatekeeper" for North-South traffic. Blocks/allows based on IP, Port, and Protocol.
* **Illumio VEN (Micro-segmentation):** The "Bodyguard" for East-West traffic. Lives on the host and uses **Cryptographic Identity** to verify servers, not just IPs.
* **WAF (Web Application Firewall):** Specialized for Layer 7 (HTTP). It "reads" traffic to block SQL Injections and API attacks.
* **AV / EDR (Anti-Virus):** The "Internal Police." It monitors the Operating System and files for malware or suspicious behavior (e.g., ransomware).
* **SIEM:** The "Security Dashboard" that collects logs from all the above tools to find red flags.

---

## 3. Linux Troubleshooting Toolset (By Layer)

If a connection between K8s and Postgres fails, use these commands to find which "layer" is broken.

| --- | --- | --- |
| **L1/L2** | Check Physical Link | `ip link show` or `ethtool eth0` |
| **L3** | Check IP & Gateway | `ip addr` and `ip route` |
| **L3** | Test Path to Server | `ping <destination_ip>` |
| **L4** | Check Local Ports | `ss -tulpn` or `netstat -plnt` |
| **L4** | Test Remote Port | `nc -zv <destination_ip> 5432` |
| **L4** | Check VEN/Firewall | `iptables -L -n -v` (to see active rules) |
| **L7** | Packet Inspection | `tcpdump -i eth0 port 5432` |

---

## 4. The Attacker's Playbook (Breach Vocabulary)

* **North-South Traffic:** Traffic entering or leaving your data center (Internet <-> Server).
* **East-West Traffic:** Traffic moving between internal assets (K8s <-> Postgres).
* **Lateral Movement:** When a hacker "jumps" sideways from one compromised server to another.
* **Exfiltration:** The act of stealing data and moving it out of the organization.
* **Blast Radius:** The total potential damage a single breach can cause.
* **Zero-Day:** A software vulnerability that has no known patch or fix yet.
* **Spoofing:** Faking an IP address to trick an IP-based firewall.

---

## 5. Common Attack Types

| Attack Level | Name | Description |
| --- | --- | --- |
| **Infrastructure** | **DDoS** | Flooding a network with junk traffic to crash the system. |
| **Infrastructure** | **VLAN Hopping** | Tricking a switch to move from a low-security VLAN to a high-security one. |
| **Application** | **SQL Injection** | Inserting malicious code into a query to steal database data. |
| **Application** | **BOLA** | An API attack where a user accesses another user's data by changing an ID. |
| **Application** | **SSRF** | Tricking a server into attacking other internal servers. |
| **Kubernetes** | **Container Escape** | Breaking out of a Pod to take control of the host physical server. |

---

## 6. Security Philosophy

* **Zero Trust:** "Never Trust, Always Verify." Assume the network is already breached.
* **Least Privilege:** Give a server/user only the bare minimum access needed (e.g., Port 5432 only).
* **Identity-Based Security:** Using certificates and keys (like Illumio VEN) to prove a server is who it says it is, rather than trusting an IP address.

---

### Summary Checklist for Connectivity Issues

1. **Physical (L1):** Check the NIC and cables (`ethtool`).
2. **Data Link (L2):** Is the device on the correct VLAN?
3. **Network (L3):** Can you reach the Gateway? (`ping`, `ip route`).
4. **Transport (L4):** Is the Illumio VEN or Firewall blocking the Port? (`nc`, `iptables`).
5. **Application (L7):** Is the service actually listening? (`ss -plnt`).

---

Final Troubleshooting Workflow

Check Physical (L1): ethtool eth0 (Is the link up?)

Check Network (L3): ping 10.x.x.x (Can I reach the gateway/destination?)

Check Port (L4): nc -zv 10.x.x.x 5432 (Is the port open?)

Check VEN (L4 Security): illumio-ven-ctl status (Is the policy blocking it?)

Check App (L7): ss -plnt (Is Postgres actually listening on that port?)

| Layer | Goal | Linux Command |


This final section provides a deep dive into the fundamental building blocks of infrastructure and networking, combined with the specific security measures (controls) used to protect them. This is formatted in `.md` for your documentation.

---

# Infrastructure & Network Security Fundamentals

## 1. Core Infrastructure Definitions

Before securing the network, we must define the physical and logical components that hold your applications (Postgres, RabbitMQ, K8s).

* **Compute (Host):** The physical server or Virtual Machine (VM) where the OS (Linux/Windows) runs.
* **Virtualization/Hypervisor:** Software (like VMware or KVM) that allows one physical server to be carved into multiple "Virtual Machines."
* **Containerization (Kubernetes/Docker):** A way to wrap an application (like RabbitMQ) with only the specific files it needs to run, making it portable and lightweight.
* **Storage:** The physical disks (SSD/HDD) or Network Attached Storage (NAS) where Postgres writes its data. Security here focuses on **Encryption at Rest**.

---

## 2. Network Basics & Hierarchy

Networking is organized into "layers" to ensure data moves efficiently.

### Layer 2: The Local Neighborhood (Data Link)

* **MAC Address:** The permanent "Hardware ID" burned into every NIC.
* **VLAN (Virtual LAN):** A method to separate one physical switch into multiple private networks.
* **ARP (Address Resolution Protocol):** The "translator" that finds which MAC address belongs to which IP address.

### Layer 3: The Global Map (Network)

* **IP Address:** The logical address assigned to a server.
* **Routing:** The process of moving packets from one Subnet to another.
* **ICMP (Ping):** A protocol used to check if a destination is "alive."

---

## 3. Security Measures & Controls

Security is applied at every stage of the infrastructure to create a "Grid" of protection.

### A. Network Security Measures

| Measure | Tool | Purpose |
| --- | --- | --- |
| **Segmentation** | VLANs / Subnets | Grouping similar assets together to limit broad access. |
| **Micro-segmentation** | **Illumio VEN** | Isolating individual servers/pods to prevent lateral movement. |
| **Access Control** | Firewall / ACLs | Defining "Who" can talk to "Whom" on which Port. |
| **Encryption in Transit** | TLS / IPsec | Scrambling data as it moves across the wire so it cannot be "sniffed." |

### B. Endpoint (Host) Security Measures

| Measure | Tool | Purpose |
| --- | --- | --- |
| **Hardening** | OS Configuration | Disabling unused services and closing unneeded ports (e.g., closing SSH if not used). |
| **Patch Management** | Update Services | Regularly updating Linux/Postgres to fix known Zero-Day vulnerabilities. |
| **EDR / AV** | Crowdstrike / SentinelOne | Real-time monitoring of the server to detect if a hacker is running "weird" commands. |
| **IAM** | RBAC / Active Directory | "Identity and Access Management"—ensuring users only have the rights they need. |

### C. Application & Data Security Measures

| Measure | Tool | Purpose |
| --- | --- | --- |
| **Input Validation** | WAF / App Code | Checking data sent to the app to prevent **SQL Injection**. |
| **Secrets Management** | HashiCorp Vault / K8s Secrets | Storing DB passwords and API keys in an encrypted "vault" instead of plain text files. |
| **Data Masking** | Database Config | Hiding sensitive data (like credit card numbers) from users who don't need to see them. |

---

## 4. The "Security Mindset" Summary

When looking at your Postgres/RabbitMQ environment, always ask three questions:

1. **Visibility:** Can I see who is talking to this database right now? (Use Illumio Map).
2. **Least Privilege:** Is Port 5432 open ONLY to the specific K8s nodes that need it?
3. **Immutability:** If a hacker changes a file on the server, will my **AV/EDR** alert me?

---

### Final Documentation Tip

> "Infrastructure provides the **capability** for data to move. Networking provides the **pathway**. Security provides the **permission**."

**Would you like me to help you create a specific "Audit Checklist" for your Postgres server to verify all these security measures are actually in place?**
