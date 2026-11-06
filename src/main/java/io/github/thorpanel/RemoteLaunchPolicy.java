// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.util.HashSet;
import java.util.Set;

/** Trigger once per upper-screen foreground entry, independent of lower-screen navigation. */
final class RemoteLaunchPolicy {
    private Set<String> packages=new HashSet<>();
    private String previous;
    void configure(Set<String> selected){
        Set<String> next=new HashSet<>(selected);
        if(previous!=null&&!packages.contains(previous)&&next.contains(previous))previous=null;
        packages=next;
    }
    boolean enabled(){return !packages.isEmpty();}
    String sample(String upperPackage,boolean canLaunch){
        // Missing metadata or an unavailable lower screen is not a foreground transition.
        if(upperPackage==null||!canLaunch)return null;
        if(upperPackage.equals(previous))return null;
        previous=upperPackage;
        return packages.contains(upperPackage)?upperPackage:null;
    }
}
