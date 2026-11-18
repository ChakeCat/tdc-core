// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: MIT

package io.github.thorpanel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PanelLayoutTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < .01f, message + ": " + actual + " != " + expected);
    }

    private static boolean overlaps(PanelLayout.Rect a, PanelLayout.Rect b) {
        return a.left < b.right - .01f && a.right > b.left + .01f
                && a.top < b.bottom - .01f && a.bottom > b.top + .01f;
    }

    private static void add(List<PanelLayout.Rect> targets, PanelLayout.Rect target) {
        if (!target.isEmpty()) targets.add(target);
    }

    private static void geometry(PanelLayout layout) {
        check(layout.textSp >= 12 && layout.keyTextSp >= 12 && layout.modeTextSp >= 12, "minimum font size");
        List<PanelLayout.Rect> targets = new ArrayList<>();
        add(targets, layout.settings);
        add(targets, layout.exit);
        add(targets, layout.option);
        for (PanelLayout.Rect mode : layout.modes) add(targets, mode);
        add(targets, layout.leftButton);
        add(targets, layout.rightButton);
        add(targets, layout.speedButton);
        add(targets, layout.imeButton);
        for (PanelLayout.Rect tab : layout.layerTabs) add(targets, tab);
        add(targets, layout.pageBack);
        add(targets, layout.pageNext);
        add(targets, layout.pageIndicator);
        for (PanelLayout.Key key : layout.keys) add(targets, key.bounds);
        for (PanelLayout.Rect target : targets) {
            check(target.width() >= PanelLayout.FULL_KEY_WIDTH - .01f
                    && target.height() >= PanelLayout.FULL_KEY_HEIGHT - .01f,
                    "minimum compact target " + layout.snapshot());
            check(target.left >= 0 && target.top >= 0 && target.right <= layout.width + .01f
                    && target.bottom <= layout.height + .01f, "target within viewport " + layout.snapshot());
            check(target.contains(target.centerX(), target.centerY()), "center hit");
            check(!target.contains(target.right, target.bottom), "exclusive right/bottom edges");
        }
        for (PanelLayout.Rect control : new PanelLayout.Rect[]{layout.settings, layout.exit,
                layout.option, layout.imeButton}) {
            if (!control.isEmpty()) check(control.height() >= PanelLayout.HEADER_TARGET - .01f,
                    "compact toolbar height");
        }
        for (PanelLayout.Rect mode : layout.modes)
            check(mode.height() >= PanelLayout.HEADER_TARGET - .01f, "mode tab height");
        for (PanelLayout.Key key : layout.keys) {
            if (layout.fullKeyboard) {
                check(key.bounds.width() + .01f >= key.label.length() * layout.keyTextSp * layout.fontScale * .64f + 8,
                        "full keyboard label fits: " + key.label);
                near(key.bounds.height(), layout.keys.get(0).bounds.height(), "full keyboard uses a uniform height");
                check(key.bounds.height() + .01f >= layout.keyTextSp * layout.fontScale * 1.4f + 12,
                        "full keyboard keeps room above and below its labels");
            } else {
                check(key.bounds.width() >= 47.99f && key.bounds.height() >= 47.99f,
                        "layered keyboard retains 48dp keys");
            }
        }
        if (layout.fullKeyboard) {
            PanelLayout.Rect previousRow = null;
            int rowCount = 0;
            for (PanelLayout.Key key : layout.keys) {
                if (previousRow != null && Math.abs(key.bounds.top - previousRow.top) < .01f)
                    continue;
                if (previousRow == null) {
                    near(key.bounds.top, layout.content.top, "keyboard starts at content top");
                } else {
                    float currentGap = key.bounds.top - previousRow.bottom;
                    near(currentGap, 4, "keyboard rows keep compact spacing");
                }
                previousRow = key.bounds;
                rowCount++;
            }
            check(rowCount == 6, "full keyboard keeps six rows");
            check(previousRow.bottom <= layout.content.bottom + .01f, "last keyboard row fits the content");
        }
        for (int i = 0; i < targets.size(); i++) {
            for (int j = i + 1; j < targets.size(); j++) {
                check(!overlaps(targets.get(i), targets.get(j)), "overlapping targets " + layout.snapshot());
            }
            if (!layout.pad.isEmpty()) check(!overlaps(targets.get(i), layout.pad), "button overlaps touchpad");
        }
        if (!layout.usable) check(layout.keys.isEmpty() && layout.pad.isEmpty(), "paused layout cannot accept input");
    }

    private static PanelLayout layout(float width, float height, float scale, int mode, int layer, int page) {
        return PanelLayout.create(width, height, scale, mode, false, false, layer, page);
    }

    private static Set<Integer> allKeys(float width, float height, float scale) {
        Set<Integer> result = new HashSet<>();
        for (int layer = 0; layer < 3; layer++) {
            PanelLayout first = layout(width, height, scale, 1, layer, 0);
            check(first.usable, "keyboard fits test window");
            for (int page = 0; page < first.pageCount; page++) {
                PanelLayout current = layout(width, height, scale, 1, layer, page);
                geometry(current);
                Set<Integer> perPage = new HashSet<>();
                for (PanelLayout.Key key : current.keys) {
                    check(perPage.add(key.code), "no duplicate keys on a page");
                    result.add(key.code);
                }
                check(perPage.contains(59) && perPage.contains(113) && perPage.contains(57)
                        && perPage.contains(117), "every page retains modifiers");
            }
        }
        return result;
    }

    private static void viewportEnvelope(PanelLayout value) {
        List<PanelLayout.Rect> regions = new ArrayList<>();
        for (PanelLayout.Rect region : new PanelLayout.Rect[]{value.title, value.status,
                value.settings, value.option, value.exit, value.content, value.pad,
                value.leftButton, value.rightButton, value.speedButton, value.imeButton,
                value.pageBack, value.pageNext, value.pageIndicator}) add(regions, region);
        for (PanelLayout.Rect region : value.modes) add(regions, region);
        for (PanelLayout.Rect region : value.layerTabs) add(regions, region);
        for (PanelLayout.Key key : value.keys) add(regions, key.bounds);
        for (PanelLayout.Rect region : regions) {
            check(Float.isFinite(region.left) && Float.isFinite(region.top)
                    && Float.isFinite(region.right) && Float.isFinite(region.bottom),
                    "responsive layout has finite drawing coordinates");
            check(region.left >= 0 && region.top >= 0 && region.right <= value.width + .01f
                    && region.bottom <= value.height + .01f,
                    "every drawn region remains inside the measured safe viewport");
        }
        if (!value.usable) check(value.keys.isEmpty() && value.pad.isEmpty(),
                "insufficient safe space does not expose partial input surfaces");
    }

    private static void responsiveViewports() {
        // Pixels are examples of measured windows, never device selectors. Insets can change
        // independently of the window and can temporarily cover its entire height.
        for (int[] pixels : new int[][]{{1280,960},{1920,1080},{1227,799},{800,600}}) {
            for (float density : new float[]{1,1.5f,2,369f/160f}) {
                for (int rotation : new int[]{0,1}) {
                    float width = (pixels[rotation] - 73 - 47) / density;
                    float height = (pixels[1-rotation] - 31 - 13) / density;
                    for (float fraction : new float[]{0,.25f,.5f,.8f,1,1.2f}) {
                        float available = Math.max(0,height*(1-fraction));
                        for (float font : new float[]{1,1.3f,2}) {
                            for (int mode=0; mode<3; mode++) {
                                PanelLayout value = PanelLayout.create(width,available,font,mode,
                                        false,fraction>0,PanelLayout.FUNCTIONS,999);
                                viewportEnvelope(value);
                                if (mode==2 && fraction>0) check(value.pad.isEmpty(),
                                        "visible input method owns the remaining text-entry area");
                            }
                        }
                    }
                }
            }
        }
        // Reflow boundaries must not introduce a one-pixel overflow at fractional dp sizes.
        for (float width : new float[]{319.5f,499.5f,500,599.5f,600,699.5f,700,839.5f,840})
            for (float height : new float[]{35.5f,95.5f,107.5f,131.5f,199.5f,279.5f,479.5f,640})
                for (int mode=0; mode<3; mode++)
                    viewportEnvelope(PanelLayout.create(width,height,1.5f,mode,false,true,0,0));
    }

    public static void main(String[] args) throws Exception {
        responsiveViewports();
        for (int width : new int[]{320, 360, 411, 600, 840}) {
            for (int height : new int[]{180, 280, 360, 480, 640, 900}) {
                for (float scale : new float[]{1, 1.3f, 1.5f, 2}) {
                    for (int mode = 0; mode < 3; mode++) {
                        for (int layer = 0; layer < 3; layer++) {
                            PanelLayout current = layout(width, height, scale, mode, layer, 0);
                            geometry(current);
                            if (mode != 1 && current.usable && !current.pad.isEmpty()) {
                                check(current.pad.height() >= 96 && current.pad.width() >= 48, "minimum touchpad size");
                            }
                        }
                    }
                }
            }
        }
        Set<Integer> expected = new HashSet<>();
        for (int code : new int[]{111,131,132,133,134,135,136,137,138,139,140,141,142,
                68,8,9,10,11,12,13,14,15,16,7,69,70,67,61,45,51,33,46,48,53,49,37,43,44,71,72,73,
                115,29,47,32,34,35,36,38,39,40,74,75,66,59,54,52,31,50,30,42,41,55,56,76,19,112,
                113,117,57,62,122,123,21,20,22}) expected.add(code);
        for (int width : new int[]{320,360,411,600,840}) {
            check(allKeys(width, 640, 1).equals(expected), "all original keys reachable at " + width);
            check(allKeys(width, 900, 1.5f).equals(expected), "all keys reachable with large text at " + width);
        }
        check(layout(320,640,1,1,0,0).variant == PanelLayout.Variant.NARROW, "narrow variant");
        check(layout(620,640,1,0,0,0).variant == PanelLayout.Variant.STANDARD, "standard variant");
        check(layout(840,360,1,0,0,0).variant == PanelLayout.Variant.WIDE_SHORT, "short variant");
        check(layout(840,900,1,1,0,0).variant == PanelLayout.Variant.LARGE, "large variant");
        check(layout(840,640,1,1,0,0).fullKeyboard, "expansion restores full keyboard");
        check(layout(600,640,1,1,0,0).fullKeyboard, "six-row keyboard fits medium windows");
        check(!layout(360,280,1,1,0,0).usable, "keyboard obstruction pauses keyboard");
        check(layout(840,280,1,0,0,0).usable, "wide short touchpad remains usable");
        check(layout(840,280,1,0,0,0).modes[0].top == layout(840,280,1,0,0,0).settings.top,
                "wide short mode tabs share header row");
        PanelLayout giant = layout(1600,1400,1,1,0,0);
        for (PanelLayout.Key key : giant.keys) check(key.bounds.height() <= 59.01f, "large screen key height stays compact");
        float min = Float.MAX_VALUE, max = 0;
        for (PanelLayout.Key key : giant.keys) {
            min = Math.min(min, key.bounds.left);
            max = Math.max(max, key.bounds.right);
        }
        check(max - min <= 1000.01f, "large screen keyboard width cap");
        PanelLayout last = layout(320,640,1,1,0,999);
        check(last.page == last.pageCount - 1, "late page index clamps after resize");
        check(layout(320,640,1,1,-10,-10).page == 0, "negative page clamps");
        check(!layout(Float.NaN,Float.POSITIVE_INFINITY,1,0,0,0).usable, "invalid dimensions pause input");

        // Recorded Thor lower display: 1240x1080 px, densityDpi 369, app height 1025 px.
        float density = 369f / 160;
        PanelLayout thor = layout(1240 / density, 1025 / density, 1, 1, 0, 0);
        check(thor.usable && thor.fullKeyboard, "Thor restores the full computer keyboard");
        near(thor.width, 537.6694f, "Thor width");
        near(thor.height, 444.4444f, "Thor height");
        near(thor.content.top, 96, "Thor compact header snapshot");
        check(thor.keys.get(0).bounds.height() >= 48 && thor.keys.get(0).bounds.height() <= 50,
                "Thor keys are slightly shorter than the original 53dp rectangles");
        check(thor.content.bottom - thor.keys.get(thor.keys.size() - 1).bounds.bottom < 30,
                "Thor compact keyboard leaves only a small bottom margin");
        check(thor.pageCount == 1 && thor.layerTabs.length == 0 && thor.pageBack.isEmpty()
                && thor.pageNext.isEmpty(), "full keyboard does not need layers or pages");
        check(thor.keys.size() == expected.size(), "all keys visible at once on Thor");
        check(layout(thor.width, thor.height, 1, 0, 0, 0).option.isEmpty(),
                "touchpad leaves the old corner toggle position empty");
        check(thor.option.width() < 160, "Thor option control uses a compact width");
        near(thor.option.right + 6, thor.settings.left, "Thor option control is right-aligned");
        near(thor.modes[0].left, 12, "Thor mode tabs start at the margin");
        near(thor.modes[2].right, thor.width - 12, "Thor mode tabs fill the row");
        for (int mode = 0; mode < 3; mode++) {
            PanelLayout tabs = layout(thor.width, thor.height, 1, mode, 0, 0);
            near(tabs.modes[2].right, thor.width - 12, "mode tabs fill the row in mode " + mode);
            near(tabs.modes[0].width(), tabs.modes[2].width(), "mode tabs share one width");
        }
        for (int width : new int[]{320, 411}) {
            PanelLayout narrowTabs = layout(width, 640, 1, 2, 0, 0);
            near(narrowTabs.modes[2].right, width - 12, "narrow mode tabs fill the row at " + width);
        }
        check(allKeys(thor.width, thor.height, 1).equals(expected), "Thor retains every key");
        PanelLayout thorLocal = PanelLayout.create(thor.width, thor.height, 1, 1, true, false, 0, 0);
        near(thorLocal.content.bottom, thor.content.bottom, "local keyboard uses measured host viewport");
        check(thorLocal.usable && thorLocal.fullKeyboard && thorLocal.keys.size() == expected.size(),
                "local Thor keyboard retains full layout");
        geometry(thorLocal);
        PanelLayout thorEditor = PanelLayout.create(thor.width, thor.height, 1, 2, false, false, 0, 0);
        PanelLayout thorIme = PanelLayout.create(thor.width, thor.height, 1, 2, false, true, 0, 0);
        geometry(thorEditor);
        check(thorEditor.status.width() >= 120, "Thor status has room for ten short Chinese characters");
        check(thorEditor.usable && !thorEditor.pad.isEmpty(), "Thor input page keeps the touchpad while the keyboard is closed");
        near(thorEditor.imeButton.right + 6, thorEditor.option.left, "open icon sits left of the IME selector");
        near(thorEditor.imeButton.width(), thorEditor.imeButton.height(), "keyboard toggle is a square icon");
        near(thorEditor.imeButton.left, thorIme.imeButton.left, "keyboard icon stays in place when opened");
        near(thorEditor.imeButton.top, thorIme.imeButton.top, "keyboard icon stays on its header row");
        near(thorEditor.pad.top, thorEditor.content.top, "touchpad uses the area below the mode tabs");
        check(thorIme.usable && thorIme.pad.isEmpty(), "open keyboard hides the Thor input touchpad");
        near(thorIme.imeButton.right + 6, thorIme.option.left, "close-keyboard action sits beside the IME selector");
        check(thorIme.imeButton.bottom <= thorIme.modes[0].top, "close-keyboard action shares the header row");
        geometry(thorIme);
        PanelLayout obscured = PanelLayout.create(thor.width, 152.5f, 1, 2, false, true, 0, 0);
        check(obscured.usable && obscured.pad.isEmpty() && !obscured.imeButton.isEmpty(),
                "short IME viewport keeps keyboard action without a touchpad");
        geometry(obscured);
        for (PanelLayout.Rect mode : obscured.modes)
            check(mode.bottom < obscured.height, "mode buttons stay above the real IME top");
        // Recorded Thor keyboard: about 303 px remain above the IME.
        PanelLayout thorOpen = PanelLayout.create(thor.width, 303 / density, 1, 2, false, true, 0, 0);
        check(thorOpen.usable && thorOpen.pad.isEmpty() && !thorOpen.imeButton.isEmpty(),
                "Thor open keyboard keeps the close action without a pause message");
        geometry(thorOpen);
        PanelLayout opening = PanelLayout.create(thor.width, 303 / density, 1, 2, false, false, 0, 0);
        check(opening.usable && opening.pad.isEmpty(), "keyboard animation never shows an undersized touchpad");
        PanelLayout narrowIme = PanelLayout.create(400, 200, 1, 2, false, true, 0, 0);
        check(narrowIme.usable && narrowIme.pad.isEmpty() && narrowIme.modes.length == 3,
                "narrow open keyboard keeps the page usable without a touchpad");
        geometry(narrowIme);
        PanelLayout narrowClosed = PanelLayout.create(400, 200, 1, 2, false, false, 0, 0);
        check(narrowClosed.usable && !narrowClosed.imeButton.isEmpty() && narrowClosed.modes.length == 3,
                "narrow keyboard action remains available beside its selector");
        geometry(narrowClosed);
        PanelLayout narrowPages = layout(320, 600, 1, 1, 0, 0);
        check(narrowPages.pageCount > 1 && narrowPages.pageBack.top > narrowPages.layerTabs[0].bottom,
                "narrow navigation keeps its separate row");
        PanelLayout enlargedPages = layout(thor.width, thor.height, 1.5f, 1, 0, 0);
        check(enlargedPages.pageCount > 1 && enlargedPages.pageBack.top > enlargedPages.layerTabs[0].bottom,
                "large font keeps separate rows when complete labels would not fit");
        PanelLayout onePage = layout(411, 900, 1, 1, 0, 0);
        check(onePage.pageCount == 1 && onePage.pageBack.isEmpty() && onePage.pageNext.isEmpty()
                && onePage.pageIndicator.isEmpty(), "one-page layers omit navigation geometry");
        String snapshots = "Thor lower screen model, 1240 x 1025 px, densityDpi 369, fontScale 1\n"
                + "Touchpad\n" + layout(thor.width, thor.height, 1, 0, 0, 0).snapshot()
                + "Remote keyboard\n" + thor.snapshot()
                + "Local keyboard\n" + thorLocal.snapshot()
                + "Input, IME hidden\n" + thorEditor.snapshot()
                + "Input, IME visible\n" + thorIme.snapshot();
        String snapshotPath = System.getProperty("panel.snapshot");
        if (snapshotPath != null) {
            check(snapshots.equals(java.nio.file.Files.readString(java.nio.file.Path.of(snapshotPath))),
                    "Thor coordinate snapshot changed. Review the layout before updating the fixture.");
        }
        if (args.length > 0 && args[0].equals("--snapshot")) System.out.print(snapshots);
        System.out.println("Passed " + checks + " panel layout checks");
    }
}
