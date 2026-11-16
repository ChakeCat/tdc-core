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
        m=new Mouse();m.pad.down(0,10,10,0,1);long token=m.pad.token();m.pad.hold(token,100);
        check(!m.dragging,"default hold does not drag at the former 100 ms threshold");m.pad.hold(token,299);
        check(m.buttons.isEmpty(),"hold must not start before 300 ms");m.pad.hold(token,300);
        check(m.dragging,"one stationary press starts dragging");
        m.pad.update(0,50,30);m.pad.finishMove();check(m.dx==40&&m.dy==20,"drag moves pointer");
        m.pad.up(0,800);equal(m.buttons,"1D","1U");check(!m.dragging,"finger up releases drag");
        m=new Mouse();m.pad.down(0,900,10,0,2);m.pad.hold(m.pad.token(),300);m.pad.up(0,500);
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
        check(ImeWindowState.hasLowerFloatingKeys(shown,id,4),"Presentation can validate a separate keyboard surface");
        check(ImeWindowState.hasLowerFloatingKeys(popup,id,4),"attached popup can validate a separate keyboard surface");
        check(!ImeWindowState.hasLowerFloatingKeys(nav.replace("[0,968]","[0,305]"),id,4),"standard IME window needs compositor proof even with full content insets");
        check(!ImeWindowState.hasLowerFloatingKeys(popup,id,0),"floating fallback keeps display isolation");
        check(!ImeWindowState.hasLowerFloatingKeys(popup,"other/.Ime",4),"floating fallback keeps owner isolation");
        check(!ImeWindowState.hasLowerFloatingKeys(popup.replace("isVisible=true","isVisible=false"),id,4),"hidden popup is not a floating fallback");
        check(!ImeWindowState.hasLowerFloatingKeys(null,id,4),"missing window data is not a floating fallback");

    }
    private static void imeFloatingCompositionTests() {
        String ime = "keyboard/.Ime", name = "PopupWindow:48d934c";
        String window = keyboardFrame("APPLICATION_ATTACHED_DIALOG", 7, "[0,389][620,934]")
                .replace("u10 InputMethod", "u0 InputMethod")
                + "    mSurface=Surface(name=" + name + ")/@0x123\n";
        String output = "Display 123 (physical)\n Composition Display State:\n"
                + " layerFilter={layerStack=7 toInternalDisplay=true }\n"
                + " - Output Layer 0x123(" + name + "#368)\n"
                + " Region visibleRegion (this=0x1, count=1)\n";
        for (String type : new String[]{"APPLICATION_ATTACHED_DIALOG", "PRESENTATION"}) {
            String candidate = window.replace("ty=APPLICATION_ATTACHED_DIALOG", "ty=" + type);
            check(ImeSurfaceState.visible(output, candidate, ime, 7),
                    "composed owned floating keyboard remains supported: " + type);
            for (String absent : new String[]{null, "", output.replace(name, "PanelActivity"),
                    output.replace(name, name + "other"), output.replace("layerStack=7", "layerStack=0"),
                    output.replace("count=1", "count=0"),
                    output.replace(name + "#368", "Surface(name=" + name + ") - animation-leash#368"),
                    "Offscreen Layers:\n" + name + "#368\nRegion visibleRegion (count=1)\n"}) {
                check(!ImeSurfaceState.visible(absent, candidate, ime, 7),
                        "WM visible floating window requires its own composed output: " + type);
            }
            for (String unrelated : new String[]{null, "", candidate.replace("package=keyboard", "package=other"),
                    candidate.replace("mDisplayId=7", "mDisplayId=0"),
                    candidate.replace("isVisible=true", "isVisible=false"),
                    candidate.replace("Surface: shown=true", "Surface: shown=false"),
                    candidate.replace("mDrawState=HAS_DRAWN", "mDrawState=NO_SURFACE"),
                    candidate.replace("mSurface=", "unknownSurface="),
                    candidate.replace(name, "PopupWindow:other")}) {
                check(!ImeSurfaceState.visible(output, unrelated, ime, 7),
                        "composed popup needs matching visible window ownership: " + type);
            }
        }
        check(!ImeSurfaceState.visible(output, window.replace("u0 InputMethod}", "u0 Other}"), ime, 7),
                "ordinary application popup cannot become keyboard evidence");
        check(ImeSurfaceState.visible(output, window.replace("u0 InputMethod}", "u10 InputMethod}"), ime, 7),
                "secondary-user IME attached windows retain composition proof");
        String mainWindow = keyboardFrame("INPUT_METHOD", 7, "[0,0][1240,1080]")
                + "    Requested w=1240 h=1080\n    mGivenContentInsets=[0,955][0,0]\n";
        String mainOutput = output.replace(name, "InputMethod");
        check(!ImeSurfaceState.visible(mainOutput, mainWindow + window, ime, 7),
                "composed navigation strip cannot validate an uncomposed floating keyboard");
        check(ImeSurfaceState.visible(mainOutput, mainWindow.replace("[0,955]", "[0,300]") + window, ime, 7),
                "independently usable main keyboard can validate composition beside a stale popup");
        check(!ImeSurfaceState.visible(mainOutput, mainWindow, ime, 7),
                "navigation-only main surface remains invalid after the floating window disappears");
        check(ImeSurfaceState.visible(mainOutput, mainWindow.replace("[0,955]", "[0,300]"), ime, 7),
                "main keyboard needs both its own key area and composed surface");
        for (String invalidMain : new String[]{null, "", mainWindow.replace("package=keyboard", "package=other"),
                mainWindow.replace("mDisplayId=7", "mDisplayId=0"),
                mainWindow.replace("mGivenContentInsets=", "unknownContentInsets=")}) {
            check(!ImeSurfaceState.visible(mainOutput, invalidMain, ime, 7),
                    "missing or unrelated main keyboard geometry cannot validate navigation");
        }
        check(ImeSurfaceState.visible(mainOutput, 7),
                "legacy surface-only query retains its independent composition semantics");
        String hidden = window.replace("isVisible=true", "isVisible=false");
        String foreign = window.replace("package=keyboard", "package=other");
        check(!ImeSurfaceState.visible(output, hidden + foreign, ime, 7),
                "separate hidden and foreign windows cannot supply a floating surface");
        check(!ImeSurfaceState.visible(output.replace("layerStack=7", "layerStack=0")
                        + output.replace(name, "PanelActivity"), window, ime, 7),
                "other display composition cannot complete the target display's output");
    }
    private static String keyboardFrame(String type, int display, String bounds) {
        return "\n  Window #0 Window{keys u0 InputMethod}:\n"
            +"    mDisplayId="+display+" rootTaskId=1\n    package=keyboard appop=NONE\n"
            +"    mAttrs={ty="+type+" }\n    mParentWindow=Window{parent u10 InputMethod}\n"
            +"    Surface: shown=true layer=0 alpha=1.0\n    mDrawState=HAS_DRAWN\n    isVisible=true\n"
            +"    Frames: parent=[0,0][2000,2000] display=[0,0][2000,2000] frame="+bounds
            +" last=[-50,0][3000,2000] insetsChanged=false\n";
    }
    private static void imeHorizontalBoundsTests() {
        String ime="keyboard/.Ime";
        ImeWindowState.HorizontalBounds fits=ImeWindowState.HorizontalBounds.FITS;
        ImeWindowState.HorizontalBounds overflow=ImeWindowState.HorizontalBounds.OVERFLOW;
        ImeWindowState.HorizontalBounds unknown=ImeWindowState.HorizontalBounds.UNKNOWN;
        for(String type:new String[]{"INPUT_METHOD","PRESENTATION","APPLICATION_ATTACHED_DIALOG"}) {
            String window=keyboardFrame(type,7,"[0,100][640,400]");
            check(ImeWindowState.horizontalBounds(window,ime,7,640)==fits,"visible keyboard outer frame fits exactly: "+type);
            check(ImeWindowState.horizontalBounds(window.replace("\n","\r\n"),ime,7,640)==fits,"Android 13/16 Frames dump accepts CRLF: "+type);
            check(ImeWindowState.horizontalBounds(window.replace("frame=[0,100]","frame=[-1,100]"),ime,7,640)==overflow,"negative left is explicit horizontal overflow: "+type);
            check(ImeWindowState.horizontalBounds(window,ime,7,639)==overflow,"right edge beyond current width is explicit overflow: "+type);
            for(int width:new int[]{320,500,640,800,1280})
                check(ImeWindowState.horizontalBounds(window,ime,7,width)==(width<640?overflow:fits),"frame is compared with each current width, independent of density or preset dimensions");
            String absentFrame=window.replace("Frames:","OtherFrames:");
            check(ImeWindowState.horizontalBounds(absentFrame,ime,7,640)==unknown,"unrelated frame sections are not the window's outer frame");
            for(String invalid:new String[]{"[0,100][0,400]","[640,100][0,400]","[0,400][640,100]",
                    "[0,100][2147483648,400]","[-2147483649,100][640,400]","[0,999999999999999][640,400]","[]"})
                check(ImeWindowState.horizontalBounds(keyboardFrame(type,7,invalid),ime,7,640)==unknown,"missing, empty or numerically invalid geometry remains unknown");
            check(ImeWindowState.horizontalBounds(keyboardFrame(type,7,"[-2147483648,100][640,400]"),ime,7,640)==overflow,"minimum valid coordinate cannot overflow arithmetic");
        }
        String good=keyboardFrame("INPUT_METHOD",7,"[0,100][640,400]");
        String wide=keyboardFrame("INPUT_METHOD",7,"[0,100][1280,400]");
        for(String other:new String[]{wide.replace("package=keyboard ","package=keyboard.other "),
                wide.replace("mDisplayId=7 ","mDisplayId=70 "),wide.replace("isVisible=true","isVisible=false"),
                wide.replace("mDrawState=HAS_DRAWN","mDrawState=DRAW_PENDING"),wide.replace("Surface: shown=true","Surface: shown=false"),
                wide.replace("ty=INPUT_METHOD ","ty=APPLICATION "),
                keyboardFrame("APPLICATION_ATTACHED_DIALOG",7,"[0,100][1280,400]").replace("u10 InputMethod}","u10 AppDialog}")}) {
            check(ImeWindowState.horizontalBounds(other,ime,7,640)==unknown,"foreign display/owner, hidden or ordinary app frame is excluded");
            check(ImeWindowState.horizontalBounds(good+other,ime,7,640)==fits,"unrelated oversized window cannot invalidate current keyboard");
        }
        check(ImeWindowState.horizontalBounds(good+wide,ime,7,640)==overflow,"one oversized current keyboard window prevents false success");
        check(ImeWindowState.horizontalBounds(good+wide.replace("Frames:","OtherFrames:"),ime,7,640)==unknown,"a missing current keyboard frame is not proved fitting by another window");
        String hidden=wide.replace("isVisible=true","isVisible=false");
        String foreign=wide.replace("package=keyboard ","package=other ");
        check(ImeWindowState.horizontalBounds(hidden+foreign,ime,7,640)==unknown,"identity and visibility from separate windows are never combined");
        check(ImeWindowState.horizontalBounds(good.replace("frame=[0,100][640,400]","frame=[0,100][400,400]"),ime,7,480)==fits,"resized window fits after rotation");
        check(ImeWindowState.horizontalBounds(good,ime,7,480)==overflow,"old landscape window cannot pass after narrower rotation");
        check(ImeWindowState.horizontalBounds(good,ime,7,960)==fits,"same outer frame remains valid after wider rotation");
        for(String dump:new String[]{null,"",good.replace("Frames:","Unknown:")})
            check(ImeWindowState.horizontalBounds(dump,ime,7,640)==unknown,"missing frame data remains unknown");
        for(String id:new String[]{null,"", "keyboard", "/Ime", "other/.Ime"})
            check(ImeWindowState.horizontalBounds(good,id,7,640)==unknown,"missing or unrelated input method cannot claim a frame");
        check(ImeWindowState.horizontalBounds(good,ime,7,0)==unknown,"zero display width remains unknown");
        check(ImeWindowState.horizontalBounds(good,ime,7,-1)==unknown,"negative display width remains unknown");
        check(ImeWindowState.horizontalBounds(good,ime,-1,640)==unknown,"missing display id remains unknown");
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

    private static String android16Ime(int user, int editorDisplay, int keyboardDisplay, String editor) {
        // Shape from AOSP android16-release IMMS/BindingController/InputMethodService dumps.
        return "  UserId="+user+"\n"
            +"    mBindingController:\n"
            +"      mSelectedMethodId=keyboard/.Ime\n"
            +"      mCurId=keyboard/.Ime\n"
            +"      mHasMainConnection=true\n"
            +"      mVisibleBound=true\n"
            +"      mCurTokenDisplayId="+keyboardDisplay+"\n"
            +"    mCurClient=ClientState{e162e45 mUid=10151 mPid=32438 mSelfReportedDisplayId="+editorDisplay+"}\n"
            +"    mImeBindingState:\n"
            +"      mFocusedWindow=android.os.BinderProxy@acb\n"
            +"    mBoundToMethod=true\n"
            +"    Input Methods:\n"
            +"      InputMethod #0:\n"
            +"        mId=keyboard/.Ime\n"
            +"Input method client state for android.view.inputmethod.InputMethodManager@eea:\n"
            +"  mCurRootView=android.view.ViewRootImpl@abc\n"
            +"Input method service state for keyboard.Ime@abc:\n"
            +"  mStartedInputConnection=RemoteInputConnection{idHash=#ff}\n"
            +"  mInputStarted=true mInputViewStarted=true\n"
            +"  mInputEditorInfo:\n"
            +"    packageName="+editor+" fieldId=1\n";
    }
    private static void android16ImeTests() {
        String ime="keyboard/.Ime", editor="remote.app";
        String header="Input Method Manager Service state:\n  mCurrentImeUserId=0\n"
            +"  mUserDataRepository:\n    userId=10\n      curClient=ClientState{old mSelfReportedDisplayId=4}\n"
            +"      curEditorInfo:\n        packageName=old.app\n"
            +"  mStartInputHistory:\n    packageName=old.app\n";
        String current=android16Ime(0,0,2,editor), ready=header+current;
        check(ImeConnectionState.ready(ready,ime,editor,0,2),"Android16 current user binding, client and IME service agree");
        check(ImeConnectionState.editorDisplay(ready)==0,"Android16 current client uses self-reported display id");
        check(ImeConnectionState.ready(ready.replace("\n","\r\n"),ime,editor,0,2),"Android16 CRLF dump is accepted");
        check(!ImeConnectionState.ready(ready,ime,editor,2,0),"Android16 reversed screen request cannot borrow correct pair");
        check(ImeConnectionState.ready(header+android16Ime(0,2,0,editor),ime,editor,2,0),"parser does not hard-code either display role");
        for(String[] replacement:new String[][]{
            {"mCurId=keyboard/.Ime","mCurId=other/.Ime"},
            {"mHasMainConnection=true","mHasMainConnection=false"},
            {"mBoundToMethod=true","mBoundToMethod=false"},
            {"mCurTokenDisplayId=2","mCurTokenDisplayId=0"},
            {"mSelfReportedDisplayId=0","mSelfReportedDisplayId=2"},
            {"mInputStarted=true","mInputStarted=false"},
            {"mStartedInputConnection=RemoteInputConnection{idHash=#ff}","mStartedInputConnection=null"},
            {"packageName=remote.app","packageName=remote.app.other"},
            {"mBindingController:","missingController:"},
            {"mCurrentImeUserId=0","mCurrentImeUserId=10"},
            {"mCurrentImeUserId=0","mCurrentImeUserId=999999999999999999999"},
            {"mCurTokenDisplayId=2","mCurTokenDisplayId=999999999999999999999"},
            {"mSelfReportedDisplayId=0","mSelfReportedDisplayId=999999999999999999999"},
            {"  UserId=0","  UserId=999999999999999999999"}}) {
            check(!ImeConnectionState.ready(ready.replace(replacement[0],replacement[1]),ime,editor,0,2),
                "Android16 rejects absent/mismatched active evidence: "+replacement[0]);
        }
        check(ImeConnectionState.editorDisplay(ready.replace("mSelfReportedDisplayId=0","mSelfReportedDisplayId=999999999999999999999"))==-1,"oversized editor ids do not throw");
        check(!ImeConnectionState.ready(current,ime,editor,0,2),"modern per-user dump requires current user marker");
        check(ImeConnectionState.editorDisplay(header)==-1,"header repository does not become current client");
        check(!ImeConnectionState.ready(header,ime,editor,0,2),"header and history alone do not prove input");
        String other=android16Ime(10,4,4,"other.app");
        check(ImeConnectionState.ready(header+other+current,ime,editor,0,2),"current user can follow another complete user block");
        check(ImeConnectionState.ready(header+current+other,ime,editor,0,2),"later user block does not pollute current service");
        check(ImeConnectionState.editorDisplay(header+other+current)==0,"capture ignores other user's client");
        check(ImeConnectionState.editorDisplay((header+current+other).replace("mCurrentImeUserId=0","mCurrentImeUserId=10"))==4,"capture selects explicitly current user");
        check(!ImeConnectionState.ready(header+other+current.replace("mHasMainConnection=true","mHasMainConnection=false"),ime,editor,0,2),"other user binding cannot complete current user");
        check(!ImeConnectionState.ready(header+current.replace("packageName=remote.app","packageName=other.app")+android16Ime(10,0,2,editor),ime,editor,0,2),"other user's service cannot complete current user");
        check(!ImeConnectionState.ready(header+current.substring(0,current.indexOf("Input method service state"))+android16Ime(10,0,2,editor),ime,editor,0,2),"missing current service does not consume later service");
        check(!ImeConnectionState.ready(header+current+current,ime,editor,0,2),"duplicate active-user blocks are ambiguous");
        check(ImeConnectionState.editorDisplay(header+current+current)==-1,"duplicate active-user blocks cannot select an editor");
        check(!ImeConnectionState.ready(ready.replace("mCurTokenDisplayId=2","mCurTokenDisplayId=2\n      mCurTokenDisplayId=2"),ime,editor,0,2),"duplicate binding fields are ambiguous");
        check(!ImeConnectionState.ready(ready.replace("mBoundToMethod=true","mBoundToMethod=true\n    mBoundToMethod=false"),ime,editor,0,2),"conflicting binding values are rejected");
        check(!ImeConnectionState.ready(ready.replace("mCurrentImeUserId=0","mCurrentImeUserId=0\n  mCurrentImeUserId=10"),ime,editor,0,2),"ambiguous current-user marker is rejected");
        check(!ImeConnectionState.ready(ready+current.substring(current.indexOf("Input method service state")),ime,editor,0,2),"duplicate service snapshots cannot be combined");
        String missingClient=current.replace("    mCurClient=ClientState{e162e45 mUid=10151 mPid=32438 mSelfReportedDisplayId=0}","    mCurClient=null");
        check(ImeConnectionState.editorDisplay(header+missingClient+android16Ime(10,0,2,editor))==-1,"null active client does not use another user's client");
        check(!ImeConnectionState.ready(header+missingClient+android16Ime(10,0,2,editor),ime,editor,0,2),"null active client cannot borrow other user binding");
        check(!ImeConnectionState.ready(ready,ime,editor,-1,2)&&!ImeConnectionState.ready(ready,ime,editor,0,-1),"negative expected display ids are rejected");
        check(ImeConnectionState.editorDisplay(ready.replace("    mCurClient=", "    mCurClient=null\n    mCurClient="))==-1,"duplicate null and live current clients are ambiguous");
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
    private static void deviceIdentityTests() {
        String[] names = {"Thor Panel Mouse", "Thor Panel Beta Mouse", "Thor Panel Keyboard",
                "Thor Panel Beta Keyboard", "x".repeat(127), "锤".repeat(42)};
        for (String name : names) {
            byte[] encoded = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            for (boolean mouse : new boolean[] {true, false}) {
                byte[] event = mouse ? UhidMouse.createEvent(name) : UhidKeyboard.createEvent(name);
                java.nio.ByteBuffer bytes = java.nio.ByteBuffer.wrap(event)
                        .order(java.nio.ByteOrder.LITTLE_ENDIAN);
                check(bytes.getInt(0) == 11, "identity uses UHID_CREATE2");
                check(java.util.Arrays.equals(encoded,
                                java.util.Arrays.copyOfRange(event, 4, 4 + encoded.length)),
                        "exact UTF-8 device name is encoded without truncation");
                boolean paddingClear = true;
                for (int i = 4 + encoded.length; i < 260; i++)
                    paddingClear &= event[i] == 0;
                check(paddingClear, "name has terminating zero and remaining identity fields stay clear");
                check(Short.toUnsignedInt(bytes.getShort(260)) == event.length - 280
                                && bytes.getShort(262) == 3 && bytes.getInt(264) == 0x1209
                                && bytes.getInt(268) == (mouse ? 0x5450 : 0x5451)
                                && bytes.getInt(272) == 1 && bytes.getInt(276) == 0,
                        "name does not overwrite HID descriptor metadata");
            }
        }
        check(java.util.Arrays.equals(
                        java.util.Arrays.copyOfRange(UhidMouse.createEvent(names[0]), 132,
                                UhidMouse.createEvent(names[0]).length),
                        java.util.Arrays.copyOfRange(UhidMouse.createEvent(names[1]), 132,
                                UhidMouse.createEvent(names[1]).length)),
                "beta mouse changes only its name");
        check(java.util.Arrays.equals(
                        java.util.Arrays.copyOfRange(UhidKeyboard.createEvent(names[2]), 132,
                                UhidKeyboard.createEvent(names[2]).length),
                        java.util.Arrays.copyOfRange(UhidKeyboard.createEvent(names[3]), 132,
                                UhidKeyboard.createEvent(names[3]).length)),
                "beta keyboard changes only its name");
        for (String name : new String[] {null, "", "bad\0name", "x".repeat(128), "锤".repeat(43)}) {
            try {
                new UhidMouse(name);
                throw new AssertionError("invalid mouse identity accepted");
            } catch (IllegalArgumentException expected) {
                check(true, "invalid mouse name rejected before opening the device");
            } catch (java.io.IOException openedDevice) {
                throw new AssertionError("invalid mouse name reached /dev/uhid", openedDevice);
            }
            try {
                new UhidKeyboard(name);
                throw new AssertionError("invalid keyboard identity accepted");
            } catch (IllegalArgumentException expected) {
                check(true, "invalid keyboard name rejected before opening the device");
            } catch (java.io.IOException openedDevice) {
                throw new AssertionError("invalid keyboard name reached /dev/uhid", openedDevice);
            }
        }
        String beta = "io.github.thorpanel.beta", stable = "io.github.thorpanel";
        check(!InputRoutePolicy.explicitExternalChoice(beta + "/io.github.thorpanel.RelayIme", beta),
                "beta gateway remains owned by beta routing");
        check(!InputRoutePolicy.explicitExternalChoice(beta + "/io.github.thorpanel.DirectIme", beta),
                "beta direct input remains owned by beta routing");
        check(InputRoutePolicy.explicitExternalChoice(stable + "/.RelayIme", beta),
                "choosing stable gateway is an explicit external choice for beta");
        check(InputRoutePolicy.explicitExternalChoice(beta + "/io.github.thorpanel.RelayIme", stable),
                "choosing beta gateway is an explicit external choice for stable");
        check(InputRoutePolicy.explicitExternalChoice(beta + "2/.RelayIme", beta),
                "package-prefix similarity cannot capture another input method");
        check(!InputRoutePolicy.explicitExternalChoice(null, beta),
                "unavailable input method is not a manual beta choice");
    }
    public static void main(String[] args){padTests();modifierTests();imeWindowTests();imeFloatingCompositionTests();imeHorizontalBoundsTests();imeConnectionTests();android16ImeTests();routeTests();dockVisibilityTests();setupTests();deviceIdentityTests();System.out.println("Passed "+checks+" interaction checks");}
}
