Here is your **Master Infrastructure & Security Reference Guide**. You can copy the entire block below and save it as `Network_Security_Master.md`.

---

# Master Infrastructure & Security Reference Guide

## 1. Core Definitions & Infrastructure Basics

Infrastructure provides the **capability** for data to move; Networking provides the **pathway**; Security provides the **permission**.

* **Compute (Host):** The physical server or VM where the OS (Linux) runs.
* **VLAN (Layer 2):** A "Virtual Room" on a switch that isolates traffic at the hardware level.
* **Subnet (Layer 3):** A logical range of IP addresses (e.g., `10.0.1.0/24`). Usually, **1 Subnet = 1 VLAN**.
* **Default Gateway:** The "Exit Door" IP used to send traffic outside the local subnet.
* **NIC:** Network Interface Card; the physical hardware converting data into electrical/light pulses.

---

## 2. The Defense Toolkit (Defense in Depth)

We use a "layered" approach so that if one tool fails, another stops the attacker.

| Tool | Focus | Job Description |
| --- | --- | --- |
| **Firewall** | North-South | The perimeter guard blocking/allowing ports and IPs at the edge. |
| **Illumio VEN** | East-West | The "Bodyguard" on the host. Uses **Cryptographic Identity** to stop lateral movement. |
| **WAF** | Layer 7 | Inspects web traffic to stop SQL Injection and API attacks. |
| **AV / EDR** | OS / Files | The "Internal Police." Monitors for malware, ransomware, and bad processes. |
| **SIEM** | Logging | The central dashboard that collects alerts from all other tools. |

---

## 3. Security Philosophy: Zero Trust & Identity

Traditional security trusts **IP Addresses**. Modern security (Illumio) trusts **Identity**.

* **Zero Trust:** "Never Trust, Always Verify." Assume the hacker is already inside.
* **Least Privilege:** Giving a server the absolute minimum access (e.g., K8s only gets Port 5432).
* **Micro-segmentation:** Dividing the network into tiny zones to limit the **Blast Radius** of an attack.
* **Cryptographic Identity:** Using private keys and certificates (issued by the PCE) to prove a server is legitimate, preventing **IP Spoofing**.

---

## 4. Attacker Playbook & Jargon

* **Lateral Movement:** When a hacker "jumps" from a compromised RabbitMQ server to a Postgres server.
* **Exfiltration:** Stealing data and moving it out of the organization.
* **SQL Injection (SQLi):** Inserting malicious code into a database query to dump data.
* **BOLA:** An API attack where a user changes an ID to see someone else’s data.
* **Container Escape:** Breaking out of a K8s Pod to take over the physical host server.

---

## 5. Technical Troubleshooting & Audit Toolset

### A. Linux Network Commands

| Layer | Goal | Command |
| --- | --- | --- |
| **L1/L2** | Check Physical Link | `ip link show` or `ethtool eth0` |
| **L3** | Check IP/Route | `ip addr` and `ip route` |
| **L3** | Test Path | `ping <destination_ip>` |
| **L4** | Check Local Ports | `ss -tulpn` |
| **L4** | Test Remote Port | `nc -zv <destination_ip> 5432` |
| **L7** | Packet Sniffing | `tcpdump -i eth0 port 5432` |

### B. Illumio VEN Commands

| Task | Command |
| --- | --- |
| **Check Status** | `illumio-ven-ctl status` |
| **Show Labels** | `illumio-ven-ctl show-labels` |
| **Test Connectivity** | `illumio-ven-ctl connectivity-test` |
| **Check Rules** | `illumio-ven-ctl show-firewall` |

---

## 6. Postgres Security Audit Checklist

* [ ] **Connectivity:** Is Port 5432 blocked for everyone except whitelisted Labels?
* [ ] **Hardening:** Are unused services (like FTP/HTTP) disabled? (`ss -tulpn`)
* [ ] **Encryption:** Is `ssl_is_used()` returning true for active connections?
* [ ] **Users:** Is the application using a non-superuser account? (`\du`)
* [ ] **EDR:** Is the Anti-Virus/EDR agent active and reporting? (`systemctl status`)

---

**This completes your Master Reference Guide. Is there anything else you'd like to dive deeper into, or are you ready to implement these checks?**
