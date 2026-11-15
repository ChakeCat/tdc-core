// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

/** Chooses usable regions from window-local observations. No platform calls occur here. */
final class PanelRegionPolicy {
    enum Orientation { HORIZONTAL, VERTICAL }

    enum Reason {
        FULL_WINDOW, BELOW_HINGE, BOTTOM_DOCK, BELOW_HINGE_DOCK, TABLETOP_DOCK,
        TOO_SMALL, VERTICAL_HINGE, INVALID_DIMENSIONS, INVALID_HINGE
    }

    enum RouteSource { THOR, GENERIC_DUAL, EXTERNAL, SINGLE, UNAVAILABLE }

    static final class Hinge {
        final PanelLayout.Rect bounds;
        final Orientation orientation;
        final boolean separating, tabletop, occluding;

        /** Bounds must use the receiving window's local dp coordinates. */
        Hinge(PanelLayout.Rect bounds, Orientation orientation, boolean separating,
                boolean tabletop, boolean occluding) {
            this.bounds = bounds;
            this.orientation = orientation;
            this.separating = separating;
            this.tabletop = tabletop;
            this.occluding = occluding;
        }

        boolean active() { return separating || tabletop || occluding; }
    }

    static final class Region {
        final PanelLayout.Rect bounds;
        final boolean usable;
        final Reason reason;

        Region(PanelLayout.Rect bounds, boolean usable, Reason reason) {
            this.bounds = bounds;
            this.usable = usable;
            this.reason = reason;
        }
    }

    static final class Route {
        final int remoteId, panelId;
        final boolean dualScreen;
        final RouteSource source;
        final DisplayTopology topology;

        Route(int remoteId, int panelId, RouteSource source) {
            this.remoteId = remoteId;
            this.panelId = panelId;
            this.source = source;
            dualScreen = remoteId >= 0 && panelId >= 0 && remoteId != panelId;
            topology = new DisplayTopology(remoteId, dualScreen ? panelId : DisplayTopology.NONE);
        }
    }

    private static final PanelLayout.Rect EMPTY = new PanelLayout.Rect(0, 0, 0, 0);

    private PanelRegionPolicy() {}

    /**
     * A horizontal hinge leaves the area below it usable. A zero-thickness fold
     * inside the window is valid. Missing or out-of-window observations use the
     * whole window, including windows already split at the fold.
     * A vertical separator requires a future side-aware layout, so input pauses.
     */
    static Region panel(float width, float height, Hinge hinge, float minWidth, float minHeight) {
        if (!positive(width) || !positive(height) || !nonnegative(minWidth) || !nonnegative(minHeight)) {
            return new Region(EMPTY, false, Reason.INVALID_DIMENSIONS);
        }
        PanelLayout.Rect window = new PanelLayout.Rect(0, 0, width, height);
        if (hinge == null || !hinge.active()) return sized(window, minWidth, minHeight, Reason.FULL_WINDOW);
        if (!validHinge(hinge)) return new Region(window, false, Reason.INVALID_HINGE);
        if (!intersects(window, hinge)) return sized(window, minWidth, minHeight, Reason.FULL_WINDOW);
        if (hinge.orientation == Orientation.VERTICAL) return new Region(window, false, Reason.VERTICAL_HINGE);
        PanelLayout.Rect below = new PanelLayout.Rect(0, Math.min(height, Math.max(0, hinge.bounds.bottom)), width, height);
        return sized(below, minWidth, minHeight, Reason.BELOW_HINGE);
    }

    /**
     * Docks at the host bottom without crossing an observed separator. In
     * landscape tabletop posture this places the panel below the hinge.
     * Requested height is bounded by the minimum and available region.
     */
    static Region floating(float hostWidth, float hostHeight, float requestedHeight,
            boolean landscape, Hinge hinge, float minWidth, float minHeight) {
        if (!positive(requestedHeight)) return new Region(EMPTY, false, Reason.INVALID_DIMENSIONS);
        Region available = panel(hostWidth, hostHeight, hinge, minWidth, minHeight);
        if (!available.usable) return available;
        float height = Math.min(available.bounds.height(), Math.max(requestedHeight, minHeight));
        PanelLayout.Rect dock = new PanelLayout.Rect(available.bounds.left, available.bounds.bottom - height,
                available.bounds.right, available.bounds.bottom);
        Reason reason = Reason.BOTTOM_DOCK;
        if (available.reason == Reason.BELOW_HINGE) {
            reason = landscape && hinge.tabletop ? Reason.TABLETOP_DOCK : Reason.BELOW_HINGE_DOCK;
        }
        return new Region(dock, true, reason);
    }

    /**
     * The caller supplies external ids already checked as valid and powered on.
     * It passes thorPreferred only after an explicit Thor capability decision.
     * Do not pass an arbitrary two-screen topology as a Thor preference.
     */
    static Route displayRoute(int localId, int[] externalIds, DisplayTopology thorPreferred) {
        if (thorPreferred != null && thorPreferred.dualScreen
                && thorPreferred.upperId >= 0 && thorPreferred.lowerId >= 0) {
            return new Route(thorPreferred.upperId, thorPreferred.lowerId, RouteSource.THOR);
        }
        if (localId < 0) return new Route(DisplayTopology.NONE, DisplayTopology.NONE, RouteSource.UNAVAILABLE);
        if (externalIds != null) {
            for (int externalId : externalIds) {
                if (externalId >= 0 && externalId != localId) return new Route(externalId, localId, RouteSource.EXTERNAL);
            }
        }
        return new Route(localId, localId, RouteSource.SINGLE);
    }

    private static Region sized(PanelLayout.Rect bounds, float minWidth, float minHeight, Reason reason) {
        boolean usable = bounds.width() > 0 && bounds.height() > 0
                && bounds.width() >= minWidth && bounds.height() >= minHeight;
        return new Region(bounds, usable, usable ? reason : Reason.TOO_SMALL);
    }

    private static boolean positive(float value) { return Float.isFinite(value) && value > 0; }
    private static boolean nonnegative(float value) { return Float.isFinite(value) && value >= 0; }

    private static boolean validHinge(Hinge hinge) {
        if (hinge.bounds == null || hinge.orientation == null) return false;
        PanelLayout.Rect bounds = hinge.bounds;
        if (!Float.isFinite(bounds.left) || !Float.isFinite(bounds.top)
                || !Float.isFinite(bounds.right) || !Float.isFinite(bounds.bottom)) return false;
        return hinge.orientation == Orientation.HORIZONTAL ? bounds.width() > 0 : bounds.height() > 0;
    }

    private static boolean intersects(PanelLayout.Rect window, Hinge hinge) {
        PanelLayout.Rect bounds = hinge.bounds;
        if (hinge.orientation == Orientation.HORIZONTAL) {
            boolean across = bounds.left < window.right && bounds.right > window.left;
            boolean inside = bounds.height() == 0
                    ? bounds.top > window.top && bounds.top < window.bottom
                    : bounds.top < window.bottom && bounds.bottom > window.top;
            return across && inside;
        }
        boolean across = bounds.top < window.bottom && bounds.bottom > window.top;
        boolean inside = bounds.width() == 0
                ? bounds.left > window.left && bounds.left < window.right
                : bounds.left < window.right && bounds.right > window.left;
        return across && inside;
    }
}
