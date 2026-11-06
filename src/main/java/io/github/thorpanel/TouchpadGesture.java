// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks only fingers on the pad; fingers holding the separate mouse buttons are excluded. */
final class TouchpadGesture {
    static final int DEFAULT_HOLD_MS=100,MIN_HOLD_MS=50,MAX_HOLD_MS=1000,HOLD_STEP_MS=10;
    static int clampHoldMs(int value){return Math.max(MIN_HOLD_MS,Math.min(MAX_HOLD_MS,value));}
    static final float RIGHT_RADIUS=100;
    static boolean inRightCorner(float x,float y,float right,float bottom){
        float dx=right-x,dy=bottom-y;
        return dx>0&&dy>0&&dx*dx+dy*dy<=RIGHT_RADIUS*RIGHT_RADIUS;
    }
    static int clickButton(boolean cornerMode,float x,float y,float right,float bottom){
        return cornerMode&&inRightCorner(x,y,right,bottom)?2:1;
    }
    interface Output {
        void move(float dx,float dy);void scroll(float dx,float dy);void button(int button,boolean down);
        void dragging(boolean active);
    }
    private static final class Point {float x,y;Point(float x,float y){this.x=x;this.y=y;}}
    private final Map<Integer,Point> fingers=new LinkedHashMap<>();
    private final Output output;
    private final float slop;
    private int primary,maxFingers,tapButton;
    private float startX,startY,lastX,lastY,lastAverageX,lastAverageY,startAverageX,startAverageY;
    private long started,token;
    private int configuredHoldMs=DEFAULT_HOLD_MS,activeHoldMs=DEFAULT_HOLD_MS;
    private boolean moved,drag;
    TouchpadGesture(float slop,Output output){this.slop=slop;this.output=output;}
    void setHoldDelayMs(int value){configuredHoldMs=clampHoldMs(value);}
    /** Scheduling and recognition share the delay captured at the start of this touch. */
    int holdDelayMs(){return activeHoldMs;}
    long token(){return token;}
    int count(){return fingers.size();}
    boolean isDragging(){return drag;}
    boolean contains(int id){return fingers.containsKey(id);}
    void down(int id,float x,float y,long now,int button){
        if(fingers.isEmpty()){
            token++;primary=id;started=now;activeHoldMs=configuredHoldMs;startX=lastX=x;startY=lastY=y;
            maxFingers=1;moved=false;tapButton=button;
        }
        fingers.put(id,new Point(x,y));
        if(fingers.size()>1){
            maxFingers=Math.max(maxFingers,fingers.size());
            if(drag){stopDrag();moved=true;}
            startAverageX=lastAverageX=averageX();startAverageY=lastAverageY=averageY();
        }
    }
    void update(int id,float x,float y){Point p=fingers.get(id);if(p!=null){p.x=x;p.y=y;}}
    void finishMove(){
        if(fingers.size()>=2){
            float x=averageX(),y=averageY();
            if(Math.hypot(x-startAverageX,y-startAverageY)>slop)moved=true;
            // Android: positive horizontal = right; negative vertical = down.
            if(moved)output.scroll((x-lastAverageX)/45f,-(y-lastAverageY)/45f);
            lastAverageX=x;lastAverageY=y;
        }else if(maxFingers==1){
            Point p=fingers.get(primary);if(p==null)return;
            if(Math.hypot(p.x-startX,p.y-startY)>slop)moved=true;
            if(moved||drag)output.move(p.x-lastX,p.y-lastY);
            lastX=p.x;lastY=p.y;
        }
    }
    void hold(long expectedToken,long now){
        if(token!=expectedToken||fingers.size()!=1||maxFingers!=1||moved||drag||now-started<activeHoldMs)return;
        drag=true;output.button(1,true);output.dragging(true);
    }
    void up(int id,long now){
        if(fingers.remove(id)==null)return;
        if(!fingers.isEmpty()){lastAverageX=averageX();lastAverageY=averageY();return;}
        if(drag)stopDrag();
        else if(!moved&&now-started<300){int button=maxFingers>=2?2:tapButton;output.button(button,true);output.button(button,false);}
        token++;
    }
    void cancel(){stopDrag();fingers.clear();token++;moved=false;}
    private void stopDrag(){if(drag){drag=false;output.button(1,false);output.dragging(false);}}
    private float averageX(){float x=0;for(Point p:fingers.values())x+=p.x;return x/fingers.size();}
    private float averageY(){float y=0;for(Point p:fingers.values())y+=p.y;return y/fingers.size();}
}
