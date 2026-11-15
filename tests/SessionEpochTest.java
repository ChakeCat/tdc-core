// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

public final class SessionEpochTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}

    public static void main(String[] args){
        SessionEpoch epoch=new SessionEpoch();
        check(epoch.acceptsInput(),"starts active");
        long move=epoch.nextEvent();
        check(move==1&&epoch.acceptEvent(0,move),"first move accepted");

        int next=epoch.beginSwitch();
        check(next==1&&!epoch.acceptsInput(),"switch pauses input");
        check(epoch.nextEvent()<0,"paused epoch rejects new events");
        check(!epoch.acceptEvent(0,move),"old generation events rejected after switch");

        epoch.noteKey(true);epoch.noteButton(true);
        epoch.beginRelease();
        check(!epoch.confirmRelease(),"held keys block release confirmation");
        epoch.noteKey(false);epoch.noteButton(false);
        check(epoch.confirmRelease(),"clear release confirms");
        check(epoch.resume(next)&&epoch.acceptsInput(),"resume after confirmed release");

        long late=move;
        check(!epoch.acceptEvent(0,late),"stale generation cannot enter new mode");

        int failed=epoch.beginSwitch();
        epoch.beginRelease();
        epoch.noteKey(true);
        check(!epoch.confirmRelease(),"leftover key keeps releasing");
        check(!epoch.resume(failed),"cannot resume with leftover key");
        epoch.fail();
        check(epoch.state()==SessionEpoch.State.FAILED,"failure sticks until reconnect");
        check(!epoch.resume(failed),"failed epoch cannot resume");
        int retry=epoch.beginSwitch();
        epoch.beginRelease();
        check(!epoch.confirmRelease(),"retry still requires releasing pending keys");
        epoch.noteKey(false);
        check(epoch.confirmRelease()&&epoch.resume(retry),"successful release permits a retry");
        check(epoch.acceptsInput()&&epoch.generation()==failed+1,"retry opens a fresh generation");

        // Late press after mode switch must not land in the new mode.
        int g=epoch.beginSwitch();
        long stalePress=2;
        check(!epoch.acceptEvent(g-1,stalePress),"late press from old generation rejected");
        epoch.beginRelease();
        check(epoch.confirmRelease(),"clear for late-press scenario");
        check(epoch.resume(g),"resume after late-press discard");
        long fresh=epoch.nextEvent();
        check(epoch.acceptEvent(g,fresh),"new mode accepts only its own events");

        System.out.println("Passed "+checks+" SessionEpoch order and release checks");
    }
}
