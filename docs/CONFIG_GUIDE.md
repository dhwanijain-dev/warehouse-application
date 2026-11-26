# Configuration Guide - Warehouse Scan (Patch v2)

Warehouse Scan uses a layered configuration system (`app_config.json`).

## Config Keys & Defaults

| Key | Type | Default | Description |
|---|---|---|---|
| `providers.auth` | String | `local` | Auth provider (`local`, `api`, `fake`) |
| `providers.email` | String | `intent` | Email provider (`intent`, `saveonly`, `smtp`, `api`, `fake`) |
| `session.timeoutMinutes` | Int | `0` | Inactivity timeout in minutes (`0` = disabled) |
| `locationSetting.duplicateScanPolicy` | String | `reject` | Duplicate scan handling (`reject`, `warn`, `allow`) |
| `admin.pin` | String | `1234` | Admin PIN code for restricted settings |
| `smtp.host` | String | `smtp.example.com` | SMTP server host |
| `smtp.port` | Int | `587` | SMTP server port |
| `smtp.security` | String | `starttls` | SMTP security mode (`starttls` or `ssl`) |
| `txtFile.includeLocationLines` | Boolean | `false` | Whether to include Rack/Zone/Setting lines in TXT files |
