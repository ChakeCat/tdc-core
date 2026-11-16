// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

/** Some dual-screen IMEs draw a Presentation instead of a standard IME surface. */
final class ImeWindowState {
    enum HorizontalBounds { FITS, OVERFLOW, UNKNOWN }

    /**
     * Check only visible keyboard window frames against the current logical display width.
     * A fitting outer window does not prove that the IME's own keys fit inside it.
     */
    static HorizontalBounds horizontalBounds(
            String dump, String imeId, int displayId, int displayWidth) {
        if (dump == null || imeId == null || imeId.indexOf('/') <= 0
                || displayId < 0 || displayWidth <= 0)
            return HorizontalBounds.UNKNOWN;
        java.util.regex.Pattern display = java.util.regex.Pattern.compile(
                "\\bmDisplayId=" + displayId + "(?=\\s|$)");
        java.util.regex.Pattern owner = java.util.regex.Pattern.compile(
                "(?:^|\\s)package="
                + java.util.regex.Pattern.quote(imeId.substring(0, imeId.indexOf('/')))
                + "(?=\\s|$)");
        java.util.regex.Pattern type = java.util.regex.Pattern.compile(
                "\\bty=(INPUT_METHOD|PRESENTATION|APPLICATION_ATTACHED_DIALOG)(?=[\\s}])");
        java.util.regex.Pattern frame = java.util.regex.Pattern.compile(
                "(?m)^\\s*Frames:[^\\r\\n]*?\\bframe=\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)"
                + "\\s*\\]\\s*\\[\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\]");
        boolean fits = false, unknown = false;
        String[] windows = dump.split("(?m)^\\s*Window #\\d+\\s+");
        for (int i = 1; i < windows.length; i++) {
            String window = windows[i];
            java.util.regex.Matcher kind = type.matcher(window);
            if (!display.matcher(window).find() || !owner.matcher(window).find()
                    || !window.matches("(?s).*\\bisVisible=true(?=\\s|$).*")
                    || !window.matches("(?s).*\\bmDrawState=HAS_DRAWN(?=\\s|$).*")
                    || !window.matches("(?s).*\\bSurface: shown=true(?=\\s|$).*")
                    || !kind.find())
                continue;
            if (kind.group(1).equals("APPLICATION_ATTACHED_DIALOG")
                    && !java.util.regex.Pattern.compile(
                            "\\bmParentWindow=Window\\{[^}\\r\\n]*\\sInputMethod\\}")
                                .matcher(window).find())
                continue;
            java.util.regex.Matcher bounds = frame.matcher(window);
            if (!bounds.find()) {
                unknown = true;
                continue;
            }
            try {
                int left = Integer.parseInt(bounds.group(1));
                int top = Integer.parseInt(bounds.group(2));
                int right = Integer.parseInt(bounds.group(3));
                int bottom = Integer.parseInt(bounds.group(4));
                if (right <= left || bottom <= top) {
                    unknown = true;
                    continue;
                }
                if (left < 0 || right > displayWidth)
                    return HorizontalBounds.OVERFLOW;
                fits = true;
            } catch (NumberFormatException invalidBounds) {
                unknown = true;
            }
        }
        return fits && !unknown ? HorizontalBounds.FITS : HorizontalBounds.UNKNOWN;
    }

    static boolean hasLowerKeys(String dump, String imeId) {
        return hasLowerKeys(dump, imeId, 4);
    }
    static boolean hasLowerKeys(String dump, String imeId, int displayId) {
        return hasLowerKeys(dump, imeId, displayId, true);
    }
    /** Separate windows can display keys even when the standard IME surface stays empty. */
    static boolean hasLowerFloatingKeys(String dump, String imeId, int displayId) {
        return hasLowerKeys(dump, imeId, displayId, false);
    }
    private static boolean hasLowerKeys(
            String dump, String imeId, int displayId, boolean includeMainWindow) {
        if (dump == null || imeId == null || !imeId.contains("/") || displayId < 0)
            return false;
        String marker = "mDisplayId=" + displayId + " ";
        String owner = "package=" + imeId.substring(0, imeId.indexOf('/')) + " ";
        for (String window : dump.split("\n  Window #")) {
            if (!window.contains(marker) || !window.contains(owner)
                    || !window.contains("isVisible=true")
                    || !window.contains("mDrawState=HAS_DRAWN")
                    || !window.contains("Surface: shown=true"))
                continue;
            if (window.contains("ty=PRESENTATION "))
                return true;
            if (window.contains("ty=APPLICATION_ATTACHED_DIALOG ")
                    && window.contains("mParentWindow=Window{")
                    && window.contains(" u0 InputMethod}"))
                return true;
            if (!includeMainWindow || !window.contains("ty=INPUT_METHOD "))
                continue;
            java.util.regex.Matcher height =
                    java.util.regex.Pattern.compile("Requested w=\\d+ h=(\\d+)").matcher(window);
            java.util.regex.Matcher top =
                    java.util.regex.Pattern.compile("mGivenContentInsets=\\[0,(\\d+)\\]")
                            .matcher(window);
            // AYN leaves a navigation strip when the floating keys failed to
            // open. It occupies ~10% of the screen, not a usable keyboard.
            if (height.find() && top.find()
                    && Integer.parseInt(top.group(1)) < Integer.parseInt(height.group(1)) * .8)
                return true;
        }
        return false;
    }
    static boolean hasLowerPresentation(String dump, String imeId) {
        return hasLowerPresentation(dump, imeId, 4);
    }
    static boolean hasLowerPresentation(String dump, String imeId, int displayId) {
        if (imeId == null || !imeId.contains("/") || displayId < 0)
            return false;
        String marker = "mDisplayId=" + displayId + " ";
        String owner = "package=" + imeId.substring(0, imeId.indexOf('/')) + " ";
        for (String window : dump.split("\\n  Window #"))
            if (window.contains(marker) && window.contains(owner)
                    && window.contains("ty=PRESENTATION ") && window.contains("isVisible=true")
                    && window.contains("mDrawState=HAS_DRAWN")
                    && window.contains("Surface: shown=true"))
                return true;
        return false;
    }
}
