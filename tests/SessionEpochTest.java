// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT
package io.github.thorpanel;

public final class SessionEpochTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}

    public static void main(String[] args){
        SessionEpoch epoch=new SessionEpoch();
        check(epoch.acceptsInput(),"starts active");
        long move=epoch.nextEvent();
        check(move==1&&epoch.acceptEvent(0,move),"first move accepted");
        check(!epoch.acceptEvent(0,move),"duplicate event rejected");
        int next=epoch.beginSwitch();
        check(next==1&&!epoch.acceptsInput(),"switch pauses input");
        check(epoch.nextEvent()<0,"paused epoch rejects new events");
        check(!epoch.acceptEvent(0,move),"old generation rejected after switch");
        check(!epoch.confirmRelease(next),"release acknowledgment requires a started release");
        check(!epoch.resume(next),"no resume before release acknowledgment");
        epoch.beginRelease();
        check(!epoch.confirmRelease(next-1),"old release cannot confirm new generation");
        check(!epoch.resume(next),"stale release does not unblock input");
        check(epoch.confirmRelease(next),"current device release acknowledgment accepted");
        check(epoch.resume(next)&&epoch.acceptsInput(),"resume after acknowledged release");
        check(!epoch.resume(next),"release acknowledgment is consumed once");
        check(!epoch.acceptEvent(0,move),"stale generation cannot enter new mode");

        int failed=epoch.beginSwitch();
        epoch.beginRelease();
        try{epoch.beginSwitch();throw new AssertionError("nested transition must fail");}
        catch(IllegalStateException expected){check(epoch.generation()==failed,"nested transition cannot replace outer generation");}
        check(epoch.state()==SessionEpoch.State.RELEASING,"nested transition leaves outer release state intact");
        epoch.fail();
        check(!epoch.confirmRelease(failed)&&!epoch.resume(failed),"failure rejects both acknowledgment and resume");
        int retry=epoch.beginSwitch();
        epoch.beginRelease();
        check(!epoch.confirmRelease(failed),"late prior release cannot acknowledge retry");
        check(epoch.confirmRelease(retry)&&epoch.resume(retry),"new release acknowledgment permits retry");
        check(epoch.acceptsInput()&&epoch.generation()==failed+1,"retry opens a fresh generation");
        long fresh=epoch.nextEvent();
        check(epoch.acceptEvent(retry,fresh),"new mode accepts its own events");
        System.out.println("Passed "+checks+" SessionEpoch order and release checks");
    }
}
