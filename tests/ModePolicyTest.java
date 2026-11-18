// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT

package io.github.thorpanel;

import java.util.List;

public final class ModePolicyTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static ModePolicy.Host host(String device, ModePolicy.Orientation orientation,
            boolean split, boolean shell, boolean overlay) {
        return new ModePolicy.Host(device, 0, 0, orientation, ModePolicy.Posture.FLAT, split, shell, overlay);
    }

    private static ModePolicy.Host portrait() {
        return host("tablet", ModePolicy.Orientation.PORTRAIT, true, true, true);
    }

    private static ModePolicy.Host landscape() {
        return host("tablet", ModePolicy.Orientation.LANDSCAPE, true, true, true);
    }

    private static ModeTransition transition(long oldOwner) {
        ModeTransition state = new ModeTransition(new ModePolicy.Memory());
        if (oldOwner != 0) state.seedOwner(oldOwner, ModePolicy.Mode.FLOATING, landscape());
        state.observeHost(portrait(), 0);
        return state;
    }

    private static void commit(ModeTransition state, ModeTransition.Attempt attempt, long newOwner, long now) {
        check(state.prepared(attempt.id, newOwner, now), "readback accepts current prepared window");
        check(!state.acceptsInput(newOwner), "prepared window cannot send input");
        check(state.beginHandoff(attempt.id, attempt.previousOwner, now), "handoff accepts current old owner");
        check(!state.acceptsInput(attempt.previousOwner), "handoff pauses old input");
        check(!state.commit(attempt.id, newOwner, now), "no commit before input release acknowledgement");
        check(state.releaseConfirmed(attempt.id, attempt.previousOwner, now), "release acknowledgement matches request and owner");
        check(state.commit(attempt.id, newOwner, now), "released window becomes sole owner");
        check(state.acceptsInput(newOwner) && !state.acceptsInput(attempt.previousOwner), "only new owner can send input");
    }

    private static void defaultPolicies() {
        for (ModePolicy.Orientation orientation : ModePolicy.Orientation.values()) {
            for (ModePolicy.Posture posture : ModePolicy.Posture.values()) {
                for (boolean split : new boolean[]{false,true}) {
                    for (boolean shell : new boolean[]{false,true}) {
                        for (boolean overlay : new boolean[]{false,true}) {
                            ModePolicy.Host host = new ModePolicy.Host("device", 2, 2, orientation, posture, split, shell, overlay);
                            ModePolicy.Memory memory = new ModePolicy.Memory();
                            for (ModePolicy.Preference preference : ModePolicy.Preference.values()) {
                                memory.remember("device", orientation, preference);
                                List<ModePolicy.Mode> attempts = ModePolicy.choose(host, memory).attempts;
                                boolean wantSplit = preference == ModePolicy.Preference.SPLIT
                                        || preference == ModePolicy.Preference.AUTO && orientation == ModePolicy.Orientation.PORTRAIT;
                                int index = 0;
                                if (wantSplit && split) {
                                    check(attempts.get(index++) == (shell ? ModePolicy.Mode.WM_SPLIT : ModePolicy.Mode.ADJACENT_SPLIT),
                                            "select the probed split backend");
                                }
                                if (overlay) check(attempts.get(index++) == ModePolicy.Mode.FLOATING, "overlay fallback requires permission");
                                check(attempts.get(index++) == ModePolicy.Mode.MANUAL_SPLIT, "manual recovery is last");
                                check(index == attempts.size(), "each attempt occurs only once");
                                ModePolicy.Host dual = new ModePolicy.Host("device", 4, 2, orientation, posture, split, shell, overlay);
                                check(ModePolicy.choose(dual, memory).attempts.equals(List.of(ModePolicy.Mode.DUAL)), "dual route overrides single-screen memory");
                            }
                        }
                    }
                }
            }
        }
        ModePolicy.Memory memory = new ModePolicy.Memory();
        memory.remember("tablet", ModePolicy.Orientation.PORTRAIT, ModePolicy.Preference.FLOATING);
        check(ModePolicy.choose(portrait(), memory).attempts.get(0) == ModePolicy.Mode.FLOATING, "manual portrait preference");
        check(memory.get("tablet", ModePolicy.Orientation.LANDSCAPE) == ModePolicy.Preference.AUTO, "orientation has independent memory");
        check(memory.get("phone", ModePolicy.Orientation.PORTRAIT) == ModePolicy.Preference.AUTO, "device has independent memory");
        memory.remember("tablet", ModePolicy.Orientation.PORTRAIT, ModePolicy.Preference.AUTO);
        check(ModePolicy.choose(portrait(), memory).attempts.get(0) == ModePolicy.Mode.WM_SPLIT, "automatic reset restores default");
        check(ModePolicy.orientation(800,1280) == ModePolicy.Orientation.PORTRAIT, "maximum host region supplies orientation");
        check(ModePolicy.orientation(1280,800) == ModePolicy.Orientation.LANDSCAPE, "host landscape");
        check(ModePolicy.orientation(800,800) == ModePolicy.Orientation.PORTRAIT, "square host is stable");
        for (float invalid : new float[]{0,-1,Float.NaN,Float.POSITIVE_INFINITY}) {
            boolean rejected = false;
            try { ModePolicy.orientation(invalid,800); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "invalid host geometry is not interpreted as a new orientation");
        }
        ModePolicy.Plan original = ModePolicy.choose(portrait(), memory);
        for (int height : new int[]{280,640,900}) {
            PanelLayout.create(800,height,1,0,false,false,0,0);
            check(ModePolicy.choose(portrait(),memory).attempts.equals(original.attempts), "panel ratio and IME resizing do not select a new form");
        }
    }

    private static void debounceAndOwnership() {
        ModeTransition state = transition(10);
        check(state.poll(499) == null, "host waits 500ms");
        state.observeHost(portrait(), 499);
        ModeTransition.Attempt first = state.poll(500);
        check(first != null && first.mode == ModePolicy.Mode.WM_SPLIT, "identical host observations do not restart debounce");
        check(state.poll(501) == null && state.active() == first, "only one concurrent request");
        check(state.acceptsInput(10), "old owner can operate while new window is prepared");
        check(!state.beginHandoff(first.id,10,501), "unprepared windows cannot pause and take over");
        check(state.prepared(first.id,20,510), "candidate is ready");
        check(state.acceptsInput(10) && !state.acceptsInput(20), "readback does not grant candidate input");
        check(!state.beginHandoff(first.id,999,511), "wrong old owner cannot begin handoff");
        check(state.beginHandoff(first.id,10,512), "handoff pauses old owner");
        check(!state.acceptsInput(10) && !state.acceptsInput(20), "neither window sends input during release");
        check(!state.releaseConfirmed(first.id,999,513), "wrong owner release rejected");
        check(!state.releaseConfirmed(first.id+1,10,513), "wrong request release rejected");
        check(!state.commit(first.id,20,513), "unreleased candidate remains blocked");
        check(state.releaseConfirmed(first.id,10,513), "old input release acknowledged");
        check(!state.commit(first.id,21,513), "acknowledgement cannot authorize a different candidate");
        check(state.commit(first.id,20,513), "candidate becomes owner");
        check(state.acceptsInput(20) && !state.acceptsInput(10), "old owner cannot send input after commit");
        check(!state.windowDestroyed(10,true) && state.acceptsInput(20), "late old-window destruction cannot close the session");
        check(state.currentHost().remoteDisplayId == 0 && state.currentHost().panelDisplayId == 0, "committed route ids are available");
        check(state.poll(10000) == null, "successful form remains until host change or explicit retry");
        check(!state.prepared(first.id,30,10001), "late prepared callback cannot create an owner");
        check(state.needsReconcile(), "late system changes request a readback repair");
        long version = state.reconcileVersion();
        state.failed(first.id);
        check(!state.reconciled(version) && state.needsReconcile(), "older readback cannot clear a newer system change");
        check(state.reconciled(state.reconcileVersion()) && !state.needsReconcile(), "latest readback clears repair request");
    }

    private static void fallbackAndFreeze() {
        ModeTransition state = transition(1);
        ModeTransition.Attempt split = state.poll(500);
        check(state.poll(1999) == null, "split timeout is not early");
        ModeTransition.Attempt floating = state.poll(2000);
        check(floating != null && floating.id != split.id && floating.mode == ModePolicy.Mode.FLOATING, "1500ms split timeout falls back once");
        check(!state.prepared(split.id,3,2001), "timed-out split readback cannot replace overlay target");
        check(!state.failed(split.id) && state.active() == floating, "old failure does not fail current fallback");
        commit(state,floating,2,2010);
        for (int i=0;i<20;i++) check(state.poll(10000+i) == null, "fallback success stays fixed");
        state.retry(11000);
        ModeTransition.Attempt retried = state.poll(11000);
        check(retried != null && retried.mode == ModePolicy.Mode.WM_SPLIT, "explicit retry starts a fresh round");
        check(!state.prepared(retried.id,1,11001), "previous owner tokens cannot be reused");
        check(state.failed(retried.id), "new split fails");
        ModeTransition.Attempt secondFloat = state.poll(11002);
        check(secondFloat.mode == ModePolicy.Mode.FLOATING, "second round has one overlay fallback");
        state.failed(secondFloat.id);
        ModeTransition.Attempt manual = state.poll(11003);
        check(manual.mode == ModePolicy.Mode.MANUAL_SPLIT && state.manualRecoveryNeeded(), "exhausted capabilities expose manual recovery");
        check(!state.prepared(manual.id,4,11004), "manual prompt cannot become an input owner");
        check(state.manualRequired(manual.id), "manual recovery marks round complete");
        check(state.acceptsInput(2), "failed replacements preserve a usable old window");
        for (int i=0;i<20;i++) check(state.poll(20000+i) == null, "manual recovery does not retry automatically");
        state.observeHost(host("tablet",ModePolicy.Orientation.PORTRAIT,true,false,true),21000);
        check(state.poll(21499) == null, "capability change also debounces");
        check(state.poll(21500).mode == ModePolicy.Mode.ADJACENT_SPLIT, "changed shell capability selects adjacent executor");
    }

    private static void releaseFailuresAndDisconnect() {
        ModeTransition state = transition(10);
        ModeTransition.Attempt attempt = state.poll(500);
        state.prepared(attempt.id,20,510);
        state.beginHandoff(attempt.id,10,520);
        check(state.releaseFailed(attempt.id,10), "release failure is accepted for matching owner");
        check(state.stage() == ModeTransition.Stage.RECOVERY && !state.acceptsInput(10), "failed release keeps all input disabled");
        check(state.poll(10000) == null, "no fallback is started before cleanup acknowledgement");
        check(!state.releaseConfirmed(attempt.id,10,10000), "late ordinary release cannot bypass recovery");
        check(!state.cleanupConfirmed(attempt.id,20) && !state.cleanupConfirmed(attempt.id+1,10), "recovery requires request and old owner");
        check(state.cleanupConfirmed(attempt.id,10) && state.acceptsInput(10), "verified cleanup restores usable old owner");
        ModeTransition.Attempt fallback = state.poll(10001);
        check(fallback.mode == ModePolicy.Mode.FLOATING, "cleanup resumes the same round's next fallback");
        state.prepared(fallback.id,30,10002);
        state.beginHandoff(fallback.id,10,10003);
        check(state.poll(11503) == null && state.stage() == ModeTransition.Stage.RECOVERY, "release timeout also blocks fallback and input");
        long recovery = state.serviceDisconnected();
        check(recovery != fallback.id && !state.cleanupConfirmed(fallback.id,10), "disconnect invalidates the earlier recovery acknowledgement");
        check(!state.acceptsInput(10) && !state.acceptsInput(30), "disconnected service has no input owner");
        check(state.cleanupConfirmed(recovery,10), "reconnected service confirms cleanup");
        ModeTransition.Attempt reconnect = state.poll(12000);
        check(reconnect.mode == ModePolicy.Mode.WM_SPLIT, "reconnect re-evaluates requested host form");
        check(!state.releaseConfirmed(fallback.id,10,12000), "prior connection callback cannot acknowledge current handoff");
        commit(state,reconnect,40,12010);
        check(!state.cleanupConfirmed(recovery,10) && state.acceptsInput(40), "duplicate cleanup cannot alter current owner");
    }

    private static void rotationsAndWindowLoss() {
        ModeTransition state = transition(1);
        state.observeHost(landscape(),200);
        state.observeHost(portrait(),400);
        check(state.poll(899) == null, "rotation burst waits for the last state to settle");
        ModeTransition.Attempt first = state.poll(900);
        state.observeHost(landscape(),1000);
        check(state.poll(1500) == null && state.active() == first, "new host state waits for active request");
        commit(state,first,2,1510);
        ModeTransition.Attempt rotated = state.poll(1511);
        check(rotated.mode == ModePolicy.Mode.FLOATING && rotated.host.orientation == ModePolicy.Orientation.LANDSCAPE,
                "settled latest rotation starts after previous commit");
        check(state.prepared(rotated.id,3,1520), "rotated candidate prepares");
        check(state.windowDestroyed(2,false), "programmatic old-window loss clears its input owner");
        check(!state.acceptsInput(2) && state.beginHandoff(rotated.id,2,1521), "lost old window still requires explicit old-input cleanup");
        check(state.releaseConfirmed(rotated.id,2,1522) && state.commit(rotated.id,3,1522), "new window can replace a lost old window after release");
        check(state.windowDestroyed(3,true) && state.stage() == ModeTransition.Stage.CLOSED, "user close ends session");
        state.retry(2000);
        state.observeHost(portrait(),2000);
        check(state.poll(5000) == null && !state.acceptsInput(3), "closed session cannot revive from callbacks");

        state = transition(1);
        first = state.poll(500);
        state.prepared(first.id,2,501);
        check(state.windowDestroyed(2,false) && state.acceptsInput(1), "new-window creation failure retains old input");
        ModeTransition.Attempt next = state.poll(502);
        check(next.mode == ModePolicy.Mode.FLOATING, "new-window failure advances to fallback");
        state.prepared(next.id,3,503);
        state.beginHandoff(next.id,1,504);
        check(state.windowDestroyed(3,false) && state.stage() == ModeTransition.Stage.RECOVERY,
                "candidate loss during release cannot re-enable old input without cleanup");
    }

    private static void manualChoiceDuringConversion() {
        ModeTransition state = transition(1);
        ModeTransition.Attempt first = state.poll(500);
        state.select(ModePolicy.Preference.FLOATING,600);
        check(state.poll(1100) == null && state.active() == first, "manual choice does not start concurrent conversion");
        commit(state,first,2,1110);
        ModeTransition.Attempt manual = state.poll(1111);
        check(manual.mode == ModePolicy.Mode.FLOATING, "manual choice is applied after the fixed active target completes");
        commit(state,manual,3,1120);
        state.observeHost(landscape(),1200);
        ModeTransition.Attempt landscape = state.poll(1700);
        commit(state,landscape,4,1710);
        state.observeHost(portrait(),1800);
        check(state.poll(2300).mode == ModePolicy.Mode.FLOATING, "portrait remembers its selected form across rotations");

        state = transition(0);
        first = state.poll(500);
        commit(state,first,1,510);
        check(!state.acceptsInput(0), "zero is never an input owner");
    }

    private static void lateReleaseAndOwnerLoss() {
        ModeTransition state = transition(1);
        ModeTransition.Attempt first = state.poll(500);
        state.prepared(first.id,2,510);
        state.beginHandoff(first.id,1,520);
        check(!state.releaseConfirmed(first.id,1,2020), "release callback checks deadline even before poll runs");
        check(state.stage() == ModeTransition.Stage.RECOVERY && !state.acceptsInput(1)
                && !state.commit(first.id,2,2021), "late release cannot grant either window input");
        state.cleanupConfirmed(first.id,1);
        ModeTransition.Attempt next = state.poll(2030);
        state.prepared(next.id,3,2040);
        state.beginHandoff(next.id,1,2050);
        check(state.releaseConfirmed(next.id,1,2051), "timely release accepted");
        check(!state.commit(next.id,3,3550) && state.acceptsInput(1), "late commit restores the old window only after verified release");

        state = transition(1);
        first = state.poll(500);
        state.prepared(first.id,2,510);
        state.windowDestroyed(1,false);
        long token = state.serviceDisconnected();
        check(state.recoveryOwnerId() == 1 && !state.cleanupConfirmed(token,0), "disconnect preserves the destroyed input owner's identity");
        check(state.cleanupConfirmed(token,1), "destroyed owner still receives explicit cleanup acknowledgement");
        next = state.poll(1000);
        check(next.previousOwner == 0, "verified cleanup clears the absent input owner");
        commit(state,next,3,1010);
        check(state.windowDestroyed(3,false) && state.stage() == ModeTransition.Stage.RECOVERY,
                "unexpected committed-window loss exposes recovery");
        long lost = state.recoveryRequestId();
        check(state.recoveryOwnerId() == 3 && state.poll(5000) == null, "window loss cannot retry before input cleanup");
        check(state.cleanupConfirmed(lost,3), "lost-window cleanup accepts its original owner");
        check(state.poll(5001) != null, "same-host freeze does not block replacing a lost window");

        state = transition(1);
        first = state.poll(500);
        state.prepared(first.id,2,510);
        state.beginHandoff(first.id,1,520);
        state.releaseFailed(first.id,1);
        state.windowDestroyed(1,false);
        token = state.serviceDisconnected();
        check(state.recoveryOwnerId() == 1 && state.cleanupConfirmed(token,1), "a second disconnect keeps the pending release owner's identity");

        state = transition(1);
        first = state.poll(500);
        state.prepared(first.id,2,510);
        check(state.windowDestroyed(2,true) && state.stage() == ModeTransition.Stage.CLOSED, "user close of a visible candidate ends the session");
        long version = state.reconcileVersion();
        check(!state.prepared(first.id,3,600) && !state.failed(first.id), "closed session refuses late window callbacks");
        check(!state.needsReconcile() && !state.manualRecoveryNeeded() && state.recoveryRequestId() == 0
                && state.reconcileVersion() == version, "closed session stays silent after stale results");
    }

    private static void requestIdsAcrossSessions() {
        boolean rejected = false;
        try { new ModeTransition(new ModePolicy.Memory(), -1); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "negative request seed is rejected");

        ModeTransition first = new ModeTransition(new ModePolicy.Memory(), 500);
        check(first.lastRequestId() == 500, "seed is visible before the first request");
        first.observeHost(portrait(), 0);
        ModeTransition.Attempt split = first.poll(500);
        check(split.id == 501 && first.lastRequestId() == split.id, "first request advances persisted seed");
        check(first.poll(501) == null && first.lastRequestId() == 501, "polling a pending request does not allocate an id");
        first.failed(split.id);
        ModeTransition.Attempt floating = first.poll(502);
        check(floating.id == 502 && first.lastRequestId() == 502, "fallback advances the same sequence");
        commit(first, floating, 30, 510);
        long disconnect = first.serviceDisconnected();
        check(disconnect == 503 && first.lastRequestId() == disconnect, "disconnect recovery participates in request sequence");
        check(first.cleanupConfirmed(disconnect, 30), "disconnect cleanup acknowledges its allocated request");
        ModeTransition.Attempt reconnected = first.poll(520);
        check(reconnected.id == 504, "reconnect cannot reuse a recovery token");
        commit(first, reconnected, 40, 530);
        check(first.windowDestroyed(40, false), "unexpected committed window loss starts recovery");
        long windowRecovery = first.recoveryRequestId();
        check(windowRecovery == 505 && first.lastRequestId() == windowRecovery,
                "window-loss recovery is included in the persisted high watermark");
        first.close();
        check(first.lastRequestId() == windowRecovery && first.serviceDisconnected() == 0,
                "closed session keeps its request watermark without allocating another token");

        ModeTransition second = new ModeTransition(new ModePolicy.Memory(), first.lastRequestId());
        second.seedOwner(40, ModePolicy.Mode.FLOATING, landscape());
        second.observeHost(portrait(), 0);
        ModeTransition.Attempt current = second.poll(500);
        check(current.id == 506 && current.id > windowRecovery, "replacement session starts above every old request kind");
        for (long stale : new long[]{split.id, floating.id, disconnect, reconnected.id, windowRecovery}) {
            check(!second.prepared(stale, 50, 501), "prior-session prepared callback is rejected");
            check(!second.beginHandoff(stale, 40, 501), "prior-session handoff cannot pause the current owner");
            check(!second.releaseConfirmed(stale, 40, 501) && !second.releaseFailed(stale, 40),
                    "prior-session release callback cannot acknowledge or fail current release");
            check(!second.cleanupConfirmed(stale, 40) && !second.commit(stale, 50, 501),
                    "prior-session cleanup and commit cannot grant new ownership");
            check(!second.failed(stale) && second.active() == current && second.acceptsInput(40),
                    "prior-session failure leaves current request and owner intact");
        }
        check(second.prepared(current.id, 50, 510) && second.beginHandoff(current.id, 40, 511),
                "current session still prepares and begins its own handoff");
        check(!second.releaseConfirmed(reconnected.id, 40, 512) && !second.commit(current.id, 50, 512),
                "matching reused owner identity cannot make an old release acknowledge the new request");
        check(second.releaseConfirmed(current.id, 40, 513) && second.commit(current.id, 50, 513),
                "only the new request can authorize its new window");
        check(second.lastRequestId() == 506 && second.acceptsInput(50), "stale callbacks never advance request sequence");
    }

    public static void main(String[] args) {
        defaultPolicies();
        debounceAndOwnership();
        fallbackAndFreeze();
        releaseFailuresAndDisconnect();
        rotationsAndWindowLoss();
        manualChoiceDuringConversion();
        lateReleaseAndOwnerLoss();
        requestIdsAcrossSessions();
        System.out.println("Passed " + checks + " mode policy and handoff checks");
    }
}
