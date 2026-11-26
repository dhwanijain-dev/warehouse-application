# User Management Guide

Warehouse Scan stores user credentials locally in an on-device Room database. No external server is required for login.

## Managing Users
1. Access **User Management** from Admin Settings or from the Home screen (for users with `ADMIN` role).
2. **Add User**: Tap "Add User", fill in User ID, Password, Display Name, Email, Role (Admin or User), and Active status.
3. **Edit / Deactivate / Delete**: Tap options on any user card. Note that the app prevents deleting or deactivating the last active administrator.

## Importing & Exporting Users (Multi-Device Setup)
To set up multiple handheld devices with the same user list:
1. On the configured device, tap **Export Users** to share or save the `users.json` file.
2. Transfer the file to other devices (via email, USB, or file share).
3. On the new device, tap **Import Users** in User Management, select the `users.json` file, and choose between **Replace All** or **Merge**.
