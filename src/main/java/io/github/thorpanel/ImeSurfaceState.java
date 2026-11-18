// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT
package io.github.thorpanel;

import java.util.regex.Pattern;
import java.util.Set;

/** Read composed output, not WM's cached visibility or off-screen layer records. */
final class ImeSurfaceState {
    static boolean visible(String dump, int display) {
        return visible(dump, display, java.util.Collections.emptySet(), true);
    }
    static boolean visible(String dump, String windows, String imeId, int display) {
        // The main surface can contain only navigation, even after a floating window closes.
        // Require its own key area independently of any floating-window candidates.
        boolean mainKeys = ImeWindowState.hasLowerMainKeys(windows, imeId, display);
        return visible(dump, display,
                ImeWindowState.floatingSurfaceNames(windows, imeId, display), mainKeys);
    }
    private static boolean visible(
            String dump, int display, Set<String> floatingNames, boolean mainKeys) {
        if (dump == null || display < 0)
            return false;
        for (String output : dump.split("(?m)^Display ")) {
            if (!output.contains("Composition Display State:")
                    || !output.contains("layerFilter={layerStack=" + display + " "))
                continue;
            for (String layer : output.split(" - Output Layer ")) {
                int end = layer.indexOf('\n');
                if (end < 0)
                    continue;
                String name = layer.substring(0, end);
                boolean keyboard = mainKeys
                        && Pattern.compile("\\(InputMethod#\\d+\\)").matcher(name).find();
                for (String floating : floatingNames)
                    if (Pattern.compile("\\(" + Pattern.quote(floating) + "#\\d+\\)")
                                    .matcher(name).find()) {
                        keyboard = true;
                        break;
                    }
                if (!keyboard)
                    continue;
                if (Pattern.compile("Region visibleRegion[^\\n]*count=[1-9]\\d*\\)")
                                .matcher(layer)
                                .find())
                    return true;
            }
        }
        return false;
    }
}
