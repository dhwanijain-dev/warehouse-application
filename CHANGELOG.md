# Changelog - Warehouse Scan UI/UX Standardization

All notable UI/UX changes and glove-friendly optimizations applied to the Warehouse Scan Android application are documented here.

---

## [Unreleased] - UI/UX Refinement & Data Safety Upgrade

### Added
- **Central Theme Tokens (`ui/theme/Color.kt`, `Theme.kt`, `Type.kt`)**:
  - Centralized WCAG AA compliant colors (`#0B5FFF` Primary, `#12873D` Success, `#C62828` Error, `#B26A00` Warning, `#F3F5F8` Background).
  - 18sp monospace typography for batch number digits across list rows.
  - Uniform 12dp corner radii for cards, text fields, and primary buttons.
- **Password Visibility Toggle (`LoginScreen.kt`)**:
  - Added eye show/hide icon toggle on password input.
  - Made form scrollable (`verticalScroll`) to prevent keyboard layout displacement.
- **Home Screen User Greeting & Service Cards (`HomeScreen.kt`)**:
  - Added greeting `"Hello, Operator"` with profile email text.
  - Redesigned operation selector into two large stacked cards (>= 120dp high) with icons and descriptive subtitles.
- **Batch Deletion Snackbar with Undo (`LocationSettingScreen.kt`)**:
  - Deleting a batch from the list now presents a Snackbar with an **Undo** action.
- **Back Press Confirmation Dialog (`LocationSettingScreen.kt`)**:
  - Pressing system Back or TopAppBar Back with unsaved batches prompts: *"Discard {N} scanned batches?"* to protect against accidental data loss.
- **Prominent Scan Result Banner (`DispatchVerificationScreen.kt`)**:
  - Banner highlights scan results in large display text: **`MATCH`** (Green `#E3F6EA`), **`No Matching Found`** (Red `#FDE8E8`), or **`Already scanned`** (Amber `#FFF3D6`).
- **Glove-Friendly Touch Targets**:
  - Enforced 56dp minimum touch target height across all text inputs, action buttons, and list controls.

### Changed
- **`ScanInputBar` Layout (`Components.kt`)**:
  - Added trailing `(X)` clear icon button to text input when text is entered.
  - Provided separate hardware **Scan** button (DataWedge imager soft scan trigger) and **Camera** icon button (live camera scanner dialog).
- **Dispatch Verification Layout**:
  - Re-architected top metric tiles into a compact single-row container (`CompactMetricCard`), saving 136.dp vertical space on 5.0" handheld displays (Zebra TC26).
  - Positioned bottom Export Report button cleanly above system navigation insets.

### Fixed
- Fixed physical Zebra SE4710 hardware sensor activation for consecutive pulls by removing explicit `STOP_SCANNING` calls in broadcast receiver.
- Removed sample barcode sequence fallbacks (`100055`) from DataWedge controller so camera scanning yields only real barcodes.
