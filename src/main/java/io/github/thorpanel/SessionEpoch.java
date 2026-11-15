// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/**
 * Shared input/control epoch for ordered mode and target switches.
 * Pause new input, discard or finish old events, confirm release, then resume.
 */
final class SessionEpoch {
    enum State {ACTIVE, PAUSED, RELEASING, FAILED}

    private int generation;
    private long sequence;
    private long acceptedSequence;
    private State state=State.ACTIVE;
    private boolean releaseConfirmed;
    private int heldKeys;
    private int heldButtons;

    synchronized int generation(){return generation;}
    synchronized long sequence(){return sequence;}
    synchronized State state(){return state;}
    synchronized boolean acceptsInput(){return state==State.ACTIVE;}

    /** Stamp a new input event when the session is active. */
    synchronized long nextEvent(){
        if(state!=State.ACTIVE)return -1;
        return ++sequence;
    }

    synchronized boolean acceptEvent(int generation,long sequence){
        if(state!=State.ACTIVE||this.generation!=generation||sequence<=acceptedSequence)return false;
        acceptedSequence=sequence;
        this.sequence=Math.max(this.sequence,sequence);
        return true;
    }

    synchronized boolean acceptGeneration(int generation){
        return this.generation==generation&&state!=State.FAILED;
    }

    /** Begin a mode or target switch: pause input and require release confirmation. */
    synchronized int beginSwitch(){
        state=State.PAUSED;
        releaseConfirmed=false;
        generation++;
        return generation;
    }

    synchronized void noteKey(boolean down){
        if(down)heldKeys++;
        else if(heldKeys>0)heldKeys--;
    }

    synchronized void noteButton(boolean down){
        if(down)heldButtons++;
        else if(heldButtons>0)heldButtons--;
    }

    synchronized void beginRelease(){
        if(state==State.PAUSED||state==State.RELEASING)state=State.RELEASING;
    }

    /** Confirm all keys and buttons are up after a release attempt. */
    synchronized boolean confirmRelease(){
        if(state!=State.RELEASING&&state!=State.PAUSED)return false;
        if(heldKeys!=0||heldButtons!=0)return false;
        releaseConfirmed=true;
        return true;
    }

    synchronized boolean resume(int generation){
        if(this.generation!=generation||!releaseConfirmed||heldKeys!=0||heldButtons!=0)return false;
        if(state!=State.PAUSED&&state!=State.RELEASING)return false;
        state=State.ACTIVE;
        return true;
    }

    synchronized void fail(){
        state=State.FAILED;
        releaseConfirmed=false;
    }

    synchronized int heldKeys(){return heldKeys;}
    synchronized int heldButtons(){return heldButtons;}
}
