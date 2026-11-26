# Open Questions for Business / Stakeholders

1. **Rack/Zone/Setting in TXT**: Confirm whether Rack, Zone, and Setting should be included as header lines inside the TXT file (`txtFile.includeLocationLines`) or kept exclusively in the filename.
2. **Total Batches Scanned Definition**: Confirm unique scanned batches as the primary headline vs total scan attempts.
3. **Batch Number Format & Case Sensitivity**: Confirm batch barcode regex and case-sensitivity requirements.
4. **Duplicates in Uploaded File**: Confirm whether duplicates in uploaded dispatch file are ignored or rejected.
5. **SMTP Account**: Confirm whether client will provide dedicated SMTP credentials for production email dispatch or rely on device intent email / save-to-downloads.
