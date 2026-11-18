// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT
package io.github.thorpanel;

public final class DisplayTopologyTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}

    public static void main(String[] args){
        DisplayTopology empty=DisplayTopology.fromIds(new int[0],null,null,4);
        check(empty.upperId==DisplayTopology.NONE&&empty.lowerId==DisplayTopology.NONE&&!empty.dualScreen,"empty");

        DisplayTopology thor=DisplayTopology.fromIds(new int[]{0,4},new boolean[]{true,true},new boolean[]{true,true},4);
        check(thor.upperId==0&&thor.lowerId==4&&thor.dualScreen,"thor 0+4");

        DisplayTopology prefer=DisplayTopology.fromIds(new int[]{0,2,4},new boolean[]{true,true,true},new boolean[]{true,true,true},4);
        check(prefer.lowerId==4,"prefer known lower over earlier second screen");

        DisplayTopology other=DisplayTopology.fromIds(new int[]{0,2},new boolean[]{true,true},new boolean[]{true,true},4);
        check(other.lowerId==2,"fallback to any second screen");

        DisplayTopology off=DisplayTopology.fromIds(new int[]{0,4},new boolean[]{true,true},new boolean[]{true,false},4);
        check(off.lowerId==DisplayTopology.NONE&&!off.dualScreen,"off lower ignored");

        DisplayTopology invalid=DisplayTopology.fromIds(new int[]{0,4},new boolean[]{true,false},new boolean[]{true,true},4);
        check(invalid.lowerId==DisplayTopology.NONE,"invalid lower ignored");

        DisplayTopology single=DisplayTopology.fromIds(new int[]{0},new boolean[]{true},new boolean[]{true},4);
        check(single.upperId==0&&!single.hasLower(),"single screen");

        DisplayTopology noDefault=DisplayTopology.fromIds(new int[]{4,2},null,null,4);
        check(noDefault.upperId==2&&noDefault.lowerId==4&&noDefault.dualScreen,"non-default upper preserves the preferred lower");
        DisplayTopology offDefault=DisplayTopology.fromIds(new int[]{0,2,4},null,new boolean[]{false,true,true},4);
        check(offDefault.upperId==2&&offDefault.lowerId==4,"off default cannot become the active upper");
        DisplayTopology allOff=DisplayTopology.fromIds(new int[]{0,4},null,new boolean[]{false,false},4);
        check(!allOff.hasUpper()&&!allOff.hasLower(),"all screens off yields no usable topology");
        DisplayTopology partial=DisplayTopology.fromIds(new int[]{0,4},new boolean[]{true},null,4);
        check(partial.upperId==0&&!partial.hasLower(),"missing availability data is not assumed valid");
        DisplayTopology external=DisplayTopology.fromIds(new int[]{2,0},null,null,4);
        check(external.upperId==0&&external.lowerId==2,"external display ordering does not replace the default upper");

        DeviceProfile thorProfile=DeviceProfile.of(thor,true,true,true,true);
        check(thorProfile.form==DeviceProfile.Form.THOR_DUAL&&thorProfile.canUseThorPanel(),"thor profile");
        DeviceProfile phone=DeviceProfile.of(single,true,false,false,false);
        check(phone.form==DeviceProfile.Form.SINGLE&&!phone.canUseThorPanel(),"single profile");
        DeviceProfile oldApi=DeviceProfile.of(thor,false,true,true,true);
        check(!oldApi.canUseThorPanel(),"api gate");
        DeviceProfile generic=DeviceProfile.of(thor,true,false,true,true);
        check(generic.form==DeviceProfile.Form.GENERIC_DUAL&&generic.canUseThorPanel(),"display 4 alone does not identify a Thor");
        check(!generic.aynImeSettings,"generic dual screens cannot inherit AYN input settings");
        DeviceProfile genericTwo=DeviceProfile.of(other,true,false,true,false);
        check(genericTwo.form==DeviceProfile.Form.GENERIC_DUAL,"generic display 2 keeps the dual-panel path");
        DeviceProfile externalProfile=DeviceProfile.of(new DisplayTopology(2,0),true,false,true,true,true);
        check(externalProfile.form==DeviceProfile.Form.EXTERNAL&&!externalProfile.canUseThorPanel(),"external route uses the experimental host instead of Thor input");
        check(!externalProfile.aynImeSettings&&externalProfile.topology.upperId==2&&externalProfile.topology.lowerId==0,
            "external profile retains route orientation without AYN settings");

        System.out.println("Passed "+checks+" DisplayTopology / DeviceProfile checks");
    }
}
