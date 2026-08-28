#!/usr/bin/env python3
"""Build a minimal .apkg (legacy Anki format) to exercise the app's importer.

Usage: python3 tools/make_fixture_apkg.py [output.apkg]
Creates 3 notes (basic, HTML with an image, cloze) and 1 media file, then
reopens the package and validates its own content (self-check).
"""
import base64
import json
import sqlite3
import sys
import tempfile
import zipfile
from pathlib import Path

SEP = "\x1f"

# 1x1 blue PNG
PIXEL_PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYPj/HwADAgH/"
    "qY5dBwAAAABJRU5ErkJggg=="
)

MODELS = {
    "1111": {
        "name": "Basic",
        "type": 0,
        "flds": [{"name": "Front", "ord": 0}, {"name": "Back", "ord": 1}],
    },
    "2222": {
        "name": "Cloze",
        "type": 1,
        "flds": [{"name": "Text", "ord": 0}, {"name": "Extra", "ord": 1}],
    },
}

DECKS = {
    "1": {"name": "Default"},
    "1234": {"name": "Test Deck"},
}

NOTES = [
    (1, 1111, f"What is DNS?{SEP}The domain name system"),
    (2, 1111, f'<b>Image:</b> <img src="pixel.png">{SEP}Back with <i>HTML</i>'),
    (3, 2222, f"The capital of Brazil is {{{{c1::Brasília}}}}{SEP}Extra note"),
]


def build(path: Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        db_path = Path(tmp) / "collection.anki2"
        con = sqlite3.connect(db_path)
        con.executescript(
            """
            CREATE TABLE col (
                id integer primary key, crt integer, mod integer, scm integer,
                ver integer, dty integer, usn integer, ls integer,
                conf text, models text, decks text, dconf text, tags text
            );
            CREATE TABLE notes (
                id integer primary key, guid text, mid integer, mod integer,
                usn integer, tags text, flds text, sfld text, csum integer,
                flags integer, data text
            );
            CREATE TABLE cards (
                id integer primary key, nid integer, did integer, ord integer,
                mod integer, usn integer, type integer, queue integer, due integer,
                ivl integer, factor integer, reps integer, lapses integer,
                left integer, odue integer, odid integer, flags integer, data text
            );
            """
        )
        con.execute(
            "INSERT INTO col VALUES (1, 0, 0, 0, 11, 0, 0, 0, '{}', ?, ?, '{}', '{}')",
            (json.dumps(MODELS), json.dumps(DECKS)),
        )
        for nid, mid, flds in NOTES:
            con.execute(
                "INSERT INTO notes VALUES (?, ?, ?, 0, 0, '', ?, '', 0, 0, '')",
                (nid, str(nid), mid, flds),
            )
        con.commit()
        con.close()

        with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
            z.write(db_path, "collection.anki2")
            z.writestr("0", PIXEL_PNG)
            z.writestr("media", json.dumps({"0": "pixel.png"}))


def self_check(path: Path) -> None:
    with tempfile.TemporaryDirectory() as tmp:
        with zipfile.ZipFile(path) as z:
            names = set(z.namelist())
            assert {"collection.anki2", "media", "0"} <= names, names
            z.extractall(tmp)
        media = json.loads((Path(tmp) / "media").read_text())
        assert media == {"0": "pixel.png"}, media
        con = sqlite3.connect(Path(tmp) / "collection.anki2")
        (count,) = con.execute("SELECT COUNT(*) FROM notes").fetchone()
        assert count == 3, count
        (models_json,) = con.execute("SELECT models FROM col").fetchone()
        models = json.loads(models_json)
        assert models["2222"]["type"] == 1
        flds = [r[0] for r in con.execute("SELECT flds FROM notes ORDER BY id")]
        assert SEP in flds[0]
        assert 'src="pixel.png"' in flds[1]
        assert "{{c1::" in flds[2]
        con.close()
    print(f"OK: {path} ({path.stat().st_size} bytes, 3 notes, 1 media file)")


if __name__ == "__main__":
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).parent / "fixture.apkg"
    build(out)
    self_check(out)
