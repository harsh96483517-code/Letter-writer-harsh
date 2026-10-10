#!/usr/bin/env bash
# Downloads the Noto fonts (SIL Open Font License 1.1) that the A4 preview and PDF export use.
# Run from the repository root:  bash tools/fetch_fonts.sh [output-dir]
# The fonts are plain .ttf files in res/font, so nothing needs to be fetched at app run time.
set -u
OUT="${1:-android/app/src/main/res/font}"
mkdir -p "$OUT"
BASE="https://raw.githubusercontent.com/notofonts/notofonts.github.io/main/fonts"
failed=0

get() { # <res name> <family dir> <file stem>
  local out="$OUT/$1.ttf"
  if [ -s "$out" ]; then echo "have $1"; return 0; fi
  for variant in unhinted hinted full; do
    if curl -fsSL --retry 3 --max-time 120 -o "$out" "$BASE/$2/$variant/ttf/$3.ttf" && [ -s "$out" ]; then
      echo "ok   $1 ($variant, $(stat -c %s "$out") bytes)"
      return 0
    fi
  done
  rm -f "$out"
  echo "FAIL $1"
  failed=1
}

get noto_sans_devanagari_regular NotoSansDevanagari NotoSansDevanagari-Regular
get noto_sans_devanagari_bold    NotoSansDevanagari NotoSansDevanagari-Bold
get noto_serif_devanagari_regular NotoSerifDevanagari NotoSerifDevanagari-Regular
get noto_serif_devanagari_bold    NotoSerifDevanagari NotoSerifDevanagari-Bold

get noto_serif_regular     NotoSerif NotoSerif-Regular
get noto_serif_bold        NotoSerif NotoSerif-Bold
get noto_serif_italic      NotoSerif NotoSerif-Italic
get noto_serif_bold_italic NotoSerif NotoSerif-BoldItalic

get noto_sans_regular     NotoSans NotoSans-Regular
get noto_sans_bold        NotoSans NotoSans-Bold
get noto_sans_italic      NotoSans NotoSans-Italic
get noto_sans_bold_italic NotoSans NotoSans-BoldItalic

mkdir -p android/app/src/main/assets/licenses
if curl -fsSL --retry 3 -o android/app/src/main/assets/licenses/NOTO-OFL.txt \
  "https://raw.githubusercontent.com/notofonts/notofonts.github.io/main/LICENSE"; then
  echo "ok   license"
else
  echo "FAIL license"
  failed=1
fi

exit $failed
