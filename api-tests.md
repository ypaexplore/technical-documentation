Here is the exhaustive **API Testing Suite**. This includes a reusable Bash script for automation and a detailed Markdown guide that explains every testing scenario (JSON, Parameters, File Uploads, etc.) using OAuth2 Client Credentials.

---

# 1. Exhaustive API Testing Script (`api_tester.sh`)

This script automates the **OAuth2 handshake** and runs a battery of tests. Save this as `api_tester.sh` and run `chmod +x api_tester.sh`.

```bash
#!/bin/bash

# ==============================================================================
# CONFIGURATION
# ==============================================================================
TOKEN_URL="https://auth.yourdomain.com/oauth2/token"
API_BASE="https://api.yourdomain.com/v1"
CLIENT_ID="your_client_id"
CLIENT_SECRET="your_client_secret"
SCOPE="read write" # Optional

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}=== Starting Exhaustive API Test Suite ===${NC}"

# ==============================================================================
# STEP 1: OAUTH2 TOKEN RETRIEVAL
# ==============================================================================
echo -n "Fetching Access Token... "
TOKEN_RESPONSE=$(curl -s -X POST "$TOKEN_URL" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -u "$CLIENT_ID:$CLIENT_SECRET" \
    -d "grant_type=client_credentials" \
    -d "scope=$SCOPE")

# Extract token using jq
ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')

if [ "$ACCESS_TOKEN" == "null" ] || [ -z "$ACCESS_TOKEN" ]; then
    echo -e "${RED}FAILED${NC}"
    echo "Response: $TOKEN_RESPONSE"
    exit 1
else
    echo -e "${GREEN}SUCCESS${NC}"
fi

# Helper function to print headers for clarity
test_header() {
    echo -e "\n${BLUE}>>> TEST: $1${NC}"
}

# ==============================================================================
# STEP 2: EXHAUSTIVE TESTING SCENARIOS
# ==============================================================================

# Scenario A: GET with Query Parameters
test_header "GET Request with Parameters"
curl -i -X GET "$API_BASE/items?status=active&category=tools" \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     -H "Accept: application/json"

# Scenario B: POST with JSON Data (Inline)
test_header "POST Request with JSON Body"
curl -i -X POST "$API_BASE/items" \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "title": "API Test Item",
       "body": "Testing JSON integration",
       "metadata": { "source": "curl-script", "id": 99 }
     }'

# Scenario C: POST with JSON Data (From File)
test_header "POST Request with JSON from File"
echo '{"name": "FileBased", "type": "test"}' > payload.json
curl -i -X POST "$API_BASE/items/upload-json" \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -d @payload.json

# Scenario D: File Upload (Multipart Form Data)
test_header "POST Request with Multipart File Upload"
echo "This is a test file content" > test_doc.txt
curl -i -X POST "$API_BASE/attachments" \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     -F "file=@test_doc.txt" \
     -F "file_type=text/plain" \
     -F "description=Test attachment via curl"

# Scenario E: PUT (Full Update) with Header versioning
test_header "PUT Request with Custom Headers"
curl -i -X PUT "$API_BASE/items/1" \
     -H "Authorization: Bearer $ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -H "X-API-Version: 2.0" \
     -d '{"title": "Updated Title"}'

# Scenario F: DELETE Request
test_header "DELETE Request"
curl -i -X DELETE "$API_BASE/items/99" \
     -H "Authorization: Bearer $ACCESS_TOKEN"

# Cleanup temporary files
rm payload.json test_doc.txt
echo -e "\n${GREEN}=== Testing Suite Complete ===${NC}"

```

---

# 2. Comprehensive API Testing Guide (Markdown)

## OAuth2 Client Credentials Flow

This flow is "Machine-to-Machine." There is no user login. The **Client ID** and **Secret** are exchanged directly for a **Bearer Token**.

### Core `curl` Flags for API Testing

| Flag | Description | Why use it? |
| --- | --- | --- |
| `-i` | Include HTTP Headers | Shows 200 OK, 401 Unauthorized, etc. |
| `-H` | Custom Header | Required for `Authorization: Bearer <token>`. |
| `-d` | Data | Used for sending JSON bodies or URL-encoded data. |
| `-G` | GET Data | Forces data to be appended to the URL as query params. |
| `-F` | Form Data | Required for uploading files (`multipart/form-data`). |
| `-u` | User | Short for Basic Auth (`client_id:client_secret`). |

---

## Exhaustive Request Patterns

### 1. Complex Query Strings

When searching or filtering data, use query parameters.

```bash
# Good way (handles encoding)
curl -G "$API_BASE/search" \
     -d "q=security protocols" \
     -d "sort=desc" \
     -H "Authorization: Bearer $TOKEN"

```

### 2. JSON Body Handling

Always ensure you tell the server to expect JSON using the `Content-Type` header.

* **Inline JSON:** Use single quotes `'` around the body so double quotes `"` inside the JSON don't break.
* **File-based JSON:** Use `-d @filename.json`. This is the best way to handle large or complex objects.

### 3. File & Binary Uploads

The `@` symbol is a special instruction to `curl` to read a local file.

```bash
curl -X POST "$API_BASE/profile/image" \
     -H "Authorization: Bearer $TOKEN" \
     -F "image=@/home/user/avatar.jpg;type=image/jpeg"

```

### 4. Handling API Responses

You can pipe the results to tools to make them readable:

* **Pretty Print:** Add `| jq .` to the end of your command.
* **Save to File:** Use `-o output.json`.
* **Extract specific data:** `| jq -r '.id'`.

---

## Security Best Practices for Testing

1. **Never Hardcode Secrets:** Use environment variables (`export CLIENT_SECRET=...`) instead of putting them in the script.
2. **Use `-s` (Silent):** When running in a CI/CD pipeline, use `-s` to avoid cluttering logs with progress bars.
3. **Verify SSL:** Never use `-k` (insecure) in production. Ensure your certificates are valid.
