# 🐧 Linux Remote Development Cheat Sheet

A practical guide for working on remote VMs, servers, Docker, and Kubernetes environments.

---

## 📁 File & Directory Management

```bash
# List files
ls

# List with details
ls -l

# List all (including hidden files)
ls -la

# Change directory
cd /path/to/dir

# Go back
cd ..

# Go to home directory
cd ~

# Create directory
mkdir myfolder

# Create nested directories
mkdir -p parent/child

# Remove file
rm file.txt

# Remove directory (empty)
rmdir myfolder

# Remove directory with contents
rm -rf myfolder

# Copy file
cp file.txt /destination/

# Copy directory
cp -r folder/ /destination/

# Move or rename file
mv file.txt newname.txt

# Move file
mv file.txt /destination/


⸻

📄 File Viewing & Editing

# View file
cat file.txt

# View page by page
less file.txt

# First 10 lines
head file.txt

# Last 10 lines
tail file.txt

# Follow logs live
tail -f app.log

# Edit file (vi)
vi file.txt

# Edit file (nano)
nano file.txt


⸻

🔍 Search & Find

# Search text in file
grep "error" file.txt

# Recursive search
grep -r "error" .

# Find file by name
find . -name "file.txt"

# Find large files
find . -size +100M


⸻

⚙️ Process Management

# Show all processes
ps -ef

# Filter processes
ps -ef | grep java

# Real-time monitor
top

# Kill process (graceful)
kill <PID>

# Force kill
kill -9 <PID>

# Kill by name
pkill java

# Kill process using a port
fuser -k 8080/tcp


⸻

🌐 Networking & Remote Access

# Ping host
ping google.com

# Check open ports
netstat -tulnp

# Modern alternative
ss -tulnp

# Call HTTP endpoint
curl http://localhost:8080

# SSH login
ssh user@host

# Copy file to remote
scp file.txt user@host:/path/

# Copy file from remote
scp user@host:/path/file.txt .

# Sync directories
rsync -avz folder/ user@host:/path/


⸻

💾 Disk & Memory

# Disk usage
df -h

# Folder size
du -sh folder/

# Memory usage
free -m

# System info
uname -a


⸻

📦 Archive & Compression

# Create archive
tar -cvf archive.tar folder/

# Extract archive
tar -xvf archive.tar

# Create compressed archive
tar -czvf archive.tar.gz folder/

# Extract compressed archive
tar -xzvf archive.tar.gz


⸻

🔐 Permissions

# Change permissions
chmod 755 file.sh

# Make executable
chmod +x script.sh

# Change owner
chown user:group file.txt


⸻

🐳 Docker Commands

# List running containers
docker ps

# List all containers
docker ps -a

# View logs
docker logs <container_id>

# Exec into container
docker exec -it <container_id> bash

# Build image
docker build -t myapp .

# Run container
docker run -p 8080:8080 myapp

# Stop container
docker stop <container_id>

# Remove container
docker rm <container_id>


⸻

☸️ Kubernetes Commands

# List pods
kubectl get pods

# Describe pod
kubectl describe pod <pod-name>

# View logs
kubectl logs <pod-name>

# Exec into pod
kubectl exec -it <pod-name> -- bash

# Restart deployment
kubectl rollout restart deployment <name>

# Port forward
kubectl port-forward pod/<pod-name> 8080:8080


⸻

🧠 Debugging Tricks

# Who is using a port
fuser 8080/tcp

# Check process details
ps -ef | grep <PID>

# Kill process on port
fuser -k 8080/tcp

# Watch logs live
tail -f app.log

# Monitor command repeatedly
watch -n 2 "ps -ef | grep java"


⸻

🚀 Pro Tips
	•	Use kill before kill -9
	•	Use rsync for large file transfers
	•	Use tail -f for logs
	•	Use fuser when ports are blocked
	•	Always verify process with ps -ef | grep
	•	Check disk (df -h) if things fail unexpectedly

⸻

✅ Quick Debug Flow

# 1. Check running processes
ps -ef | grep java

# 2. Check port usage
fuser 8080/tcp

# 3. Kill stuck process
kill -9 <PID>

# 4. Check logs
tail -f app.log

---
