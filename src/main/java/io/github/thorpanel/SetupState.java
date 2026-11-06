// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/** Setup decisions use live prerequisites; choosing a default IME is optional. */
final class SetupState {
    enum ShizukuStatus { MISSING, STOPPED, UPDATE_REQUIRED, NEEDS_PERMISSION, PERMISSION_BLOCKED, READY }
    final ShizukuStatus shizuku;
    final boolean hasKeyboard, lowerAvailable, defaultSelected;
    SetupState(ShizukuStatus shizuku,boolean hasKeyboard,boolean lowerAvailable,boolean defaultSelected){
        this.shizuku=shizuku;this.hasKeyboard=hasKeyboard;this.lowerAvailable=lowerAvailable;this.defaultSelected=defaultSelected;
    }
    boolean canUse(){return shizuku==ShizukuStatus.READY&&hasKeyboard&&lowerAvailable;}
    boolean showGuide(boolean completed){return !completed||!canUse();}
}
