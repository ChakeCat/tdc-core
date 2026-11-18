// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT
package io.github.thorpanel;

public final class WindowTaskStateTest {
    private static int checks;
    private static final String SAMPLE = "ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)\n"
            + "Display #0 (activities from top to bottom):\n"
            + "  * Task{root #1 type=undefined visible=true mode=fullscreen}\n"
            + "    * Task{main #2 type=undefined rootTaskId=1 visible=true mode=multi-window}\n"
            + "      * Task{remote #20 type=standard rootTaskId=1 visible=true mode=multi-window}\n"
            + "        * Hist #0: ActivityRecord{a u0 sample.remote/.Main} t20}\n"
            + "          CurrentConfiguration={winConfig={ mBounds=Rect(0, 0 - 800, 632) mWindowingMode=multi-window}}\n"
            + "    * Task{side #3 type=undefined rootTaskId=1 visible=true mode=multi-window}\n"
            + "      * Task{panel #30 type=standard rootTaskId=1 visible=true mode=multi-window}\n"
            + "        * Hist #0: ActivityRecord{b u0 sample.panel/.Panel} t30}\n"
            + "          CurrentConfiguration={winConfig={ mBounds=Rect(0, 648 - 800, 1280) mWindowingMode=multi-window}}\n";

    private static void check(boolean expected, String text) {
        checks++;
        boolean actual = WindowTaskState.parse(text).pairedAbove("sample.remote", 30, "sample.panel");
        if (expected != actual) throw new AssertionError("Pair result: " + expected + " != " + actual);
    }

    public static void main(String[] args) {
        check(true, SAMPLE);
        check(false, SAMPLE.replace("panel #30", "panel #31"));
        check(false, SAMPLE.replace("sample.remote", "sample.remote.other"));
        check(false, SAMPLE.replace("sample.panel", "sample.panel.other"));
        check(false, SAMPLE.replace("rootTaskId=1 visible=true mode=multi-window}\n        * Hist #0: ActivityRecord{b", "rootTaskId=99 visible=true mode=multi-window}\n        * Hist #0: ActivityRecord{b"));
        check(false, SAMPLE.replace("side #3", "side #2"));
        check(false, SAMPLE.replace("visible=true", "visible=false"));
        check(false, SAMPLE.replace("Rect(0, 648 - 800, 1280)", "Rect(0, 600 - 800, 1280)"));
        check(false, SAMPLE.replace("Rect(0, 648 - 800, 1280)", "Rect(0, 900 - 800, 1280)"));
        check(false, SAMPLE.replace("Rect(0, 648 - 800, 1280)", "Rect(20, 648 - 800, 1280)"));
        check(false, SAMPLE.replace("Rect(0, 648 - 800, 1280)", "Rect(-1, 648 - 800, 1280)"));
        check(false, SAMPLE.replace("mWindowingMode=multi-window", "mWindowingMode=fullscreen"));
        check(false, SAMPLE.replace("CurrentConfiguration=", "RequestedOverrideConfiguration="));
        check(true, SAMPLE + "ActivityTaskSupervisor state:\n      * Task{duplicate #20 visible=true mode=fullscreen}\n");
        String letterboxed = SAMPLE.replace("        * Hist #0: ActivityRecord{a", "        mBounds=Rect(0, 0 - 800, 632)\n        * Hist #0: ActivityRecord{a")
                .replace("CurrentConfiguration={winConfig={ mBounds=Rect(0, 0 - 800, 632)", "CurrentConfiguration={winConfig={ mBounds=Rect(100, 0 - 700, 632)");
        check(true, letterboxed);
        String independent = SAMPLE.replace("rootTaskId=1", "rootTaskId=20").replace("side #3", "side #3");
        independent = independent.replace("panel #30 type=standard rootTaskId=20", "panel #30 type=standard rootTaskId=30");
        check(false, independent);
        System.out.println("Window task state: " + checks + " checks passed");
    }
}
