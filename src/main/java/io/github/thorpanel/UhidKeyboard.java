// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/** Standard USB HID keyboard reports, seen by UU as an external keyboard. */
final class UhidKeyboard implements AutoCloseable {
    private final FileOutputStream out;
    private final Set<Integer> keys=new LinkedHashSet<>();
    private int modifiers;
    private static final byte[] DESCRIPTOR={
        0x05,0x01,0x09,0x06,(byte)0xa1,0x01,0x05,0x07,0x19,(byte)0xe0,0x29,(byte)0xe7,
        0x15,0x00,0x25,0x01,0x75,0x01,(byte)0x95,0x08,(byte)0x81,0x02,
        (byte)0x95,0x01,0x75,0x08,(byte)0x81,0x01,
        (byte)0x95,0x06,0x75,0x08,0x15,0x00,0x25,0x65,0x05,0x07,0x19,0x00,0x29,0x65,(byte)0x81,0x00,(byte)0xc0
    };
    UhidKeyboard() throws IOException {
        out=new FileOutputStream("/dev/uhid");
        ByteBuffer b=ByteBuffer.allocate(280+DESCRIPTOR.length).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(11);b.put("Thor Panel Keyboard".getBytes(StandardCharsets.UTF_8));
        b.position(260);b.putShort((short)DESCRIPTOR.length);b.putShort((short)3);
        b.putInt(0x1209);b.putInt(0x5451);b.putInt(1);b.putInt(0);b.put(DESCRIPTOR);out.write(b.array());
    }
    private static int modifier(int code){
        switch(code){case 113:return 1;case 59:return 2;case 57:return 4;case 117:return 8;default:return 0;}
    }
    private static int usage(int code){
        if(code>=29&&code<=54)return code-29+4;
        if(code>=8&&code<=16)return code-8+30;
        if(code>=131&&code<=142)return code-131+58;
        switch(code){
            case 7:return 39;case 66:return 40;case 111:return 41;case 67:return 42;case 61:return 43;
            case 62:return 44;case 69:return 45;case 70:return 46;case 71:return 47;case 72:return 48;
            case 73:return 49;case 74:return 51;case 75:return 52;case 68:return 53;case 55:return 54;
            case 56:return 55;case 76:return 56;case 115:return 57;case 122:return 74;case 112:return 76;
            case 123:return 77;case 22:return 79;case 21:return 80;case 20:return 81;case 19:return 82;
            default:return 0;
        }
    }
    synchronized void key(int code,int action) throws IOException {
        int bit=modifier(code);
        if(bit!=0)modifiers=action==0?modifiers|bit:modifiers&~bit;
        else{int usage=usage(code);if(usage==0)return;if(action==0)keys.add(usage);else keys.remove(usage);}
        report();
    }
    private void report() throws IOException {
        ByteBuffer b=ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(12);b.putShort((short)8);b.put((byte)modifiers);b.put((byte)0);
        int n=0;for(int key:keys){if(n++==6)break;b.put((byte)key);}out.write(b.array());
    }
    synchronized void release() throws IOException {keys.clear();modifiers=0;report();}
    @Override public void close() throws IOException {
        try{release();out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(1).array());}finally{out.close();}
    }
}
