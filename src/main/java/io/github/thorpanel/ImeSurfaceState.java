// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.util.regex.Pattern;

/** Read composed output, not WM's cached visibility or off-screen layer records. */
final class ImeSurfaceState {
    static boolean visible(String dump,int display){
        if(dump==null)return false;
        for(String output:dump.split("(?m)^Display ")){
            if(!output.contains("Composition Display State:")
                ||!output.contains("layerFilter={layerStack="+display+" "))continue;
            for(String layer:output.split(" - Output Layer ")){
                int end=layer.indexOf('\n');if(end<0)continue;
                String name=layer.substring(0,end);
                if(!Pattern.compile("\\(InputMethod#\\d+\\)").matcher(name).find())continue;
                if(Pattern.compile("Region visibleRegion[^\\n]*count=[1-9]\\d*\\)").matcher(layer).find())return true;
            }
        }
        return false;
    }
}
