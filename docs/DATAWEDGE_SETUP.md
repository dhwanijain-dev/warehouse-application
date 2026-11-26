# DataWedge setup for Warehouse Scan

The app attempts to create this profile automatically: **WarehouseScanProfile**. If MDM blocks profile changes, an IT administrator can create it manually on the TC26:

1. Open DataWedge and create a profile named exactly `WarehouseScanProfile`.
2. Associate `com.example.warehousescan` with the profile and activity `*` (repeat for a flavor-specific application id if applicable).
3. Enable the Barcode input plugin and the QR Code and Code 128 decoders. Enable any other configured symbologies as needed.
4. Enable Intent Output, select **Broadcast intent**, set the intent action to `com.warehouse.scan.ACTION`, and keep the category `android.intent.category.DEFAULT`.
5. Disable Keystroke Output.
6. Save the profile and return to Warehouse Scan. Use the Scan button if the hardware trigger is unavailable.

The action and profile name can be changed in `app/src/main/assets/config/app_config.json` under `scanner`.
