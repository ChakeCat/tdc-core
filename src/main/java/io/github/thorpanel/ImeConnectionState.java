// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.util.regex.Pattern;

/** Verify the current editor, not a visible but disconnected or unrelated keyboard. */
final class ImeConnectionState {
    static int editorDisplay(String dump){
        if(dump==null)return -1;
        java.util.regex.Matcher match=Pattern.compile("^  mCurClient=ClientState\\{[^\\r\\n}]* displayId=(\\d+)\\}",Pattern.MULTILINE).matcher(dump);
        return match.find()?Integer.parseInt(match.group(1)):-1;
    }
    static boolean ready(String dump,String imeId,String editorPackage,int editorDisplay,int keyboardDisplay){
        if(dump==null||imeId==null||editorPackage==null)return false;
        int serviceStart=dump.indexOf("\nInput method service state for ");
        if(serviceStart<0)return false;
        String server=dump.substring(0,serviceStart),service=dump.substring(serviceStart);
        if(!matches(server,"^  mCurId="+Pattern.quote(imeId)+" mHaveConnection=true mBoundToMethod=true(?: |$)"))return false;
        if(!matches(server,"^  mCurClient=ClientState\\{[^\\r\\n}]* displayId="+editorDisplay+"\\}"))return false;
        if(!matches(server,"^  mCurTokenDisplayId="+keyboardDisplay+"\\s*$"))return false;
        return matches(service,"^  mInputStarted=true(?: |$)")
            &&matches(service,"^  mStartedInputConnection=(?!null(?:\\s|$))\\S+")
            &&matches(service,"^    packageName="+Pattern.quote(editorPackage)+"(?: |$)");
    }
    private static boolean matches(String value,String pattern){return Pattern.compile(pattern,Pattern.MULTILINE).matcher(value).find();}
}
