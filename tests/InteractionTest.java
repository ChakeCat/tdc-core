// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** State-transition regressions: no Android/device or remote computer required. */
public final class InteractionTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void equal(List<String> actual,String... expected){check(actual.equals(Arrays.asList(expected)),actual+" != "+Arrays.toString(expected));}
    private static final class Mouse implements TouchpadGesture.Output {
        final List<String> buttons=new ArrayList<>();float dx,dy,wheel,horizontal;boolean dragging;
        final TouchpadGesture pad=new TouchpadGesture(10,this);
        public void move(float x,float y){dx+=x;dy+=y;}
        public void scroll(float x,float y){horizontal+=x;wheel+=y;}
        public void button(int b,boolean down){buttons.add(b+(down?"D":"U"));}
        public void dragging(boolean value){dragging=value;}
    }
    private static void padTests(){
        check(TouchpadGesture.clickButton(true,590,430,602,438)==2,"bottom-right corner is right click");
        check(TouchpadGesture.clickButton(true,590,300,602,438)==1,"upper right remains left click");
        check(TouchpadGesture.clickButton(true,450,430,602,438)==1,"bottom outside corner remains left click");
        check(TouchpadGesture.clickButton(false,590,430,602,438)==1,"corner option disabled gives left click");
        check(TouchpadGesture.clickButton(true,602,438,602,438)==1,"outside pad boundary is not in right-click zone");
        check(TouchpadGesture.clickButton(true,522,378,602,438)==2,"point on the quarter-circle arc is right click");
        check(TouchpadGesture.clickButton(true,522,377,602,438)==1,"point just outside the arc remains left click");
        check(TouchpadGesture.clickButton(true,510,375,602,438)==1,"old rectangle corner outside the sector remains left click");
        check(TouchpadGesture.clickButton(true,590,350,602,438)==2,"upper portion of the sector matches its visible dots");
        check(TouchpadGesture.clickButton(true,603,430,602,438)==1,"point past the right edge is not right click");
        check(TouchpadGesture.clickButton(true,590,439,602,438)==1,"point past the bottom edge is not right click");
        Mouse m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.up(0,90);equal(m.buttons,"1D","1U");
        m=new Mouse();m.pad.down(0,900,10,0,2);m.pad.up(0,90);equal(m.buttons,"2D","2U");
        m=new Mouse();m.pad.down(0,10,10,0,1);long token=m.pad.token();m.pad.hold(token,99);
        check(m.buttons.isEmpty(),"hold must not start before 100 ms");m.pad.hold(token,100);
        check(m.dragging,"one stationary press starts dragging");
        m.pad.update(0,50,30);m.pad.finishMove();check(m.dx==40&&m.dy==20,"drag moves pointer");
        m.pad.up(0,800);equal(m.buttons,"1D","1U");check(!m.dragging,"finger up releases drag");
        m=new Mouse();m.pad.down(0,900,10,0,2);m.pad.hold(m.pad.token(),100);m.pad.up(0,500);
        equal(m.buttons,"1D","1U"); // A right-zone long hold still performs a normal left drag.
        for(int delay:new int[]{50,100,150,300,450,1000}){
            m=new Mouse();m.pad.setHoldDelayMs(delay);m.pad.down(0,10,10,700,1);
            check(m.pad.holdDelayMs()==delay,"timer uses custom hold delay");
            m.pad.hold(m.pad.token(),700+delay-1);check(m.buttons.isEmpty(),"custom hold cannot start early: "+delay);
            m.pad.hold(m.pad.token(),700+delay);check(m.dragging,"custom hold starts on time: "+delay);
            m.pad.up(0,701+delay);equal(m.buttons,"1D","1U");
        }
        m=new Mouse();m.pad.setHoldDelayMs(400);m.pad.down(0,10,10,0,1);m.pad.setHoldDelayMs(100);
        check(m.pad.holdDelayMs()==400,"changing settings keeps the current touch's timer and threshold together");
        m.pad.hold(m.pad.token(),100);check(!m.dragging,"new shorter delay cannot start the current touch early");
        m.pad.hold(m.pad.token(),400);check(m.dragging,"current touch still starts at its original threshold");m.pad.up(0,410);
        m.pad.down(0,10,10,500,1);check(m.pad.holdDelayMs()==100,"next touch uses updated setting");
        m.pad.hold(m.pad.token(),599);check(!m.dragging,"updated setting keeps its early boundary");
        m.pad.hold(m.pad.token(),600);check(m.dragging,"updated setting activates on next touch");m.pad.up(0,610);equal(m.buttons,"1D","1U","1D","1U");
        m=new Mouse();m.pad.setHoldDelayMs(Integer.MIN_VALUE);m.pad.down(0,10,10,0,1);
        check(m.pad.holdDelayMs()==50,"invalid short delay is limited to minimum");m.pad.cancel();
        m.pad.setHoldDelayMs(Integer.MAX_VALUE);m.pad.down(0,10,10,100,1);
        check(m.pad.holdDelayMs()==1000,"invalid long delay is limited to maximum");m.pad.cancel();
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.update(0,30,10);m.pad.finishMove();m.pad.update(0,10,10);m.pad.finishMove();
        m.pad.hold(m.pad.token(),600);m.pad.up(0,650);check(m.buttons.isEmpty(),"moving before hold cancels drag, even after returning");
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.up(0,60);m.pad.down(0,10,10,120,1);m.pad.up(0,180);
        equal(m.buttons,"1D","1U","1D","1U");
        m=new Mouse();m.pad.down(0,10,10,0,1);token=m.pad.token();m.pad.up(0,60);m.pad.down(0,10,10,200,1);
        m.pad.hold(token,300);check(!m.dragging,"old timer cannot latch a later touch");
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.hold(m.pad.token(),450);m.pad.cancel();m.pad.cancel();equal(m.buttons,"1D","1U");
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.down(7,20,10,40,1);m.pad.hold(m.pad.token(),500);
        check(!m.dragging,"two fingers cannot start drag");m.pad.up(0,90);m.pad.up(7,100);equal(m.buttons,"2D","2U");
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.down(1,20,10,30,1);
        m.pad.update(0,10,55);m.pad.update(1,20,55);m.pad.finishMove();m.pad.up(1,100);m.pad.up(0,120);
        check(m.wheel==-1&&m.buttons.isEmpty(),"two-finger scroll must not click");
        m=new Mouse();m.pad.down(0,100,10,0,1);m.pad.down(1,200,10,30,1);
        m.pad.update(0,190,10);m.pad.update(1,290,10);m.pad.finishMove();m.pad.up(1,100);m.pad.up(0,120);
        check(m.horizontal==2&&m.wheel==0&&m.buttons.isEmpty(),"horizontal swipe scrolls right without vertical scroll or right click");
        m=new Mouse();m.pad.down(0,100,10,0,1);m.pad.down(1,200,10,30,1);
        m.pad.update(0,10,55);m.pad.update(1,110,55);m.pad.finishMove();m.pad.up(1,100);m.pad.up(0,120);
        check(m.horizontal==-2&&m.wheel==-1&&m.buttons.isEmpty(),"diagonal swipe preserves both scroll axes");
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.hold(m.pad.token(),450);m.pad.down(1,20,10,460,1);m.pad.up(0,470);m.pad.up(1,480);
        equal(m.buttons,"1D","1U");check(!m.dragging,"second finger releases drag without a stray right click");
        m=new Mouse();m.pad.down(0,10,10,0,1);m.pad.update(99,500,900);m.pad.finishMove();
        check(m.dx==0&&m.wheel==0,"a finger holding a separate mouse button is not part of the pad gesture");
    }
    private static void modifierTests(){
        for(int code:new int[]{59,113,57,117}){
            List<String> events=new ArrayList<>();ModifierKeys k=new ModifierKeys((c,d,meta)->events.add(c+(d?"D":"U")));
            k.down(code,0);check(k.isActive(code)&&k.meta()!=0,"physical modifier down");k.up(code,80,false);
            check(!k.isActive(code)&&!k.isLocked(code)&&k.meta()==0,"single click is released");
            equal(events,code+"D",code+"U");
            k.down(code,170);k.up(code,220,false);check(k.isLocked(code)&&k.isActive(code),"double tap latches");
            equal(events,code+"D",code+"U",code+"D");
            k.down(code,300);k.up(code,350,false);check(!k.isActive(code)&&!k.isLocked(code),"one click unlocks");
            equal(events,code+"D",code+"U",code+"D",code+"U");
        }
        List<String> events=new ArrayList<>();ModifierKeys k=new ModifierKeys((c,d,meta)->events.add(c+(d?"D":"U")));
        k.down(59,0);k.chord();k.up(59,80,false);k.down(59,140);k.up(59,200,false);
        check(!k.isLocked(59),"a held chord followed by a tap cannot latch");
        k.reset();k.down(59,0);k.up(59,400,false);k.down(59,430);k.up(59,460,false);
        check(!k.isLocked(59),"long press is not first half of a double tap");
        k.reset();k.down(59,0);k.up(59,50,false);k.down(59,500);k.up(59,540,false);
        check(!k.isLocked(59),"slow taps do not latch");
        k.reset();k.down(113,0);k.up(113,50,false);k.down(113,100);k.up(113,150,true);
        check(!k.isLocked(113)&&!k.isActive(113),"cancel/outside release cannot latch");
        k.reset();k.down(113,0);k.up(113,50,false);k.down(113,100);k.up(113,150,false);
        k.down(59,200);k.up(59,250,false);k.down(59,300);k.up(59,350,false);
        check(k.isLocked(113)&&k.isLocked(59),"multiple modifiers can be locked together");
        k.reset();check(k.meta()==0&&!k.isLocked(113)&&!k.isLocked(59),"mode change releases all locks");
        int before=events.size();k.up(59,400,false);k.reset();check(events.size()==before,"late finger up after cancellation cannot relatch");
    }
    private static void imeWindowTests(){
        String composed="Display 123 (physical, \"\")\n   Composition Display State:\n"
            +"   layerFilter={layerStack=4 toInternalDisplay=true }\n"
            +"  - Output Layer 0x123(InputMethod#18065)\n"
            +"        Region visibleRegion (this=0x1, count=1)\n    [0,305,1240,1080]\n";
        check(ImeSurfaceState.visible(composed,4),"composed lower IME is visible");
        check(!ImeSurfaceState.visible(composed,0),"composed upper IME cannot validate lower display");
        check(!ImeSurfaceState.visible(composed.replace("count=1","count=0"),4),"empty visible region is not shown");
        check(!ImeSurfaceState.visible(composed.replace("InputMethod#18065","PanelActivity#18065"),4),"panel surface is not a keyboard");
        check(!ImeSurfaceState.visible(composed.replace("InputMethod#18065","Surface(name=InputMethod) - animation-leash#18065"),4),"animation leash alone is not keyboard content");
        check(!ImeSurfaceState.visible("Offscreen Layers:\nInputMethod#18065\nRegion visibleRegion (count=1)",4),"cached offscreen surface is not shown");
        String emptyLower=composed.replace("InputMethod#18065","PanelActivity#18065");
        check(!ImeSurfaceState.visible(emptyLower+composed.replace("layerStack=4","layerStack=0"),4),"do not combine lower display and upper IME");
        check(!ImeSurfaceState.visible(null,4),"missing compositor state is not success");

        // Reduced fields from the actual YuyanDual window dump: standard IME height is 0.
        String id="com.yuyan.pinyin.online.release/com.yuyan.imemodule.service.ImeService";
        String shown="\n  Window #22 Window{4054ae1 u0 com.yuyan.pinyin.online.release}:\n"
            +"    mDisplayId=4 rootTaskId=670\n    mOwnerUid=10134 package=com.yuyan.pinyin.online.release appop=NONE\n"
            +"    mAttrs={(0,0)(fillxfill) ty=PRESENTATION wanim=0x10302fd}\n"
            +"      Surface: shown=true layer=0 alpha=1.0\n      mDrawState=HAS_DRAWN mLastHidden=false\n    isVisible=true\n";
        check(ImeWindowState.hasLowerPresentation(shown,id),"visible lower Presentation counts as keyboard");
        check(!ImeWindowState.hasLowerPresentation(shown.replace("mDisplayId=4 ","mDisplayId=0 "),id),"upper-screen Presentation is not lower keyboard");
        check(!ImeWindowState.hasLowerPresentation(shown.replace("isVisible=true","isVisible=false"),id),"hidden Presentation does not count");
        check(!ImeWindowState.hasLowerPresentation(shown.replace("Surface: shown=true","Surface: shown=false"),id),"unshown surface does not count");
        check(!ImeWindowState.hasLowerPresentation(shown.replace("ty=PRESENTATION ","ty=INPUT_METHOD "),id),"phantom IME window does not count as Presentation");
        check(!ImeWindowState.hasLowerPresentation(shown,"com.tencent.wetype/.plugin.hld.WxHldService"),"unrelated IME package does not count");
        String hidden=shown.replace("isVisible=true","isVisible=false");
        String other=shown.replace("package=com.yuyan.pinyin.online.release ","package=other.app ");
        check(!ImeWindowState.hasLowerPresentation(hidden+other,id),"do not combine owner and visibility from different windows");
        String nav=shown.replace("ty=PRESENTATION ","ty=INPUT_METHOD ")+"Requested w=1240 h=1080\nmGivenContentInsets=[0,968][0,0]\n";
        check(!ImeWindowState.hasLowerKeys(nav,id),"navigation-only IME is not usable keys");
        check(ImeWindowState.hasLowerKeys(nav.replace("[0,968]","[0,305]"),id),"full keyboard content is present");
        String popup=shown.replace("ty=PRESENTATION ","ty=APPLICATION_ATTACHED_DIALOG ")+"mParentWindow=Window{abc u0 InputMethod}\n";
        check(ImeWindowState.hasLowerKeys(nav+popup,id),"floating keys beside navigation strip are present");
        check(!ImeWindowState.hasLowerKeys(popup.replace("isVisible=true","isVisible=false"),id),"hidden floating keys are absent");
        check(!ImeWindowState.hasLowerKeys(popup.replace(" u0 InputMethod}"," u0 AppDialog}"),id),"ordinary app popup is not IME keys");
        check(!ImeWindowState.hasLowerKeys(popup.replace("mDisplayId=4 ","mDisplayId=0 "),id),"upper floating keys do not validate lower output");
        check(ImeWindowState.hasLowerKeys(shown,id),"dual-screen Presentation remains supported");

    }
    private static void imeConnectionTests(){
        String ime="com.tencent.wetype/.plugin.hld.WxHldService",editor="com.netease.uuremote";
        String ready="  mCurClient=ClientState{e162e45 uid=10151 pid=32438 displayId=0} mCurSeq=2609\n"
            +"  mCurId="+ime+" mHaveConnection=true mBoundToMethod=true mVisibleBound=true\n"
            +"  mCurTokenDisplayId=4\n"
            +"Input method service state for com.tencent.wetype.plugin.hld.WxHldService@2ed69b6:\n"
            +"  mStartedInputConnection=RemoteInputConnection{idHash=#438f9ce}\n"
            +"  mInputStarted=true mInputViewStarted=true mCandidatesViewStarted=false\n"
            +"  mInputEditorInfo:\n    packageName="+editor+" fieldId=2131428065\n";
        check(ImeConnectionState.ready(ready,ime,editor,0,4),"visible keyboard must also be bound to upper UU");
        check(ImeConnectionState.editorDisplay(ready)==0,"picker captures the upper IME client despite lower dialog focus");
        check(ImeConnectionState.editorDisplay(ready.replace("displayId=0","displayId=4"))==4,"lower editor remains lower during picker");
        check(ImeConnectionState.editorDisplay("  mCurClient=null\n history displayId=0")==-1,"missing client cannot choose a historical editor");
        check(ImeConnectionState.editorDisplay(null)==-1,"unavailable client does not invent a display");
        check(!ImeConnectionState.ready(ready,"io.github.thorpanel/.DirectIme",editor,0,4),"old real keyboard surface cannot validate hidden IME");
        check(!ImeConnectionState.ready(ready.replace("mHaveConnection=true","mHaveConnection=false"),ime,editor,0,4),"disconnected IME is not ready");
        check(!ImeConnectionState.ready(ready.replace("mBoundToMethod=true","mBoundToMethod=false"),ime,editor,0,4),"unbound IME is not ready");
        check(!ImeConnectionState.ready(ready.replace("displayId=0","displayId=4"),ime,editor,0,4),"lower editor cannot satisfy upper UU request");
        check(!ImeConnectionState.ready(ready.replace("mCurTokenDisplayId=4","mCurTokenDisplayId=0"),ime,editor,0,4),"upper keyboard does not satisfy lower request");
        check(!ImeConnectionState.ready(ready.replace("mInputStarted=true","mInputStarted=false"),ime,editor,0,4),"finished editor is not ready even if metadata remains");
        check(!ImeConnectionState.ready(ready.replace("RemoteInputConnection{idHash=#438f9ce}","null"),ime,editor,0,4),"null input connection is not ready");
        check(!ImeConnectionState.ready(ready.replace("packageName="+editor,"packageName=io.github.thorpanel"),ime,editor,0,4),"panel placeholder editor is not ready");
        check(!ImeConnectionState.ready(ready.replace("packageName="+editor,"packageName="+editor+".other"),ime,editor,0,4),"editor package must match exactly");
        String history="    packageName="+editor+" fieldId=2131428065\n";
        check(!ImeConnectionState.ready(history+ready.replace("packageName="+editor,"packageName=other.app"),ime,editor,0,4),"historical UU editor cannot satisfy current editor check");
        check(!ImeConnectionState.ready(ready.substring(0,ready.indexOf("\nInput method service")),ime,editor,0,4),"missing service dump is not success");
        check(!ImeConnectionState.ready(null,ime,editor,0,4),"unavailable state is not success");
    }
    private static void routeTests(){
        String wechat="com.tencent.wetype/.plugin.hld.WxHldService";
        InputRoutePolicy p=new InputRoutePolicy(false);
        check(p.update(false)==InputRoutePolicy.NONE,"idle leaves route unchanged");
        check(p.update(true)==InputRoutePolicy.OPENED,"opening picker arms a choice, not an exit");
        check(p.update(true)==InputRoutePolicy.NONE,"open picker is handled once");
        check(p.update(false)==InputRoutePolicy.CLOSED,"dismissal is evaluated separately");
        check(!InputRoutePolicy.explicitExternalChoice("io.github.thorpanel/.DirectIme"),"cancelled picker preserves default");
        check(!InputRoutePolicy.explicitExternalChoice("io.github.thorpanel/.RelayIme"),"choosing gateway preserves lower route");
        check(InputRoutePolicy.explicitExternalChoice(wechat),"explicitly selecting same WeChat exits lower route");
        check(InputRoutePolicy.explicitExternalChoice("other.ime/.Keyboard"),"explicitly selecting another normal IME exits");
        check(!InputRoutePolicy.explicitExternalChoice(null),"unavailable state is not a manual choice");
        p=new InputRoutePolicy(true);
        check(p.update(true)==InputRoutePolicy.NONE,"launch picker is ignored");
        check(p.update(false)==InputRoutePolicy.NONE,"finishing launch choice cannot cancel itself");
        check(p.update(true)==InputRoutePolicy.OPENED,"next picker is observed");
        check(p.update(false)==InputRoutePolicy.CLOSED,"next cancellation is evaluated");
    }
    private static void dockVisibilityTests(){
        DockVisibility dock=new DockVisibility();
        check(!dock.visible(),"no keyboard means no background");
        dock.beginOpen();check(dock.visible(),"keep target available during explicit keyboard handoff");
        dock.keyboard(false);check(dock.visible(),"temporary handoff hide does not interrupt opening");
        dock.finishOpen();check(!dock.visible(),"failed/hidden keyboard cannot leave a permanent background");
        dock.keyboard(true);check(dock.visible(),"real keyboard automatically restores background");
        dock.keyboard(false);check(!dock.visible(),"leaving editor or suspending app hides background");
        dock.keyboard(true);check(dock.visible(),"refocusing editor restores without changing default IME");
        dock.showSettings();check(!dock.visible(),"settings hide background");
        dock.keyboard(true);check(!dock.visible(),"keyboard events cannot cover the settings page");
        dock.keyboard(false);dock.beginOpen();check(dock.visible(),"returning from settings permits a new handoff");
        dock.keyboard(true);dock.finishOpen();check(dock.visible(),"settings return preserves shown keyboard");
        dock.keyboard(false);check(!dock.visible(),"keyboard's own hide button also hides background");
        dock.showSettings();dock.dismissSettings();check(!dock.visible(),"closing settings opened while suspended does not reopen keyboard");
        dock.keyboard(true);check(dock.visible(),"next actual keyboard can restore after suspended settings close");
        dock.editorDisplay(4);check(!dock.visible(),"lower editor must remain visible instead of being covered by background");
        dock.beginOpen();check(!dock.visible(),"a lower-screen handoff must never flash the background");
        dock.keyboard(true);dock.finishOpen();check(!dock.visible(),"lower same-screen keyboard does not need a background");
        dock.editorDisplay(0);check(dock.visible(),"moving input back to upper restores the background");
        dock.showSettings();dock.editorDisplay(4);dock.editorDisplay(0);check(!dock.visible(),"screen changes cannot reveal background above settings");
        dock.dismissSettings();check(dock.visible(),"current upper keyboard can restore when settings close");
        dock.keyboard(false);dock.editorDisplay(4);dock.editorDisplay(0);check(!dock.visible(),"changing display alone never opens hidden keyboard background");
        dock.editorDisplay(-1);dock.keyboard(true);check(!dock.visible(),"unknown display must not cover an application");
    }
    private static void setupTests(){
        SetupState ready=new SetupState(SetupState.ShizukuStatus.READY,true,true,false);
        check(ready.showGuide(false),"first launch explains setup even on a preconfigured device");
        check(ready.canUse(),"default lower-screen input is optional");
        check(!ready.showGuide(true),"completed setup opens the panel without recurring onboarding");
        for(SetupState.ShizukuStatus status:SetupState.ShizukuStatus.values()){
            if(status==SetupState.ShizukuStatus.READY)continue;
            SetupState lost=new SetupState(status,true,true,true);
            check(!lost.canUse(),"a remembered default does not mask a lost prerequisite: "+status);
            check(lost.showGuide(true),"revisit recovery after restart, revoke, uninstall or old Shizuku: "+status);
        }
        SetupState disabledKeyboard=new SetupState(SetupState.ShizukuStatus.READY,false,true,true);
        check(disabledKeyboard.showGuide(true)&&!disabledKeyboard.canUse(),"disabled or removed keyboard needs recovery despite remembered default");
        SetupState lowerOff=new SetupState(SetupState.ShizukuStatus.READY,true,false,true);
        check(lowerOff.showGuide(true)&&!lowerOff.canUse(),"closed lower display keeps setup accessible instead of exiting");
        SetupState restored=new SetupState(SetupState.ShizukuStatus.READY,true,true,true);
        check(!restored.showGuide(true)&&restored.canUse(),"restored prerequisites resume normal launch");
        check(ready.canUse()==restored.canUse(),"choosing an ordinary IME does not force setup again");
    }
    public static void main(String[] args){padTests();modifierTests();imeWindowTests();imeConnectionTests();routeTests();dockVisibilityTests();setupTests();System.out.println("Passed "+checks+" interaction checks");}
}
