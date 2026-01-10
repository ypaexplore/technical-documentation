Here is the comprehensive **Security Compliance Script** and the updated **Master Documentation** in `.md` format.

This bash script is designed to be run on your Linux nodes (Postgres, RabbitMQ, or K8s Workers) to audit them for the weak protocols and configurations we discussed.

---

# 1. Security Compliance Audit Script (`audit_security.sh`)

```bash
#!/bin/bash
# Security Compliance Audit Script
# Checks for weak protocols, ports, and crypto settings

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting Security Compliance Audit...${NC}\n"

# 1. Check for Weak Protocols (Telnet, FTP, etc.)
echo "--- Checking for Insecure Services ---"
for service in telnet ftp rsh; do
    if systemctl is-active --quiet $service; then
        echo -e "${RED}[FAIL]${NC} $service is active. Disable it immediately."
    else
        echo -e "${GREEN}[PASS]${NC} $service is not active."
    fi
done

# 2. Check for Weak SSH Settings
echo -e "\n--- Checking SSH Configuration ---"
if grep -q "PermitRootLogin yes" /etc/ssh/sshd_config; then
    echo -e "${RED}[FAIL]${NC} Root Login is enabled over SSH."
else
    echo -e "${GREEN}[PASS]${NC} Root Login is disabled/restricted."
fi

# 3. Check for Weak Hashing/Ciphers in Postgres
if [ -f /var/lib/pgsql/data/postgresql.conf ] || [ -f /etc/postgresql/*/main/postgresql.conf ]; then
    echo -e "\n--- Checking Postgres SSL Status ---"
    CONF_FILE=$(find /etc/postgresql /var/lib/pgsql -name "postgresql.conf" 2>/dev/null | head -n 1)
    if grep -q "^ssl = on" "$CONF_FILE"; then
        echo -e "${GREEN}[PASS]${NC} SSL is enabled in Postgres."
    else
        echo -e "${RED}[FAIL]${NC} SSL is NOT enabled in Postgres configuration."
    fi
fi

# 4. Check for Illumio VEN Status
echo -e "\n--- Checking Illumio VEN Status ---"
if command -v illumio-ven-ctl &> /dev/null; then
    STATUS=$(illumio-ven-ctl status | grep "VEN State")
    echo -e "${GREEN}[INFO]${NC} $STATUS"
else
    echo -e "${RED}[WARN]${NC} Illumio VEN not found on this system."
fi

# 5. Check for Listening Ports (Layer 4)
echo -e "\n--- Checking Open Ports ---"
ss -tulpn | grep -E ':80|:21|:23' && echo -e "${RED}[FAIL]${NC} Found insecure ports (80/21/23) listening!" || echo -e "${GREEN}[PASS]${NC} No obvious insecure ports listening."

echo -e "\n${GREEN}Audit Complete.${NC}"

```

---

# 2. Master Documentation: Protocol & Algorithm Standards

## Detailed Protocol & Cryptography Security Standard

### A. Network Protocol Hardening

We categorize protocols into **Cleartext (Insecure)** and **Encrypted (Secure)**.

| Protocol | Port | Category | Security Measure |
| --- | --- | --- | --- |
| **HTTP** | 80 | **Weak** | Redirect to Port 443 (HTTPS). |
| **Telnet** | 23 | **Weak** | Disable and use **SSH (Port 22)**. |
| **FTP** | 21 | **Weak** | Replace with **SFTP** or **SCP**. |
| **SMB v1** | 445 | **Weak** | Disable; allow only **SMB v3.1** with encryption. |
| **HTTPS** | 443 | **Strong** | Enforce **TLS 1.2** or **TLS 1.3** only. |
| **DB (SSL)** | 5432 | **Strong** | Ensure `ssl = on` is set in DB configuration. |

---

### B. Cryptographic Algorithms (The "Math" of Security)

Even secure protocols fail if the underlying math (algorithms) is outdated.

#### 1. Hashing Algorithms (Integrity)

* **Avoid (Broken):** **MD5, SHA-1**. These are vulnerable to collision attacks (where two different files produce the same hash).
* **Use (Secure):** **SHA-256, SHA-512, SHA-3**.
* **Password Specific:** Use **Argon2** or **bcrypt**. Unlike standard hashes, these are "computationally expensive," making brute-force attacks extremely slow for hackers.

#### 2. Encryption Algorithms (Confidentiality)

* **Avoid (Weak):** **DES, 3DES, RC4**. These have small key sizes and mathematical "leaks."
* **Use (Secure):** **AES-256 (GCM Mode)**. This is the industry standard for high-performance, high-security encryption.

#### 3. Key Exchange & Digital Signatures (Identity)

* **Avoid (Legacy):** **RSA < 2048-bit**.
* **Use (Modern):** **RSA 4096-bit** or **ECC (Elliptic Curve Cryptography)**.
* *Why ECC?* It provides the same security as RSA but with much smaller keys, making it significantly faster for Kubernetes certificates and API handshakes.



---

### C. Implementation Examples (Detailed)

#### Postgres Hardening (`postgresql.conf`)

Ensure your database doesn't allow "weak locks":

```ini
# Force modern TLS and strong ciphers
ssl = on
ssl_min_protocol_version = 'TLSv1.2'
ssl_ciphers = 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384'

```

#### Kubernetes Ingress Hardening

Block old browsers/clients that use weak ciphers:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  annotations:
    # Only allow the strongest TLS versions
    nginx.ingress.kubernetes.io/ssl-protocols: "TLSv1.3"
    nginx.ingress.kubernetes.io/ssl-ciphers: "AES256+EECDH:AES256+EDH:!aNULL"

```

---

### D. Summary Compliance Table

| Security Feature | Weak / Banned | Strong / Required |
| --- | --- | --- |
| **Hashing** | MD5, SHA-1 | **SHA-256, SHA-3** |
| **Symmetric Cipher** | DES, RC4, 3DES | **AES-256-GCM** |
| **Key Exchange** | Diffie-Hellman 1024 | **ECDHE (Elliptic Curve)** |
| **TLS Version** | TLS 1.0, 1.1 | **TLS 1.2, 1.3** |
| **Authentication** | Passwords only | **MFA / SSH Keys / Certs** |

---

**You now have the full master guide, the troubleshooting commands, and an automation script for auditing. Would you like me to explain how to integrate this audit script into a Jenkins or GitLab CI/CD pipeline for automated security checks?**
