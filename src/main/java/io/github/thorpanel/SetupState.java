// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/** Setup decisions use live prerequisites; choosing a default IME is optional. */
final class SetupState {
    enum ShizukuStatus {
        MISSING,
        STOPPED,
        UPDATE_REQUIRED,
        NEEDS_PERMISSION,
        PERMISSION_BLOCKED,
        READY
    }
    final ShizukuStatus shizuku;
    final boolean hasKeyboard, lowerAvailable, defaultSelected;
    final DeviceProfile profile;
    SetupState(ShizukuStatus shizuku, boolean hasKeyboard, boolean lowerAvailable,
            boolean defaultSelected) {
        this(shizuku, hasKeyboard, lowerAvailable, defaultSelected, null);
    }
    SetupState(ShizukuStatus shizuku, boolean hasKeyboard, boolean lowerAvailable,
            boolean defaultSelected, DeviceProfile profile) {
        this.shizuku = shizuku;
        this.hasKeyboard = hasKeyboard;
        this.lowerAvailable = lowerAvailable;
        this.defaultSelected = defaultSelected;
        this.profile = profile;
    }
    /** Thor panel needs a usable lower screen; single-screen forms wait for later stages. */
    boolean canUse() {
        if (shizuku != ShizukuStatus.READY || !hasKeyboard)
            return false;
        if (profile != null) {
            if (!profile.apiSupported)
                return false;
            if (profile.form == DeviceProfile.Form.SINGLE)
                return false;
            if (profile.form == DeviceProfile.Form.THOR_DUAL
                    || profile.form == DeviceProfile.Form.GENERIC_DUAL)
                return profile.canUseThorPanel();
        }
        return lowerAvailable;
    }
    boolean showGuide(boolean completed) {
        return !completed || !canUse();
    }
}
