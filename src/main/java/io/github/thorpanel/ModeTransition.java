// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT

package io.github.thorpanel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Serial window preparation and input handoff. The platform adapter performs all side effects. */
final class ModeTransition {
    static final long HOST_STABLE_MS = 500;
    static final long ATTEMPT_TIMEOUT_MS = 1500;
    enum Stage { IDLE, REQUESTED, PREPARED, RELEASING, RECOVERY, CLOSED }

    static final class Attempt {
        final long id;
        final ModePolicy.Host host;
        final ModePolicy.Mode mode;
        final long previousOwner;
        final long deadlineMs;

        private Attempt(
                long id, ModePolicy.Host host, ModePolicy.Mode mode, long previousOwner, long now) {
            this.id = id;
            this.host = host;
            this.mode = mode;
            this.previousOwner = previousOwner;
            deadlineMs = deadline(now, ATTEMPT_TIMEOUT_MS);
        }
    }

    private final ModePolicy.Memory memory;
    private final Set<Long> usedOwners = new HashSet<>();
    private ModePolicy.Host observedHost, finishedHost, ownerHost;
    private ModePolicy.Mode ownerMode;
    private ModePolicy.Plan plan;
    private int planIndex;
    private long observedSince, nextRequestId, owner, inputOwner, candidate, stageDeadline;
    private long recoveryRequest, recoveryOwner, reconcileVersion;
    private boolean inputEnabled, released, retryRequested, reconcile, manualRecovery;
    private Attempt active;
    private Stage stage = Stage.IDLE;

    ModeTransition(ModePolicy.Memory memory) {
        this(memory, 0);
    }
    ModeTransition(ModePolicy.Memory memory, long lastRequestId) {
        this.memory = Objects.requireNonNull(memory);
        if (lastRequestId < 0)
            throw new IllegalArgumentException("Invalid request seed");
        nextRequestId = lastRequestId;
    }

    synchronized void seedOwner(long owner, ModePolicy.Mode mode, ModePolicy.Host host) {
        if (stage != Stage.IDLE || this.owner != 0 || !usedOwners.isEmpty() || owner <= 0) {
            throw new IllegalStateException("An initial owner can only be supplied once");
        }
        this.owner = owner;
        inputOwner = owner;
        ownerMode = Objects.requireNonNull(mode);
        ownerHost = Objects.requireNonNull(host);
        usedOwners.add(owner);
        inputEnabled = true;
    }

    synchronized void observeHost(ModePolicy.Host host, long now) {
        if (stage == Stage.CLOSED)
            return;
        Objects.requireNonNull(host);
        if (host.equals(observedHost))
            return;
        observedHost = host;
        observedSince = now;
    }

    synchronized void select(ModePolicy.Preference preference, long now) {
        if (stage == Stage.CLOSED || observedHost == null)
            return;
        memory.remember(observedHost.deviceKey, observedHost.orientation, preference);
        retryRequested = true;
        observedSince = now;
    }

    synchronized void retry(long now) {
        if (stage == Stage.CLOSED)
            return;
        retryRequested = true;
        observedSince = now - HOST_STABLE_MS;
    }

    /** Return each new request once. Repeated polling never creates a second concurrent request. */
    synchronized Attempt poll(long now) {
        if (stage == Stage.CLOSED || stage == Stage.RECOVERY)
            return null;
        if (active != null) {
            if (now < stageDeadline)
                return null;
            failCurrent();
            if (stage == Stage.RECOVERY)
                return null;
        }
        if (plan != null && planIndex >= plan.attempts.size()) {
            finishedHost = plan.host;
            plan = null;
            manualRecovery = true;
        }
        if (plan == null) {
            if (observedHost == null || elapsed(now, observedSince) < HOST_STABLE_MS
                    || (!retryRequested && observedHost.equals(finishedHost)))
                return null;
            plan = ModePolicy.choose(observedHost, memory);
            planIndex = 0;
            retryRequested = false;
            manualRecovery = false;
        }
        ModePolicy.Mode mode = plan.attempts.get(planIndex++);
        active = new Attempt(++nextRequestId, plan.host, mode, inputOwner, now);
        candidate = 0;
        released = false;
        stage = Stage.REQUESTED;
        stageDeadline = active.deadlineMs;
        if (mode == ModePolicy.Mode.MANUAL_SPLIT)
            manualRecovery = true;
        return active;
    }

    /** Call only after the new window and its requested placement both pass readback. */
    synchronized boolean prepared(long requestId, long newOwner, long now) {
        if (stage == Stage.CLOSED)
            return false;
        if (!matches(requestId) || stage != Stage.REQUESTED || now >= stageDeadline) {
            markReconcile();
            return false;
        }
        if (newOwner <= 0 || usedOwners.contains(newOwner)
                || active.mode == ModePolicy.Mode.MANUAL_SPLIT)
            return false;
        usedOwners.add(newOwner);
        candidate = newOwner;
        stage = Stage.PREPARED;
        return true;
    }

    synchronized boolean beginHandoff(long requestId, long expectedOldOwner, long now) {
        if (!matches(requestId) || stage != Stage.PREPARED
                || expectedOldOwner != active.previousOwner
                || (owner != 0 && owner != expectedOldOwner) || now >= stageDeadline)
            return false;
        inputEnabled = false;
        stage = Stage.RELEASING;
        stageDeadline = deadline(now, ATTEMPT_TIMEOUT_MS);
        return true;
    }

    /** The adapter must obtain this acknowledgement from the input service, not from window disposal. */
    synchronized boolean releaseConfirmed(long requestId, long expectedOldOwner, long now) {
        if (!matches(requestId) || stage != Stage.RELEASING
                || expectedOldOwner != active.previousOwner)
            return false;
        if (now >= stageDeadline) {
            failCurrent();
            return false;
        }
        released = true;
        inputOwner = 0;
        return true;
    }

