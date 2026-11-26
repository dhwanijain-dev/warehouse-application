# Zebra TC26 QR scan test plan

Run on a TC26 with DataWedge installed. Record Android version, DataWedge version, build flavor, and test date.

- [ ] DataWedge profile is ready and status shows version and last symbology.
- [ ] Hardware trigger scans QR and Code 128 on Location Setting and Dispatch Verification.
- [ ] Scan button starts scanning; leaving the screens disables the imager.
- [ ] QR reads from near and far working distances.
- [ ] Small printed QR and Code 128 labels scan reliably at the closest practical focus distance.
- [ ] Damaged and glossy QR codes produce a clear invalid/unreadable response when unreadable.
- [ ] Low light scanning works with illumination enabled.
- [ ] Rapidly scan 20 distinct codes; confirm all 20 are accounted for in order.
- [ ] Scan repeated code rapidly; confirm debounce suppresses only immediate repeated reads.
- [ ] Scanning works after screen off/on and after background/foreground transitions.
- [ ] Camera scanning permission prompt, preview, torch, cancel, and QR capture work without Play Services.
- [ ] Missing/blocked DataWedge permits camera or manual entry fallback.
- [ ] Invalid URL, delimited, JSON, and garbage samples match configured payload extraction expectations.
