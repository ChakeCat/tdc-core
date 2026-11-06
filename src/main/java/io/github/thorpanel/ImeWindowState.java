// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

/** Some dual-screen IMEs draw a Presentation instead of a standard IME surface. */
final class ImeWindowState {
    static boolean hasLowerKeys(String dump,String imeId){
        if(dump==null||imeId==null||!imeId.contains("/"))return false;
        String owner="package="+imeId.substring(0,imeId.indexOf('/'))+" ";
        for(String window:dump.split("\n  Window #")){
            if(!window.contains("mDisplayId=4 ")||!window.contains(owner)||!window.contains("isVisible=true")
                ||!window.contains("mDrawState=HAS_DRAWN")||!window.contains("Surface: shown=true"))continue;
            if(window.contains("ty=PRESENTATION "))return true;
            if(window.contains("ty=APPLICATION_ATTACHED_DIALOG ")&&window.contains("mParentWindow=Window{")&&window.contains(" u0 InputMethod}"))return true;
            if(!window.contains("ty=INPUT_METHOD "))continue;
            java.util.regex.Matcher height=java.util.regex.Pattern.compile("Requested w=\\d+ h=(\\d+)").matcher(window);
            java.util.regex.Matcher top=java.util.regex.Pattern.compile("mGivenContentInsets=\\[0,(\\d+)\\]").matcher(window);
            // AYN leaves a navigation strip when the floating keys failed to
            // open. It occupies ~10% of the screen, not a usable keyboard.
            if(height.find()&&top.find()&&Integer.parseInt(top.group(1))<Integer.parseInt(height.group(1))*.8)return true;
        }
        return false;
    }
    static boolean hasLowerPresentation(String dump,String imeId){
        if(imeId==null||!imeId.contains("/"))return false;
        String owner="package="+imeId.substring(0,imeId.indexOf('/'))+" ";
        for(String window:dump.split("\\n  Window #"))
            if(window.contains("mDisplayId=4 ")&&window.contains(owner)
                &&window.contains("ty=PRESENTATION ")&&window.contains("isVisible=true")
                &&window.contains("mDrawState=HAS_DRAWN")&&window.contains("Surface: shown=true"))return true;
        return false;
    }
}
