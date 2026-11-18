// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT

package io.github.thorpanel;

public final class PanelRegionPolicyTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < .01f, message + ": " + actual + " != " + expected);
    }

    private static PanelRegionPolicy.Hinge horizontal(float top, float bottom) {
        return new PanelRegionPolicy.Hinge(new PanelLayout.Rect(0, top, 840, bottom),
                PanelRegionPolicy.Orientation.HORIZONTAL, true, true, bottom > top);
    }

    private static void bounds(PanelRegionPolicy.Region region, float left, float top, float right, float bottom) {
        near(region.bounds.left, left, "region left");
        near(region.bounds.top, top, "region top");
        near(region.bounds.right, right, "region right");
        near(region.bounds.bottom, bottom, "region bottom");
    }

    public static void main(String[] args) {
        PanelRegionPolicy.Region region = PanelRegionPolicy.panel(840, 900, null, 320, 180);
        check(region.usable && region.reason == PanelRegionPolicy.Reason.FULL_WINDOW, "no hinge keeps viewport layout");
        bounds(region, 0, 0, 840, 900);
        region = PanelRegionPolicy.panel(840, 900, horizontal(440,460), 320, 180);
        check(region.usable && region.reason == PanelRegionPolicy.Reason.BELOW_HINGE, "horizontal hinge uses lower area");
        bounds(region, 0, 460, 840, 900);
        region = PanelRegionPolicy.panel(840, 900, horizontal(450,450), 320, 180);
        check(region.usable && region.reason == PanelRegionPolicy.Reason.BELOW_HINGE, "zero-thickness fold is not an empty observation");
        bounds(region, 0, 450, 840, 900);
        for (PanelRegionPolicy.Hinge outside : new PanelRegionPolicy.Hinge[]{
                horizontal(-20,0), horizontal(900,920), horizontal(0,0), horizontal(900,900),
                new PanelRegionPolicy.Hinge(new PanelLayout.Rect(900,440,1000,460),
                    PanelRegionPolicy.Orientation.HORIZONTAL,true,true,true)}) {
            region = PanelRegionPolicy.panel(840,900,outside,320,180);
            check(region.usable && region.reason == PanelRegionPolicy.Reason.FULL_WINDOW,
                    "already split or out-of-window hinge uses local viewport");
            bounds(region,0,0,840,900);
        }
        PanelRegionPolicy.Hinge flat = new PanelRegionPolicy.Hinge(new PanelLayout.Rect(0,450,840,450),
                PanelRegionPolicy.Orientation.HORIZONTAL,false,false,false);
        region = PanelRegionPolicy.panel(840,900,flat,320,180);
        check(region.usable && region.reason == PanelRegionPolicy.Reason.FULL_WINDOW, "flat nonseparating crease retains area");
        PanelRegionPolicy.Hinge occluding = new PanelRegionPolicy.Hinge(new PanelLayout.Rect(0,440,840,460),
                PanelRegionPolicy.Orientation.HORIZONTAL,false,false,true);
        region = PanelRegionPolicy.panel(840,900,occluding,320,180);
        bounds(region,0,460,840,900);
        for (float thickness : new float[]{0,20}) {
            PanelRegionPolicy.Hinge vertical = new PanelRegionPolicy.Hinge(new PanelLayout.Rect(410,0,410+thickness,900),
                    PanelRegionPolicy.Orientation.VERTICAL,true,false,thickness>0);
            region = PanelRegionPolicy.panel(840,900,vertical,320,180);
            check(!region.usable && region.reason == PanelRegionPolicy.Reason.VERTICAL_HINGE,
                    "vertical hinge requires side-aware layout instead of lower-half geometry");
            bounds(region,0,0,840,900);
            check(!PanelRegionPolicy.floating(840,900,300,true,vertical,320,180).usable,
                    "floating panel cannot cross vertical separator");
        }
        region = PanelRegionPolicy.panel(840,900,horizontal(800,820),320,180);
        check(!region.usable && region.reason == PanelRegionPolicy.Reason.TOO_SMALL, "insufficient lower area pauses input");
        region = PanelRegionPolicy.panel(840,900,horizontal(880,940),320,1);
        check(!region.usable && region.bounds.height()==0, "hinge crossing bottom leaves no input area");
        region = PanelRegionPolicy.floating(1280,800,300,true,null,320,180);
        check(region.usable && region.reason == PanelRegionPolicy.Reason.BOTTOM_DOCK, "normal floating panel docks at bottom");
        bounds(region,0,500,1280,800);
        PanelRegionPolicy.Hinge tabletop = new PanelRegionPolicy.Hinge(new PanelLayout.Rect(0,390,1280,410),
                PanelRegionPolicy.Orientation.HORIZONTAL,true,true,true);
        region = PanelRegionPolicy.floating(1280,800,600,true,tabletop,320,180);
        check(region.usable && region.reason == PanelRegionPolicy.Reason.TABLETOP_DOCK, "landscape tabletop docks below hinge");
        bounds(region,0,410,1280,800);
        region = PanelRegionPolicy.floating(1280,800,200,true,tabletop,320,180);
        bounds(region,0,600,1280,800);
        region = PanelRegionPolicy.floating(1280,800,40,true,tabletop,320,180);
        bounds(region,0,620,1280,800);
        check(!PanelRegionPolicy.floating(840,900,200,true,horizontal(800,820),320,180).usable,
                "floating panel pauses when lower area is too short");
        check(!PanelRegionPolicy.panel(0,900,null,320,180).usable, "zero viewport is unavailable");
        check(!PanelRegionPolicy.panel(Float.NaN,900,null,320,180).usable, "nonfinite viewport is unavailable");
        check(!PanelRegionPolicy.panel(840,900,null,Float.NaN,180).usable, "invalid minimum is unavailable");
        check(!PanelRegionPolicy.floating(840,900,Float.POSITIVE_INFINITY,true,null,320,180).usable,
                "invalid requested height is unavailable");
        region = PanelRegionPolicy.panel(840,900,new PanelRegionPolicy.Hinge(null,
                PanelRegionPolicy.Orientation.HORIZONTAL,true,false,false),320,180);
        check(!region.usable && region.reason == PanelRegionPolicy.Reason.INVALID_HINGE, "malformed active observation pauses input");

        PanelRegionPolicy.Route route = PanelRegionPolicy.displayRoute(0,new int[]{2,4},new DisplayTopology(0,4));
        check(route.source == PanelRegionPolicy.RouteSource.THOR && route.remoteId==0 && route.panelId==4,
                "explicit Thor preference preserves upper and lower displays");
        route = PanelRegionPolicy.displayRoute(0,new int[]{-1,0,2,2,4},null);
        check(route.source == PanelRegionPolicy.RouteSource.EXTERNAL && route.remoteId==2 && route.panelId==0,
                "external route puts remote on external and panel on local");
        check(route.dualScreen && route.topology.upperId==2 && route.topology.lowerId==0, "external logical topology is explicit");
        route = PanelRegionPolicy.displayRoute(7,new int[]{9},null);
        check(route.remoteId==9 && route.panelId==7, "external route does not assume local id zero");
        route = PanelRegionPolicy.displayRoute(0,new int[]{2},new DisplayTopology(0,-1));
        check(route.source == PanelRegionPolicy.RouteSource.EXTERNAL, "incomplete Thor preference cannot hide external route");
        route = PanelRegionPolicy.displayRoute(0,new int[]{-1,0},null);
        check(route.source == PanelRegionPolicy.RouteSource.SINGLE && !route.dualScreen
                && route.remoteId==0 && route.panelId==0 && !route.topology.hasLower(), "single display has no artificial lower display");
        route = PanelRegionPolicy.displayRoute(-1,new int[]{2},null);
        check(route.source == PanelRegionPolicy.RouteSource.UNAVAILABLE && route.remoteId==-1 && route.panelId==-1,
                "missing local display cannot host panel");
        System.out.println("Passed " + checks + " panel region policy checks");
    }
}
