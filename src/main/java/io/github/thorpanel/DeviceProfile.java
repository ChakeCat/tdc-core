// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/** Device form derived from topology and capability probes, not a model whitelist. */
public final class DeviceProfile {
    public enum Form { THOR_DUAL, GENERIC_DUAL, EXTERNAL, SINGLE, UNKNOWN }

    public final Form form;
    public final DisplayTopology topology;
    public final boolean apiSupported;
    public final boolean uhidLikely;
    public final boolean aynImeSettings;

    public DeviceProfile(Form form,DisplayTopology topology,boolean apiSupported,boolean uhidLikely,boolean aynImeSettings){
        this.form=form;this.topology=topology;this.apiSupported=apiSupported;this.uhidLikely=uhidLikely;this.aynImeSettings=aynImeSettings;
    }

    public static DeviceProfile of(DisplayTopology topology,boolean apiSupported,boolean prefersThorLower,boolean uhidLikely,boolean aynImeSettings){
        return of(topology,apiSupported,prefersThorLower,false,uhidLikely,aynImeSettings);
    }

    public static DeviceProfile of(DisplayTopology topology,boolean apiSupported,boolean trustedThor,
            boolean external,boolean uhidLikely,boolean aynImeSettings){
        Form form=Form.UNKNOWN;
        if(topology!=null&&topology.dualScreen&&external)form=Form.EXTERNAL;
        else if(topology!=null&&topology.dualScreen&&trustedThor)form=Form.THOR_DUAL;
        else if(topology!=null&&!topology.dualScreen&&topology.hasUpper())form=Form.SINGLE;
        else if(topology!=null&&topology.dualScreen)form=Form.GENERIC_DUAL;
        return new DeviceProfile(form,topology==null?new DisplayTopology(DisplayTopology.NONE,DisplayTopology.NONE):topology,
            apiSupported,uhidLikely,form==Form.THOR_DUAL&&aynImeSettings);
    }

    public boolean panelOnLower(){return (form==Form.THOR_DUAL||form==Form.GENERIC_DUAL)&&topology.hasLower();}
    public boolean canUseThorPanel(){return apiSupported&&panelOnLower();}
}
