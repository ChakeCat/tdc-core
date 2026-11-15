// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/**
 * Pure setup page machine for Thor. Single-screen pages are reserved for a later stage.
 * Callers pass live detection; this class only decides the current page and actions.
 */
final class SetupFlow {
    enum DeviceKind { THOR, SINGLE }
    enum Page { ENABLE_WIRELESS, PAIR_CODE, PICK_IME, LOWER_INPUT, DONE, RECONNECT, PICK_REMOTE }
    enum Action {
        NONE,
        OPEN_WIRELESS_SETTINGS,
        OPEN_DEVELOPER_OPTIONS,
        OPEN_SYSTEM_SETTINGS,
        USE_SHIZUKU,
        RETRY_SEARCH,
        ENTER_MANUAL_PORTS,
        CANCEL_CONNECT,
        SUBMIT_PAIR,
        START_SERVICE,
        ENABLE_OTHER_IME,
        CHOOSE_IME,
        ENABLE_LOWER_INPUT,
        SKIP_LOWER_INPUT,
        OPEN_TOUCHPAD,
        TOGGLE_AUTO_CONNECT,
        RECONNECT,
        START_BACKGROUND_PAIRING,
        CANCEL_BACKGROUND_PAIRING,
        IN_APP_PAIRING,
        PICK_REMOTE_APP,
        VIEW_RECOVERY
    }

    /** Live inputs observed by the app. */
    static final class Snapshot {
        DeviceKind device = DeviceKind.THOR;
        boolean serviceReady;
        boolean pairedCredentials;
        boolean authInvalid;
        boolean connecting;
        boolean lateResult;
        boolean searchFailed;
        boolean searchTimedOut;
        boolean manualPortMode;
        boolean propertyFallback;
        int pairingPort;
        int connectionPort;
        boolean hasKeyboard;
        boolean chooseKeyboard;
        boolean singleInApp;
        boolean backgroundPairing;
        boolean remoteSelected;
        boolean lowerInputDecided;
        boolean lowerInputEnabled;
        boolean lowerDisplayAvailable;
        boolean autoConnectEnabled;
        boolean writeSecureGranted;
        boolean androidBelow11;
        boolean shizukuRunning;
        boolean provisionIncomplete;
        boolean guideCompleted;
        /** When true, reopen guide only for wireless reconnect (settings entry). */
        boolean wirelessOnly;
    }

    static final class Result {
        final Page page;
        final int step;
        final int stepCount;
        final Action primary;
        final Action secondary;
        final boolean showManualPorts;
        final boolean showPairFields;
        final boolean showRecovery;
        final boolean showAutoConnect;
        final boolean canCancel;
        Result(Page page, int step, int stepCount, Action primary, Action secondary,
                boolean showManualPorts, boolean showPairFields, boolean showRecovery,
                boolean showAutoConnect, boolean canCancel) {
            this.page = page;
            this.step = step;
            this.stepCount = stepCount;
            this.primary = primary;
            this.secondary = secondary;
            this.showManualPorts = showManualPorts;
            this.showPairFields = showPairFields;
            this.showRecovery = showRecovery;
            this.showAutoConnect = showAutoConnect;
            this.canCancel = canCancel;
        }
    }

    static final long SEARCH_TIMEOUT_MS = 10_000L;

    private SetupFlow() {
    }

    static Result decide(Snapshot s) {
        if (s == null)
            s = new Snapshot();
        if (s.device == DeviceKind.SINGLE)
            return single(s);
        if (s.wirelessOnly && s.serviceReady) {
            return page(s, Page.DONE, Action.NONE, Action.NONE, false);
        }
        if (s.guideCompleted && !s.serviceReady && !s.wirelessOnly) {
            return reconnect(s);
        }
        if (!s.serviceReady)
            return connection(s);
        if (!s.hasKeyboard || s.chooseKeyboard)
            return ime(s);
        if (!s.lowerInputDecided)
            return lower(s);
        return done(s);
    }

    private static Result single(Snapshot s) {
        if (!s.serviceReady) {
            if (s.singleInApp || s.androidBelow11)
                return connection(s);
            return new Result(Page.ENABLE_WIRELESS, 1, 3,
                    s.backgroundPairing ? Action.CANCEL_BACKGROUND_PAIRING
                                        : Action.START_BACKGROUND_PAIRING,
                    Action.IN_APP_PAIRING, false, false, false, false, s.backgroundPairing);
        }
        if (s.wirelessOnly)
            return page(s, Page.DONE, Action.NONE, Action.NONE, false);
        if (!s.remoteSelected)
            return page(s, Page.PICK_REMOTE, Action.PICK_REMOTE_APP, Action.NONE, false);
        return done(s);
    }

