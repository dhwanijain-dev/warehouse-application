# Email Delivery Modes Guide

Warehouse Scan supports multiple email dispatch modes, configurable via `providers.email` in `app_config.json` or Admin Settings:

1. **`intent` (Default)**: Opens the device's native email client with the TXT file attached via FileProvider, pre-filled recipient email, subject, and body. After the email client closes, a confirmation dialog ("Did you send the email?") ensures session integrity.
2. **`saveonly`**: Automatically saves the generated TXT file to the device's Downloads folder via MediaStore (no storage permissions required) and acts as an automatic fallback if network or email sending fails.
3. **`smtp`**: Sends email directly through an SMTP server configured with host, port, security mode (`starttls`), and credentials in Admin Settings.
4. **`api` & `fake`**: Backend REST API multipart upload or simulated debug email senders.
