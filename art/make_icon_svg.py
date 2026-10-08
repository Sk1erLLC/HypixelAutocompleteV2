"""Build icon.svg for Hypixel Autocomplete from plain vector shapes.

The lettering is Montserrat (SIL OFL 1.1, https://github.com/JulietaUla/Montserrat)
converted to outlines, so the SVG renders the same everywhere with no font installed.

    pip install fonttools
    curl -LO "https://github.com/google/fonts/raw/main/ofl/montserrat/Montserrat%5Bwght%5D.ttf"
    python3 art/make_icon_svg.py "Montserrat[wght].ttf" art/icon.svg
    rsvg-convert -w 1024 -h 1024 art/icon.svg -o src/main/resources/assets/hypixel_auto_complete/icon.png
"""
import argparse

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

SIZE = 1024
BG_TOP, BG_BOTTOM = "#233335", "#2a393c"
TILE_TOP, TILE_BOTTOM = "#0d1d1d", "#122627"
YELLOW = "#f4c048"
CREAM = "#f6eedb"

# (text, baseline y, cap height, ink width) measured from the original 1024px icon
LINES = [("HYPIXEL", 170, 86, 502), ("AUTOCOMPLETE", 282, 82, 880)]
TRACKING = 0.0  # extra letter spacing, as a fraction of the em


def text_path(font, text, baseline, cap_height, ink_width):
    glyph_set = font.getGlyphSet()
    cmap = font.getBestCmap()
    scale = cap_height / font["OS/2"].sCapHeight
    em = font["head"].unitsPerEm
    names = [cmap[ord(ch)] for ch in text]
    advances = [glyph_set[n].width + TRACKING * em for n in names]
    # ink extent in font units: first glyph's left bearing to last glyph's right edge
    glyf = font["glyf"]
    first, last = glyf[names[0]], glyf[names[-1]]
    first.recalcBounds(glyf); last.recalcBounds(glyf)
    ink = sum(advances[:-1]) + last.xMax - first.xMin
    # squeeze horizontally so the line spans the same width as the original
    sx = ink_width / ink
    x = (SIZE - ink_width) / 2 - first.xMin * sx
    pen = SVGPathPen(glyph_set)
    for name, advance in zip(names, advances):
        glyph_set[name].draw(TransformPen(pen, (sx, 0, 0, -scale, x, baseline)))
        x += advance * sx
    return pen.getCommands()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("font", help="Montserrat variable font (Montserrat[wght].ttf)")
    parser.add_argument("out", help="output .svg path")
    parser.add_argument("--weight", type=float, default=740)
    args = parser.parse_args()

    font = instantiateVariableFont(TTFont(args.font), {"wght": args.weight})
    lettering = " ".join(text_path(font, *line) for line in LINES)

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="{SIZE}" height="{SIZE}" viewBox="0 0 {SIZE} {SIZE}">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="{BG_TOP}"/>
      <stop offset="1" stop-color="{BG_BOTTOM}"/>
    </linearGradient>
    <linearGradient id="tile" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="{TILE_TOP}"/>
      <stop offset="1" stop-color="{TILE_BOTTOM}"/>
    </linearGradient>
  </defs>
  <rect width="{SIZE}" height="{SIZE}" fill="url(#bg)"/>
  <path fill="{YELLOW}" d="{lettering}"/>
  <rect x="198" y="335" width="628" height="575" rx="130" fill="url(#tile)"/>
  <g fill="none" stroke="{CREAM}" stroke-linecap="round" stroke-linejoin="round">
    <line x1="504.4" y1="422.5" x2="397.2" y2="798.5" stroke-width="47"/>
    <path d="M553 684.5 H695 M636 625.5 L695 684.5 L636 743.5" stroke-width="34"/>
  </g>
</svg>
"""
    with open(args.out, "w") as f:
        f.write(svg)


if __name__ == "__main__":
    main()
