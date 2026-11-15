// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/**
 * Screen ids for the current device. Upper is the default display when present.
 * Lower is the preferred non-default presentation display, else any other valid on screen.
 */
public final class DisplayTopology {
    public static final int NONE = -1;
    public final int upperId, lowerId;
    public final boolean dualScreen;

    public DisplayTopology(int upperId, int lowerId) {
        this.upperId = upperId;
        this.lowerId = lowerId;
        this.dualScreen = upperId != NONE && lowerId != NONE && upperId != lowerId;
    }

    /** Prefer a known lower id (Thor historically uses 4) when several second screens exist. */
    public static DisplayTopology fromIds(
            int[] ids, boolean[] valid, boolean[] on, int preferredLower) {
        if (ids == null || ids.length == 0)
            return new DisplayTopology(NONE, NONE);
        int upper = NONE;
        for (int i = 0; i < ids.length; i++) {
            if (!available(ids, valid, on, i))
                continue;
            if (ids[i] == 0) {
                upper = 0;
                break;
            }
            if (upper == NONE || (upper == preferredLower && ids[i] != preferredLower))
                upper = ids[i];
        }
        int lower = NONE;
        for (int i = 0; i < ids.length; i++) {
            if (!available(ids, valid, on, i) || ids[i] == upper)
                continue;
            if (ids[i] == preferredLower) {
                lower = ids[i];
                break;
            }
            if (lower == NONE)
                lower = ids[i];
        }
        return new DisplayTopology(upper, lower);
    }

    private static boolean available(int[] ids, boolean[] valid, boolean[] on, int index) {
        return ids[index] >= 0 && (valid == null || (index < valid.length && valid[index]))
                && (on == null || (index < on.length && on[index]));
    }

    public boolean hasLower() {
        return lowerId != NONE;
    }
    public boolean hasUpper() {
        return upperId != NONE;
    }
}
