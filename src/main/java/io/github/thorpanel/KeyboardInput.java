// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local letter case, shared by the keycaps, Android events and USB HID events. */
final class KeyboardInput {
    private static final int CAPS=115,SHIFT=59,SHIFT_META=0x1|0x40;
    private static final int SHORTCUT_META=0x1000|0x2|0x10000;
    private static final long TAP_MS=250,DOUBLE_MS=350;
    private final ModifierKeys.Output output;
    private final ModifierKeys modifiers;
    // Preserve each held key's case; another finger must not inherit its Shift.
    private final Map<Integer,Boolean> held=new LinkedHashMap<>();
    private boolean nextUpper,locked,shiftDown;
    private long capsPress=-1,lastCapsTap=-1;

    KeyboardInput(ModifierKeys.Output output){
        this.output=output;modifiers=new ModifierKeys(this::modifierEvent);
    }
    private static boolean letter(int code){return code>=29&&code<=54;}
    private boolean shortcut(){return (modifiers.meta()&SHORTCUT_META)!=0;}
    boolean isActive(int code){return code==CAPS?nextUpper||locked:modifiers.isActive(code);}
    boolean isLocked(int code){return code==CAPS?locked:modifiers.isLocked(code);}
    String label(int code,String fallback){
        if(!letter(code))return fallback;
        boolean upper=nextUpper||locked||modifiers.isActive(SHIFT);
        return String.valueOf((char)((upper?'A':'a')+code-29));
    }
    private int meta(){return (modifiers.meta()&~SHIFT_META)|(shiftDown?SHIFT_META:0);}
    private boolean latestUpper(){boolean upper=false;for(boolean value:held.values())upper=value;return upper;}
    private void shift(boolean upper){
        boolean desired=modifiers.isActive(SHIFT)||(upper&&!shortcut());
        if(shiftDown==desired)return;
        shiftDown=desired;output.key(SHIFT,desired,meta());
    }
    private void modifierEvent(int code,boolean down,int ignored){
        shift(latestUpper());
        if(code!=SHIFT)output.key(code,down,meta());
    }
    void down(int code,long time){
        if(code==CAPS){
            if(capsPress<0){capsPress=time;modifiers.chord();}
            return;
        }
        lastCapsTap=-1;
        if(ModifierKeys.isModifier(code)){
            modifiers.down(code,time);if(!held.isEmpty())modifiers.chord();return;
        }
        if(held.containsKey(code))return;
        modifiers.chord();
        boolean upper=letter(code)&&!shortcut()&&(nextUpper||locked);
        if(letter(code)&&!shortcut())nextUpper=false;
        held.put(code,upper);shift(upper);output.key(code,true,meta());
    }
    void up(int code,long time,boolean canceled){
        if(code==CAPS){
            if(capsPress<0)return;
            boolean tap=time-capsPress<=TAP_MS;capsPress=-1;
            if(canceled){lastCapsTap=-1;return;}
            if(locked){locked=false;nextUpper=false;lastCapsTap=-1;}
            else if(nextUpper&&tap&&lastCapsTap>=0&&time-lastCapsTap<=DOUBLE_MS){
                locked=true;nextUpper=false;lastCapsTap=-1;
            }else{
                nextUpper=!nextUpper;lastCapsTap=nextUpper&&tap?time:-1;
            }
            return;
        }
        if(ModifierKeys.isModifier(code)){modifiers.up(code,time,canceled);return;}
        Boolean upper=held.remove(code);if(upper==null)return;
        shift(upper);output.key(code,false,meta());shift(latestUpper());
    }
    void repeat(int code){
        Boolean upper=held.get(code);if(upper==null)return;
        shift(upper);output.key(code,true,meta());shift(latestUpper());
    }
    void reset(){
        for(int code:new ArrayList<>(held.keySet()))up(code,0,true);
        modifiers.reset();shift(false);
        nextUpper=locked=false;capsPress=lastCapsTap=-1;
    }
}
