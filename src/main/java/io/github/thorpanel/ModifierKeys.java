// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Physical press/release behavior, with an explicit double-tap latch. */
final class ModifierKeys {
    interface Output {void key(int code,boolean down,int meta);}
    private static final long TAP_MS=250,DOUBLE_MS=350;
    private final Output output;
    private final Set<Integer> active=new HashSet<>(),locked=new HashSet<>();
    private final Map<Integer,Press> pressed=new HashMap<>();
    private int lastTapCode=-1;
    private long lastTapTime;
    private static final class Press {
        long time;boolean used,unlock;
        Press(long time,boolean unlock){this.time=time;this.unlock=unlock;}
    }
    ModifierKeys(Output output){this.output=output;}
    static boolean isModifier(int code){return code==59||code==113||code==57||code==117;}
    boolean isActive(int code){return active.contains(code);}
    boolean isLocked(int code){return locked.contains(code);}
    int meta(){
        int result=0;
        for(int code:active)switch(code){
            case 59:result|=0x1|0x40;break; // SHIFT_ON | SHIFT_LEFT_ON
            case 113:result|=0x1000|0x2000;break;
            case 57:result|=0x2|0x10;break;
            case 117:result|=0x10000|0x20000;break;
        }
        return result;
    }
    void down(int code,long time){
        if(pressed.containsKey(code))return;
        boolean unlock=locked.remove(code);
        pressed.put(code,new Press(time,unlock));
        if(unlock){active.remove(code);lastTapCode=-1;output.key(code,false,meta());}
        else{active.add(code);output.key(code,true,meta());}
    }
    void up(int code,long time,boolean canceled){
        Press press=pressed.remove(code);if(press==null)return;
        if(press.unlock)return;
        boolean tap=!canceled&&!press.used&&time-press.time<=TAP_MS;
        if(tap&&lastTapCode==code&&time-lastTapTime<=DOUBLE_MS){
            locked.add(code);lastTapCode=-1;return;
        }
        active.remove(code);output.key(code,false,meta());
        lastTapCode=tap?code:-1;lastTapTime=time;
    }
    void chord(){lastTapCode=-1;for(Press press:pressed.values())press.used=true;}
    void reset(){
        for(int code:new HashSet<>(active)){active.remove(code);output.key(code,false,meta());}
        locked.clear();pressed.clear();lastTapCode=-1;
    }
}
