// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Linux UHID CREATE2/INPUT2: real relative mouse, available to Android's shell UID. */
final class UhidMouse implements AutoCloseable {
    private FileOutputStream out;
    private int buttons;
    private float remainderX,remainderY,remainderWheel,remainderPan;
    private static final byte[] DESCRIPTOR = new byte[] {
        0x05,0x01,0x09,0x02,(byte)0xa1,0x01,0x09,0x01,(byte)0xa1,0x00,
        0x05,0x09,0x19,0x01,0x29,0x03,0x15,0x00,0x25,0x01,0x75,0x01,(byte)0x95,0x03,(byte)0x81,0x02,
        0x75,0x05,(byte)0x95,0x01,(byte)0x81,0x03,0x05,0x01,0x09,0x30,0x09,0x31,0x09,0x38,
        0x15,(byte)0x81,0x25,0x7f,0x75,0x08,(byte)0x95,0x03,(byte)0x81,0x06,
        0x05,0x0c,0x0a,0x38,0x02,(byte)0x95,0x01,(byte)0x81,0x06, // Consumer AC Pan -> REL_HWHEEL
        (byte)0xc0,(byte)0xc0
    };
    UhidMouse() throws IOException {
        out=new FileOutputStream("/dev/uhid");
        ByteBuffer b=ByteBuffer.allocate(280+DESCRIPTOR.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(11); byte[] name="Thor Panel Mouse".getBytes(StandardCharsets.UTF_8); b.put(name);
        b.position(4+128+64+64); b.putShort((short)DESCRIPTOR.length); b.putShort((short)3);
        b.putInt(0x1209); b.putInt(0x5450); b.putInt(1); b.putInt(0); b.put(DESCRIPTOR);
        out.write(b.array());
    }
    private void report(int dx,int dy,int wheel,int pan) throws IOException {
        ByteBuffer b=ByteBuffer.allocate(11).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(12); b.putShort((short)5); b.put((byte)buttons); b.put((byte)dx);b.put((byte)dy);b.put((byte)wheel);b.put((byte)pan);
        out.write(b.array());
    }
    synchronized void move(float x,float y) throws IOException {
        remainderX+=x; remainderY+=y;
        int dx=(int)remainderX,dy=(int)remainderY; remainderX-=dx;remainderY-=dy;
        while(dx!=0 || dy!=0) {
            int sx=Math.max(-127,Math.min(127,dx)),sy=Math.max(-127,Math.min(127,dy));
            report(sx,sy,0,0);dx-=sx;dy-=sy;
        }
    }
    synchronized void button(int mask,boolean down) throws IOException {
        if ((mask & ~7)!=0) throw new IllegalArgumentException("Invalid button");
        buttons=down?buttons|mask:buttons&~mask;report(0,0,0,0);
    }
    synchronized void scroll(float horizontal,float vertical) throws IOException {
        remainderWheel+=vertical;remainderPan+=horizontal;
        int wheel=(int)remainderWheel,pan=(int)remainderPan;remainderWheel-=wheel;remainderPan-=pan;
        while(wheel!=0||pan!=0) {
            int v=Math.max(-127,Math.min(127,wheel)),h=Math.max(-127,Math.min(127,pan));
            report(0,0,v,h);wheel-=v;pan-=h;
        }
    }
    synchronized void release() throws IOException { buttons=0;remainderX=remainderY=remainderWheel=remainderPan=0;report(0,0,0,0); }
    @Override public synchronized void close() throws IOException {
        if(out==null)return;
        try {release();ByteBuffer b=ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1);out.write(b.array());}
        finally {out.close();out=null;}
    }
}
