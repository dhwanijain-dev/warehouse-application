# API Contract - Warehouse Scan

## 1. Login Endpoint
- **Method**: POST
- **Path**: `api/v1/auth/login`
- **Request Body**:
  ```json
  {
    "username": "user1",
    "password": "password123"
  }
  ```
- **Response**:
  ```json
  {
    "data": {
      "userId": "123",
      "email": "user1@warehouse.local",
      "token": "jwt-token-abc"
    }
  }
  ```

## 2. Location TXT Email Endpoint
- **Method**: POST
- **Path**: `api/v1/location/email`
- **Content-Type**: `multipart/form-data`
- **Parts**:
  - `file`: TXT file attachment containing scanned batches
  - `email`: Recipient email address
  - `subject`: Email subject
  - `body`: Email body text
- **Response**: `200 OK`