    private static Result connection(Snapshot s) {
        Page page = connectionPage(s);
        if (s.androidBelow11) {
            return page(s, Page.ENABLE_WIRELESS, Action.USE_SHIZUKU, Action.NONE, false);
        }
        if (s.connecting) {
            return page(s, page, Action.CANCEL_CONNECT, Action.NONE, false);
        }
        boolean needsManual = s.manualPortMode || s.searchTimedOut || s.searchFailed;
        if (page == Page.PAIR_CODE) {
            Action primary = s.pairingPort > 0 || needsManual ? Action.SUBMIT_PAIR
                                                              : Action.OPEN_WIRELESS_SETTINGS;
            Action secondary = needsManual ? Action.RETRY_SEARCH : Action.NONE;
            if (!needsManual && s.pairingPort <= 0 && s.connectionPort <= 0)
                secondary = Action.RETRY_SEARCH;
            return new Result(Page.PAIR_CODE, stepOf(s, Page.PAIR_CODE), stepCount(s), primary,
                    secondary, needsManual, true, s.provisionIncomplete, false, false);
        }
        // ENABLE_WIRELESS
        Action primary = Action.OPEN_WIRELESS_SETTINGS;
        Action secondary = Action.NONE;
        if (s.provisionIncomplete)
            secondary = Action.VIEW_RECOVERY;
        else if (needsManual) {
            primary = s.pairedCredentials && !s.authInvalid ? Action.START_SERVICE
                                                            : Action.ENTER_MANUAL_PORTS;
            secondary = Action.RETRY_SEARCH;
        } else if (s.pairedCredentials && s.connectionPort > 0 && !s.authInvalid)
            primary = Action.START_SERVICE;
        else if (s.shizukuRunning)
            secondary = Action.USE_SHIZUKU;
        else if (s.searchTimedOut || s.searchFailed) {
            secondary = Action.ENTER_MANUAL_PORTS;
            primary = Action.RETRY_SEARCH;
        }
        return new Result(Page.ENABLE_WIRELESS, stepOf(s, Page.ENABLE_WIRELESS), stepCount(s),
                primary, secondary, needsManual, false, s.provisionIncomplete, false, false);
    }

    private static Page connectionPage(Snapshot s) {
        if (s.authInvalid || (!s.pairedCredentials && !s.serviceReady)) {
            // First pair, or credentials rejected: pair page once wireless is reachable or manual.
            if (s.pairingPort > 0 || s.manualPortMode || s.searchTimedOut || s.searchFailed
                    || s.connectionPort > 0)
                return Page.PAIR_CODE;
        }
        if (s.pairedCredentials && !s.authInvalid)
            return Page.ENABLE_WIRELESS;
        if (s.connectionPort > 0 || s.pairingPort > 0 || s.manualPortMode || s.searchTimedOut)
            return Page.PAIR_CODE;
        return Page.ENABLE_WIRELESS;
    }

    private static Result reconnect(Snapshot s) {
        if (s.connecting)
            return page(s, Page.RECONNECT, Action.CANCEL_CONNECT, Action.NONE, false);
        Action primary = s.pairedCredentials && !s.authInvalid ? Action.RECONNECT
                                                               : Action.OPEN_WIRELESS_SETTINGS;
        Action secondary = s.shizukuRunning ? Action.USE_SHIZUKU : Action.RETRY_SEARCH;
        boolean manual = s.manualPortMode || s.searchTimedOut || s.searchFailed;
        boolean needsPair = !s.pairedCredentials || s.authInvalid;
        if (manual) {
            primary = needsPair ? Action.SUBMIT_PAIR : Action.START_SERVICE;
            secondary = Action.RETRY_SEARCH;
        }
        if (s.authInvalid) {
            primary = Action.SUBMIT_PAIR;
            secondary = Action.RETRY_SEARCH;
        }
        return new Result(Page.RECONNECT, 1, 1, primary, secondary, manual || s.authInvalid,
                needsPair && (manual || s.authInvalid), s.provisionIncomplete, false, false);
    }

    private static Result ime(Snapshot s) {
        return page(s, Page.PICK_IME, s.hasKeyboard ? Action.CHOOSE_IME : Action.ENABLE_OTHER_IME,
                s.hasKeyboard ? Action.ENABLE_OTHER_IME : Action.NONE, false);
    }

    private static Result lower(Snapshot s) {
        return page(s, Page.LOWER_INPUT, Action.ENABLE_LOWER_INPUT, Action.SKIP_LOWER_INPUT, false);
    }

    private static Result done(Snapshot s) {
        return new Result(Page.DONE, stepOf(s, Page.DONE), stepCount(s), Action.OPEN_TOUCHPAD,
                Action.NONE, false, false, false, true, false);
    }

    private static Result page(
            Snapshot s, Page page, Action primary, Action secondary, boolean manual) {
        return new Result(page, stepOf(s, page), stepCount(s), primary, secondary, manual, false,
                false, false, s.connecting);
    }

    static int stepCount(Snapshot s) {
        if (s.wirelessOnly)
            return 1;
        if (s.device == DeviceKind.SINGLE)
            return 3;
        // Paired and authorized: skip pair page → 4 steps; otherwise 5.
        if (s.pairedCredentials && !s.authInvalid)
            return 4;
        return 5;
    }

    static int stepOf(Snapshot s, Page page) {
        if (s.wirelessOnly)
            return 1;
        if (s.device == DeviceKind.SINGLE)
            return page == Page.DONE ? 3 : page == Page.PICK_REMOTE ? 2 : 1;
        boolean skipPair = s.pairedCredentials && !s.authInvalid;
        switch (page) {
            case ENABLE_WIRELESS:
            case RECONNECT:
                return 1;
            case PAIR_CODE:
                return 2;
            case PICK_IME:
                return skipPair ? 2 : 3;
            case LOWER_INPUT:
                return skipPair ? 3 : 4;
            case DONE:
                return skipPair ? 4 : 5;
            default:
                return 1;
        }
    }

    /** Whether discovery should auto-advance from enable-wireless after a port appears. */
    static boolean shouldAdvanceFromDiscovery(Snapshot s) {
        if (s.serviceReady || s.connecting || s.lateResult || s.androidBelow11)
            return false;
        if (s.pairedCredentials && !s.authInvalid)
            return s.connectionPort > 0;
        return s.pairingPort > 0 || s.connectionPort > 0;
    }
}
