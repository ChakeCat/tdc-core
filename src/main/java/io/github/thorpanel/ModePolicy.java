// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Chooses a window form from the host display, independently of the panel's size. */
final class ModePolicy {
    enum Orientation { PORTRAIT, LANDSCAPE }
    enum Posture { FLAT, TABLETOP, BOOK }
    enum Preference { AUTO, SPLIT, FLOATING }
    enum Mode { DUAL, WM_SPLIT, ADJACENT_SPLIT, FLOATING, MANUAL_SPLIT }

    static final class Host {
        final String deviceKey;
        final int remoteDisplayId, panelDisplayId;
        final Orientation orientation;
        final Posture posture;
        final boolean splitSupported, shellSplitSupported, overlayAllowed;

        Host(String deviceKey, int remoteDisplayId, int panelDisplayId,
                Orientation orientation, Posture posture, boolean splitSupported,
                boolean shellSplitSupported, boolean overlayAllowed) {
            if (deviceKey == null || deviceKey.isEmpty() || remoteDisplayId < 0 || panelDisplayId < 0) {
                throw new IllegalArgumentException("A device and valid display ids are required");
            }
            this.deviceKey = deviceKey;
            this.remoteDisplayId = remoteDisplayId;
            this.panelDisplayId = panelDisplayId;
            this.orientation = Objects.requireNonNull(orientation);
            this.posture = Objects.requireNonNull(posture);
            this.splitSupported = splitSupported;
            this.shellSplitSupported = shellSplitSupported;
            this.overlayAllowed = overlayAllowed;
        }

        boolean dualScreen() { return remoteDisplayId != panelDisplayId; }

        @Override public boolean equals(Object value) {
            if (!(value instanceof Host)) return false;
            Host other = (Host)value;
            return deviceKey.equals(other.deviceKey) && remoteDisplayId == other.remoteDisplayId
                    && panelDisplayId == other.panelDisplayId && orientation == other.orientation
                    && posture == other.posture && splitSupported == other.splitSupported
                    && shellSplitSupported == other.shellSplitSupported && overlayAllowed == other.overlayAllowed;
        }

        @Override public int hashCode() {
            return Objects.hash(deviceKey, remoteDisplayId, panelDisplayId, orientation, posture,
                    splitSupported, shellSplitSupported, overlayAllowed);
        }
    }

    /** The Android adapter can restore these choices from its local preferences. */
    static final class Memory {
        private final Map<String, Map<Orientation, Preference>> devices = new HashMap<>();

        synchronized Preference get(String deviceKey, Orientation orientation) {
            Map<Orientation, Preference> choices = devices.get(deviceKey);
            return choices == null ? Preference.AUTO : choices.getOrDefault(orientation, Preference.AUTO);
        }

        synchronized void remember(String deviceKey, Orientation orientation, Preference preference) {
            Objects.requireNonNull(deviceKey);
            Objects.requireNonNull(orientation);
            Objects.requireNonNull(preference);
            if (preference == Preference.AUTO) {
                Map<Orientation, Preference> choices = devices.get(deviceKey);
                if (choices != null) {
                    choices.remove(orientation);
                    if (choices.isEmpty()) devices.remove(deviceKey);
                }
                return;
            }
            devices.computeIfAbsent(deviceKey, ignored -> new HashMap<>()).put(orientation, preference);
        }
    }

    static final class Plan {
        final Host host;
        final Preference preference;
        final List<Mode> attempts;

        private Plan(Host host, Preference preference, List<Mode> attempts) {
            this.host = host;
            this.preference = preference;
            this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
        }
    }

    private ModePolicy() {}

    static Orientation orientation(float maximumWidth, float maximumHeight) {
        if (!Float.isFinite(maximumWidth) || !Float.isFinite(maximumHeight)
                || maximumWidth <= 0 || maximumHeight <= 0) {
            throw new IllegalArgumentException("A positive host display region is required");
        }
        return maximumWidth > maximumHeight ? Orientation.LANDSCAPE : Orientation.PORTRAIT;
    }

    static Plan choose(Host host, Memory memory) {
        Objects.requireNonNull(host);
        Objects.requireNonNull(memory);
        Preference preference = memory.get(host.deviceKey, host.orientation);
        List<Mode> attempts = new ArrayList<>();
        if (host.dualScreen()) {
            attempts.add(Mode.DUAL);
        } else {
            boolean split = preference == Preference.SPLIT
                    || (preference == Preference.AUTO && host.orientation == Orientation.PORTRAIT);
            if (split && host.splitSupported) {
                // Capability probing selects one split executor. A timed-out request falls back to overlay.
                attempts.add(host.shellSplitSupported ? Mode.WM_SPLIT : Mode.ADJACENT_SPLIT);
            }
            if (host.overlayAllowed) attempts.add(Mode.FLOATING);
            attempts.add(Mode.MANUAL_SPLIT);
        }
        return new Plan(host, preference, attempts);
    }
}
