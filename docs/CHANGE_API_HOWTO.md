# Change API How-To

When backend endpoints, field names, or auth schemes change, zero Kotlin source code edits are required.

1. **Changing Server URL**: Update `api.baseUrl` in `app_config.json` or via Admin Settings.
2. **Changing Endpoint Paths or Methods**: Update `api.endpoints.login`, `sendTxtEmail`, or `profile` path/method in `app_config.json`.
3. **Changing JSON Response Field Mapping**: Update `api.fieldMap` dot-paths (e.g. `login.response.userId`, `login.response.email`, `login.response.token`) in `app_config.json`.
4. **Swapping Auth or Email Providers**: Implement the respective interface (`AuthRepository` or `EmailSender`) and update `providers.auth` or `providers.email` in `app_config.json`.
