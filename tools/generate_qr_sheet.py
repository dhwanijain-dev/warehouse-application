#!/usr/bin/env python3
"""Generate the printable TC26 QR test sheet. Requires PyMuPDF and system libqrencode."""
import ctypes
import ctypes.util
from pathlib import Path

import fitz

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/sample/qr_test_sheet.pdf"
ITEMS = [(str(n), f"Batch {n}: accepted") for n in range(100001, 100051)] + [
    ("999999", "Unknown batch: expected not found"),
    ("https://example.com/b/100001", "URL: expected invalid in raw mode"),
    ("100001|ROLL7|2026", "Delimited: expected invalid in raw mode"),
    ('{"batch":"100001"}', "JSON: expected invalid in raw mode"),
    ("not-a-batch!!!", "Garbage: expected invalid"),
]


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    library = ctypes.CDLL(ctypes.util.find_library("qrencode") or "libqrencode.so.4")

    class QRCode(ctypes.Structure):
        _fields_ = [("version", ctypes.c_int), ("width", ctypes.c_int), ("data", ctypes.POINTER(ctypes.c_ubyte))]

    library.QRcode_encodeString.restype = ctypes.POINTER(QRCode)
    library.QRcode_encodeString.argtypes = [ctypes.c_char_p, ctypes.c_int, ctypes.c_int, ctypes.c_int, ctypes.c_int]
    library.QRcode_free.argtypes = [ctypes.POINTER(QRCode)]
    pdf = fitz.open()
    page_w, page_h = 595, 842
    cell_w, cell_h, qr_size = page_w / 4, 155, 82
    for index, (payload, expected) in enumerate(ITEMS):
        per_page = 20
        local = index % per_page
        if index and local == 0:
            pdf.new_page(width=page_w, height=page_h)
        if index == 0:
            page = pdf.new_page(width=page_w, height=page_h)
        else:
            page = pdf[-1]
        col, row = local % 4, local // 4
        x = col * cell_w
        y_top = row * cell_h
        qr = library.QRcode_encodeString(payload.encode(), 0, 0, 2, 1)
        if not qr:
            raise RuntimeError(f"Could not encode {payload!r}")
        code = qr.contents
        matrix = [[code.data[i * code.width + j] for j in range(code.width)] for i in range(code.width)]
        library.QRcode_free(qr)
        margin = 4
        module = qr_size / (code.width + margin * 2)
        origin_x = x + (cell_w - qr_size) / 2 + margin * module
        origin_y = y_top + 12 + margin * module
        shape = page.new_shape()
        for my, line in enumerate(matrix):
            for mx, val in enumerate(line):
                if val & 1:
                    rect = fitz.Rect(origin_x + mx * module, origin_y + my * module,
                                     origin_x + (mx + 1) * module, origin_y + (my + 1) * module)
                    shape.draw_rect(rect)
        shape.finish(color=None, fill=(0, 0, 0))
        shape.commit()
        page.insert_text((x + 3, y_top + 106), payload[:40], fontsize=6, fontname="hebo")
        page.insert_text((x + 3, y_top + 120), expected[:48], fontsize=5, fontname="helv")
    pdf.set_metadata({"title": "Warehouse Scan TC26 QR Test Sheet"})
    pdf.save(OUT)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    main()
