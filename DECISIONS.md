# Architectural & Design Decisions

| Open point | Decision / Choice |
|---|---|
| Module Structure | Single `:app` module with strict package-level Clean Architecture boundaries (`domain`, `data`, `feature`, `core`). |
| User Authentication | On-device Room database seeded from `users.json`, plain text passwords via `PasswordVerifier` interface (`PlainPasswordVerifier`). |
| Email Delivery Default | `IntentEmailSender` (`"intent"`) as default, opening system mail app with FileProvider attachment and confirmation prompt. Fallback to `SaveOnlyEmailSender` (`"saveonly"`). |
| Duplicate Scan Policy | Config-driven (`locationSetting.duplicateScanPolicy`): `"reject"`, `"warn"`, `"allow"`. |
| Demo Build Flavor | Added `demo` product flavor with visual "DEMO" banner, local login, and save-only email mode. |
| Admin PIN | Config-driven (`admin.pin`, default `"1234"`), required for non-admin users accessing Admin Settings or User Management. |
| Compile & Target SDK | Set to 35 to fully support `androidx.core:core-ktx:1.15.0`. |
| QR patch module boundary | Keep the QR payload extractor in `app/src/main/java/.../domain` because this repository has one `:app` module, as documented above. |
| QR payload default | Use the requested `raw` mode; URL, delimited, and JSON payloads remain invalid until the corresponding config mode is selected. |
| QR test sheet generator | Use PyMuPDF plus the system `libqrencode` library in the development script; neither is an app runtime dependency. |
| Camera scan resolution | Request 1280x720 analysis frames and use ML Kit auto-zoom bounded by the active camera's reported maximum; this targets smaller/distant labels without analyzing full-resolution 13 MP frames. |
| Zebra Sensor Continuity | Removed `STOP_SCANNING` call from DataWedge `onReceive` so SE4710 hardware sensor stays active for continuous trigger pulls. |
| Camera Scan Sample Code Cleanup | Removed all fallback mock sample code generation (`100055`) from `DataWedgeScannerController` so camera scanning yields only genuine barcodes. |
| Multiple Pending Challan Storage | Added `LocationChallanEntity` & `LocationChallanDao` in Room DB to allow saving and reopening multiple location challans (Rack, Zone, Setting, Batches) concurrently. |
| Dispatch Verification Layout | Redesigned metric counters into a compact single-row card container, reducing top height by 136.dp to fit 5.0" Zebra TC26 display. |
