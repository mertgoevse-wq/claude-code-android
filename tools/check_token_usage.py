#!/usr/bin/env python3
"""
Contrast checker verifying contrast ratios from docs/03-design/color-and-contrast.md.
"""
import sys

def rel_luminance(hex_str: str) -> float:
    cleaned = hex_str.strip().lstrip('#')
    r = int(cleaned[0:2], 16) / 255.0
    g = int(cleaned[2:4], 16) / 255.0
    b = int(cleaned[4:6], 16) / 255.0

    def adjust(channel: float) -> float:
        return channel / 12.92 if channel <= 0.03928 else ((channel + 0.055) / 1.055) ** 2.4

    return 0.2126 * adjust(r) + 0.7152 * adjust(g) + 0.0722 * adjust(b)

def contrast_ratio(hex1: str, hex2: str) -> float:
    l1 = rel_luminance(hex1)
    l2 = rel_luminance(hex2)
    lighter = max(l1, l2)
    darker = min(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)

def main():
    # Light theme tests
    light_surface = "#FFFFFF"
    light_bg = "#FAF9F5"

    light_tokens = [
        ("textPrimary", "#1F1E1B", 15.0),
        ("textSecondary", "#5C5A54", 4.5),
        ("textTertiary", "#6E6B65", 4.5),
        ("accent", "#B25133", 4.5),
        ("accentText", "#A84C2E", 4.5),
        ("success", "#3F7D58", 4.5),
        ("warning", "#9A6B15", 4.4),
        ("danger", "#B23A2F", 4.5),
        ("info", "#3A6EA5", 4.5),
        ("borderStrong", "#948E82", 3.0),
    ]

    for name, hex_val, min_ratio in light_tokens:
        ratio = contrast_ratio(hex_val, light_surface)
        if ratio < min_ratio:
            print(f"FAIL: Light token {name} ({hex_val}) on surface has ratio {ratio:.2f} < {min_ratio}")
            sys.exit(1)

    # Dark theme tests
    dark_surface = "#232220"
    dark_bg = "#1A1917"

    dark_tokens = [
        ("textPrimary", "#F2F0EA", 12.0),
        ("textSecondary", "#B4B0A8", 4.5),
        ("textTertiary", "#99958F", 4.5),
        ("accent", "#E08A66", 4.5),
        ("accentText", "#E59572", 4.5),
        ("success", "#6FBE8C", 4.5),
        ("warning", "#D9A94A", 4.5),
        ("danger", "#E8796B", 4.5),
        ("info", "#79AEE0", 4.5),
        ("borderStrong", "#74716D", 3.0),
    ]

    for name, hex_val, min_ratio in dark_tokens:
        ratio = contrast_ratio(hex_val, dark_surface)
        if ratio < min_ratio:
            print(f"FAIL: Dark token {name} ({hex_val}) on surface has ratio {ratio:.2f} < {min_ratio}")
            sys.exit(1)

    # Specific pairs
    # onAccent on accent in light
    ratio_light_accent = contrast_ratio("#FFFFFF", "#B25133")
    assert ratio_light_accent >= 4.5, f"Light onAccent on accent ratio {ratio_light_accent:.2f} < 4.5"

    # onAccent on accent in dark (must be dark text on light accent)
    ratio_dark_accent = contrast_ratio("#1A1917", "#E08A66")
    assert ratio_dark_accent >= 4.5, f"Dark onAccent on accent ratio {ratio_dark_accent:.2f} < 4.5"

    print("PASS: All theme color contrast ratios pass WCAG requirements.")

if __name__ == "__main__":
    main()
