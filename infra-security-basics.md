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

| Layer | Goal | Linux Command |
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

**Would you like me to add the specific Illumio CLI commands (like `illumio-ven-ctl`) to this guide so you can check the VEN status directly?**