    synchronized boolean commit(long requestId, long newOwner, long now) {
        if (!matches(requestId) || stage != Stage.RELEASING || !released || newOwner != candidate)
            return false;
        if (now >= stageDeadline) {
            failCurrent();
            return false;
        }
        owner = candidate;
        inputOwner = candidate;
        ownerMode = active.mode;
        ownerHost = active.host;
        finishedHost = active.host;
        inputEnabled = true;
        candidate = 0;
        active = null;
        plan = null;
        released = false;
        manualRecovery = false;
        stage = Stage.IDLE;
        return true;
    }

    synchronized boolean failed(long requestId) {
        if (stage == Stage.CLOSED)
            return false;
        if (!matches(requestId)) {
            markReconcile();
            return false;
        }
        failCurrent();
        return true;
    }

    synchronized boolean releaseFailed(long requestId, long expectedOldOwner) {
        if (!matches(requestId) || stage != Stage.RELEASING
                || expectedOldOwner != active.previousOwner)
            return false;
        released = false;
        failCurrent();
        return true;
    }

    /** Finish a manual-recovery notification without granting input to a new window. */
    synchronized boolean manualRequired(long requestId) {
        if (!matches(requestId) || active.mode != ModePolicy.Mode.MANUAL_SPLIT)
            return false;
        finishedHost = active.host;
        active = null;
        plan = null;
        stage = Stage.IDLE;
        manualRecovery = true;
        return true;
    }

    private void failCurrent() {
        boolean uncertainRelease =
                (stage == Stage.RELEASING && !released) || (owner == 0 && inputOwner != 0);
        if (uncertainRelease) {
            recoveryRequest = active.id;
            recoveryOwner = active.previousOwner;
            inputEnabled = false;
            stage = Stage.RECOVERY;
        } else {
            inputEnabled = owner != 0;
            inputOwner = owner;
            stage = Stage.IDLE;
        }
        candidate = 0;
        active = null;
        released = false;
        markReconcile();
    }

    synchronized boolean cleanupConfirmed(long requestId, long expectedOldOwner) {
        if (stage != Stage.RECOVERY || recoveryRequest != requestId
                || recoveryOwner != expectedOldOwner)
            return false;
        recoveryRequest = 0;
        recoveryOwner = 0;
        inputEnabled = owner != 0;
        inputOwner = owner;
        stage = Stage.IDLE;
        return true;
    }

    synchronized long serviceDisconnected() {
        if (stage == Stage.CLOSED)
            return 0;
        recoveryRequest = ++nextRequestId;
        recoveryOwner = stage == Stage.RECOVERY ? recoveryOwner
                : active != null                ? active.previousOwner
                                                : inputOwner;
        inputEnabled = false;
        active = null;
        candidate = 0;
        plan = null;
        finishedHost = null;
        released = false;
        markReconcile();
        stage = Stage.RECOVERY;
        return recoveryRequest;
    }

    /** Old windows cannot close or disable an owner committed after them. */
    synchronized boolean windowDestroyed(long windowOwner, boolean userClosed) {
        if (windowOwner <= 0 || stage == Stage.CLOSED)
            return false;
        if (windowOwner == candidate) {
            if (userClosed)
                close();
            else
                failCurrent();
            return true;
        }
        if (windowOwner != owner)
            return false;
        owner = 0;
        inputEnabled = false;
        if (userClosed) {
            close();
        } else if (active == null || active.mode == ModePolicy.Mode.MANUAL_SPLIT) {
            if (stage != Stage.RECOVERY) {
                recoveryRequest = ++nextRequestId;
                recoveryOwner = inputOwner;
            }
            active = null;
            candidate = 0;
            plan = null;
            finishedHost = null;
            stage = Stage.RECOVERY;
            markReconcile();
        }
        return true;
    }

    synchronized void close() {
        owner = 0;
        inputOwner = 0;
        candidate = 0;
        active = null;
        plan = null;
        inputEnabled = false;
        released = false;
        reconcile = false;
        reconcileVersion++;
        manualRecovery = false;
        recoveryRequest = 0;
        recoveryOwner = 0;
        stage = Stage.CLOSED;
    }

    private boolean matches(long requestId) {
        return active != null && active.id == requestId;
    }
    private void markReconcile() {
        reconcile = true;
        reconcileVersion++;
    }
    private static long elapsed(long now, long since) {
        return now >= since ? now - since : 0;
    }
    private static long deadline(long now, long duration) {
        return now > Long.MAX_VALUE - duration ? Long.MAX_VALUE : now + duration;
    }

    synchronized Stage stage() {
        return stage;
    }
    synchronized long lastRequestId() {
        return nextRequestId;
    }
    synchronized Attempt active() {
        return active;
    }
    synchronized long owner() {
        return owner;
    }
    synchronized ModePolicy.Mode currentMode() {
        return ownerMode;
    }
    synchronized ModePolicy.Host currentHost() {
        return ownerHost;
    }
    synchronized boolean acceptsInput(long windowOwner) {
        return inputEnabled && owner != 0 && owner == windowOwner;
    }
    synchronized long recoveryRequestId() {
        return recoveryRequest;
    }
    synchronized long recoveryOwnerId() {
        return recoveryOwner;
    }
    synchronized boolean manualRecoveryNeeded() {
        return manualRecovery;
    }
    synchronized boolean needsReconcile() {
        return reconcile;
    }
    synchronized long reconcileVersion() {
        return reconcileVersion;
    }
    synchronized boolean reconciled(long version) {
        if (stage == Stage.CLOSED || version != reconcileVersion)
            return false;
        reconcile = false;
        return true;
    }
}
