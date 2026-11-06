// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Verify letter case and matching Shift events for both keyboard output routes. */
public final class KeyboardInputTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void equal(List<String> actual,String... expected){check(actual.equals(Arrays.asList(expected)),actual+" != "+Arrays.toString(expected));}
    private static final class Keyboard implements ModifierKeys.Output {
        final List<String> events=new ArrayList<>(),letters=new ArrayList<>();
        final Set<Integer> down=new HashSet<>();
        final KeyboardInput input=new KeyboardInput(this);
        public void key(int code,boolean pressed,int meta){
            check(code!=115,"Caps must never toggle the computer's Caps Lock");
            if(pressed)down.add(code);else down.remove(code);
            check(((meta&0x41)!=0)==down.contains(59),"Android and USB HID agree on Shift");
            events.add(code+(pressed?"D":"U")+":"+meta);
            if(pressed&&code>=29&&code<=54)letters.add(String.valueOf((char)((down.contains(59)?'A':'a')+code-29)));
        }
        void tap(int code,long time){input.down(code,time);input.up(code,time+50,false);}
        void lock(){tap(115,0);tap(115,150);check(input.isLocked(115),"double tap locks Caps");}
    }
    public static void main(String[] args){
        Keyboard k=new Keyboard();
        check(!k.input.isActive(115)&&!k.input.isLocked(115),"keyboard starts in lowercase");
        for(int code=29;code<=54;code++){
            check(k.input.label(code,"ignored").equals(String.valueOf((char)('a'+code-29))),"default letter keycap is lowercase");k.tap(code,0);
        }
        equal(k.letters,"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z");
        k=new Keyboard();k.tap(115,0);
        check(k.events.isEmpty()&&k.input.isActive(115)&&!k.input.isLocked(115),"single Caps arms uppercase without sending a lock key");
        check(k.input.label(29,"A").equals("A"),"one-shot uppercase appears on keycaps");
        for(int code:new int[]{8,55,62,67,19,66})k.tap(code,100);
        check(k.input.isActive(115)&&!k.down.contains(59),"non-letters do not consume uppercase or hold Shift");
        k.events.clear();k.tap(29,200);k.tap(30,300);
        equal(k.letters,"A","b");equal(k.events,"59D:65","29D:65","29U:65","59U:0","30D:0","30U:0");
        check(k.input.label(29,"A").equals("a")&&!k.input.isActive(115),"one letter restores lowercase keycaps and Caps state");
        k=new Keyboard();k.lock();k.tap(29,300);k.tap(30,400);k.tap(54,500);
        equal(k.letters,"A","B","Z");check(k.input.isLocked(115)&&k.input.label(54,"Z").equals("Z"),"lock survives multiple letters");
        k.events.clear();k.tap(8,600);k.tap(55,700);equal(k.events,"8D:0","8U:0","55D:0","55U:0");
        k.tap(115,800);k.tap(31,900);equal(k.letters,"A","B","Z","c");
        check(!k.input.isActive(115)&&!k.input.isLocked(115),"single tap unlocks Caps");
        k=new Keyboard();k.tap(115,0);k.tap(115,500);
        check(!k.input.isActive(115),"separate slow taps cancel one-shot instead of locking");
        k=new Keyboard();k.tap(115,0);k.tap(29,100);k.tap(115,180);
        check(k.input.isActive(115)&&!k.input.isLocked(115),"typing between Caps taps starts a new one-shot");
        k=new Keyboard();k.tap(115,0);k.tap(8,100);k.tap(115,180);
        check(!k.input.isLocked(115),"a non-letter interrupts double-tap detection");
        k=new Keyboard();k.input.down(115,0);k.input.up(115,50,true);
        check(!k.input.isActive(115)&&k.events.isEmpty(),"canceled or outside Caps release does nothing");
        k.tap(115,100);k.input.down(115,200);k.input.up(115,250,true);
        check(k.input.isActive(115)&&!k.input.isLocked(115),"canceled second tap preserves one-shot without locking");
        k=new Keyboard();k.input.down(115,0);k.input.up(115,400,false);k.tap(115,450);
        check(!k.input.isLocked(115),"long press cannot form a double tap");
        k=new Keyboard();k.tap(115,0);k.input.down(29,100);k.input.down(30,110);
        equal(k.letters,"A","b");check(k.input.label(29,"A").equals("a"),"overlapping next letter and keycaps are lowercase");
        k.input.up(30,150,false);k.input.up(29,160,false);check(k.down.isEmpty(),"overlapping releases leave no keys held");
        k=new Keyboard();k.lock();k.input.down(29,300);k.tap(8,310);k.input.up(29,400,false);
        check(k.events.contains("8D:0")&&k.down.isEmpty(),"number entered while uppercase letter is held remains unshifted");
        for(int modifier:new int[]{113,57,117}){
            k=new Keyboard();k.tap(115,0);k.input.down(modifier,100);k.tap(29,150);k.input.up(modifier,220,false);
            check(k.input.isActive(115),"shortcut preserves one-shot for actual text");
            k.tap(30,300);equal(k.letters,"a","B");check(k.down.isEmpty(),"shortcut and automatic Shift both release");
        }
        k=new Keyboard();k.tap(115,0);k.input.down(29,100);k.input.down(59,110);k.input.up(29,150,false);
        check(k.down.contains(59),"automatic Shift release preserves a physically held Shift");
        k.input.up(59,180,false);check(k.down.isEmpty(),"physical Shift finally releases");
        k=new Keyboard();k.tap(115,0);k.input.down(59,100);k.input.down(29,110);k.input.up(59,150,false);
        check(k.down.contains(59),"physical Shift release preserves the held uppercase letter");
        k.input.up(29,180,false);check(k.down.isEmpty(),"uppercase letter finally releases Shift");
        k=new Keyboard();k.input.down(59,0);k.tap(29,50);k.input.up(59,120,false);k.tap(30,200);
        equal(k.letters,"A","b");check(!k.input.isActive(115),"ordinary Shift chord does not change Caps state");
        k=new Keyboard();k.tap(59,0);k.tap(59,150);k.tap(29,300);
        check(k.input.isLocked(59)&&k.input.label(29,"A").equals("A"),"Shift double-tap lock still works");
        k.tap(59,400);k.tap(30,500);equal(k.letters,"A","b");
        k=new Keyboard();k.tap(115,0);k.input.down(67,100);k.input.repeat(67);k.input.up(67,200,false);
        equal(k.events,"67D:0","67D:0","67U:0");check(k.input.isActive(115),"backspace repeat preserves next uppercase letter");
        k=new Keyboard();k.lock();k.input.down(29,300);k.input.down(113,310);k.input.reset();
        check(k.down.isEmpty()&&!k.input.isActive(115)&&k.input.label(29,"A").equals("a"),"mode change or cancellation releases all keys and returns to lowercase");
        int before=k.events.size();k.input.up(29,400,false);k.input.up(115,400,false);k.input.reset();
        check(before==k.events.size(),"late releases after reset cannot emit keys or relock Caps");
        k=new Keyboard();k.input.down(115,0);k.input.reset();k.input.up(115,100,false);
        check(!k.input.isActive(115)&&k.events.isEmpty(),"canceling an in-progress Caps tap cannot arm uppercase");
        System.out.println("Passed "+checks+" keyboard case checks");
    }
}
