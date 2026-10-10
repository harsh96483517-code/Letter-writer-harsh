#!/usr/bin/env python3
"""Validates the built-in Writer templates. Usage: python3 tools/validate_templates.py [templates-dir]

Exits 0 and prints OK when every template is well formed; otherwise prints each problem.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_DIR = os.path.join(ROOT, "android", "app", "src", "main", "assets", "templates")

EXPECTED = {
    "gov": [
        "gov_dm", "gov_sdm", "gov_tehsildar", "gov_bdo", "gov_gram_panchayat",
        "gov_vdo", "gov_lekhpal", "gov_police_station", "gov_character_cert", "gov_residence_cert",
    ],
    "education": [
        "edu_school_admission", "edu_college_admission", "edu_scholarship", "edu_bonafide",
        "edu_transfer_cert", "edu_record_correction", "edu_exam", "edu_leave",
        "edu_fee_concession", "edu_principal",
    ],
    "schemes": [
        "sch_ration_correction", "sch_ration_add_member", "sch_ration_remove_member", "sch_aadhaar",
        "sch_income_cert", "sch_caste_cert", "sch_domicile_cert", "sch_birth_cert_correction",
        "sch_pension", "sch_pm_kisan",
    ],
    "complaints": [
        "cmp_electricity", "cmp_water", "cmp_road", "cmp_garbage", "cmp_drainage",
        "cmp_streetlight", "cmp_public_service", "cmp_consumer", "cmp_bank_service", "cmp_postal",
    ],
    "banking": [
        "bank_account_correction", "bank_address_update", "bank_passbook", "bank_atm_card",
        "bank_transaction_complaint", "bank_statement", "bank_loan",
    ],
    "employment": [
        "job_application", "job_cover_letter", "job_internship", "job_experience_cert",
        "job_salary_cert", "job_resignation", "job_leave",
    ],
    "personal": [
        "per_formal", "per_thank_you", "per_permission", "per_invitation", "per_apology", "per_request",
    ],
}
STANDARD_KEYS = {
    "name", "father", "address", "village", "post", "block", "district", "state", "pin", "mobile",
    "date", "recipient_district", "recipient_office", "recipient_designation",
}
STYLES = {"application", "personal"}
DEVANAGARI = re.compile(r"[ऀ-ॿ]")
PLACEHOLDER = re.compile(r"\{\{([a-z0-9_]+)\}\}")
GENDERED_HI = ["ता हूँ", "ती हूँ", "ता हूं", "ती हूं", "ता था", "ती थी", "ता रहा", "ती रही", "चाहता", "चाहती"]


def main():
    directory = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_DIR
    errors, warnings = [], []
    seen = {}

    def err(tid, msg):
        errors.append(f"{tid}: {msg}")

    if not os.path.isdir(directory):
        print(f"No such directory: {directory}")
        return 1

    for name in sorted(os.listdir(directory)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(directory, name)
        try:
            with open(path, encoding="utf-8") as handle:
                data = json.load(handle)
        except Exception as exc:  # noqa: BLE001
            errors.append(f"{name}: not valid JSON ({exc})")
            continue
        if not isinstance(data, list):
            errors.append(f"{name}: top level must be a JSON array")
            continue
        for tpl in data:
            check_template(tpl, name, seen, err, warnings)

    for category, ids in EXPECTED.items():
        for tid in ids:
            if tid not in seen:
                errors.append(f"missing template {tid} (category {category})")
            elif seen[tid] != category:
                errors.append(f"{tid}: category is {seen[tid]}, expected {category}")
    expected_ids = {tid for ids in EXPECTED.values() for tid in ids}
    for tid in seen:
        if tid not in expected_ids:
            errors.append(f"{tid}: unexpected id")

    for message in warnings:
        print("warning:", message)
    if errors:
        for message in errors:
            print("error:", message)
        print(f"{len(errors)} error(s)")
        return 1
    print(f"OK ({len(seen)} templates)")
    return 0


def check_template(tpl, filename, seen, err, warnings):
    tid = tpl.get("id", f"<no id in {filename}>") if isinstance(tpl, dict) else f"<non-object in {filename}>"
    if not isinstance(tpl, dict):
        err(tid, "template must be an object")
        return
    if not re.fullmatch(r"[a-z0-9_]+", str(tid)):
        err(tid, "bad id")
    if tid in seen:
        err(tid, "duplicate id")
    seen[tid] = tpl.get("category")
    if tpl.get("category") not in EXPECTED:
        err(tid, f"bad category {tpl.get('category')!r}")
    if tpl.get("style") not in STYLES:
        err(tid, f"style must be one of {sorted(STYLES)}")

    for key, limit in (("titleHi", 40), ("titleEn", 40), ("descHi", 70), ("descEn", 70)):
        value = tpl.get(key)
        if not isinstance(value, str) or not value.strip():
            err(tid, f"{key} missing")
            continue
        if len(value) > limit:
            err(tid, f"{key} is {len(value)} characters (max {limit})")
        check_script(tid, key, value, key.endswith("Hi"), err)

    keywords = tpl.get("keywords")
    if not isinstance(keywords, list) or len(keywords) < 5 or not all(isinstance(k, str) and k.strip() for k in keywords):
        err(tid, "keywords must be a list of at least 5 non-empty strings")
    elif not any(DEVANAGARI.search(k) for k in keywords):
        err(tid, "keywords need at least one Hindi word")

    recipient = tpl.get("recipient")
    if recipient is None:
        if tpl.get("style") == "application":
            err(tid, "application style needs a recipient")
    else:
        for lang in ("hi", "en"):
            block = recipient.get(lang) if isinstance(recipient, dict) else None
            if not isinstance(block, dict) or not str(block.get("designation", "")).strip():
                err(tid, f"recipient.{lang}.designation missing")
                continue
            check_script(tid, f"recipient.{lang}.designation", block["designation"], lang == "hi", err)
            if "office" in block and block["office"]:
                check_script(tid, f"recipient.{lang}.office", block["office"], lang == "hi", err)

    for part, lo_hi, lo_en, hi_max in (
        ("salutation", 3, 3, 60),
        ("closing", 3, 3, 60),
        ("subject", 8, 12, 130),
        ("body", 250, 330, 2000),
    ):
        block = tpl.get(part)
        if not isinstance(block, dict):
            err(tid, f"{part} must have hi and en")
            continue
        for lang, minimum in (("hi", lo_hi), ("en", lo_en)):
            text = block.get(lang)
            if not isinstance(text, str):
                err(tid, f"{part}.{lang} missing")
                continue
            if len(text.strip()) < minimum:
                err(tid, f"{part}.{lang} too short ({len(text.strip())} < {minimum})")
            if len(text) > hi_max:
                err(tid, f"{part}.{lang} too long ({len(text)} > {hi_max})")
            check_script(tid, f"{part}.{lang}", text, lang == "hi", err)
            if "$" in text or "\r" in text or "TODO" in text or "<" in text:
                err(tid, f"{part}.{lang} contains a forbidden character or marker")
            if re.search(r"[{}]", PLACEHOLDER.sub("", text)):
                err(tid, f"{part}.{lang} has malformed {{{{placeholder}}}} braces")
            if part == "body":
                if "\n\n" in text:
                    err(tid, "body.%s must not contain blank lines (use a single newline)" % lang)
                paragraphs = [p for p in text.split("\n") if p.strip()]
                if not 3 <= len(paragraphs) <= 6:
                    err(tid, f"body.{lang} has {len(paragraphs)} paragraphs (need 3 to 6)")
            if lang == "hi":
                for pattern in GENDERED_HI:
                    if pattern in text:
                        err(tid, f"{part}.hi uses a gendered form '{pattern}'")

    fields = tpl.get("fields")
    field_keys = []
    if not isinstance(fields, list) or not 1 <= len(fields) <= 4:
        err(tid, "fields must have 1 to 4 entries")
        fields = []
    for field in fields:
        key = field.get("key") if isinstance(field, dict) else None
        if not isinstance(key, str) or not re.fullmatch(r"[a-z0-9_]+", key):
            err(tid, f"bad field key {key!r}")
            continue
        if key in STANDARD_KEYS:
            err(tid, f"field key {key} clashes with a standard key")
        if key in field_keys:
            err(tid, f"duplicate field key {key}")
        field_keys.append(key)
        for label in ("labelHi", "labelEn", "hintHi", "hintEn"):
            value = field.get(label)
            if not isinstance(value, str) or not value.strip():
                err(tid, f"field {key}.{label} missing")
            else:
                check_script(tid, f"field {key}.{label}", value, label.endswith("Hi"), err)
        for flag in ("multiline", "required"):
            if not isinstance(field.get(flag), bool):
                err(tid, f"field {key}.{flag} must be true or false")

    used = set()
    for part in ("subject", "body"):
        block = tpl.get(part)
        if isinstance(block, dict):
            for lang in ("hi", "en"):
                text = block.get(lang)
                if isinstance(text, str):
                    keys = set(PLACEHOLDER.findall(text))
                    used |= keys
                    for key in keys:
                        if key not in STANDARD_KEYS and key not in field_keys:
                            err(tid, f"{part}.{lang} uses undeclared placeholder {{{{{key}}}}}")
    body = tpl.get("body") if isinstance(tpl.get("body"), dict) else {}
    for key in field_keys:
        for lang in ("hi", "en"):
            if key not in PLACEHOLDER.findall(str(body.get(lang, ""))):
                err(tid, f"field {key} is not used in body.{lang}")


def check_script(tid, label, value, is_hindi, err):
    has_dev = bool(DEVANAGARI.search(value))
    if is_hindi and not has_dev:
        err(tid, f"{label} should be Hindi (Devanagari)")
    if not is_hindi and has_dev:
        err(tid, f"{label} should be English but contains Devanagari")


if __name__ == "__main__":
    sys.exit(main())
