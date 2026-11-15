// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package io.github.thorpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable panel geometry in dp. Font sizes are in sp, before font scaling. */
final class PanelLayout {
    static final int LETTERS = 0;
    static final int SYMBOLS = 1;
    static final int FUNCTIONS = 2;
    static final float MIN_TARGET = 48f;
    private static final float MARGIN = 12f;
    private static final float GAP = 6f;
    private static final Rect EMPTY = new Rect(0, 0, 0, 0);

    enum Variant { STANDARD, WIDE_SHORT, LARGE, NARROW }

    static final class Rect {
        final float left, top, right, bottom;

        Rect(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = Math.max(left, right);
            this.bottom = Math.max(top, bottom);
        }

        float width() { return right - left; }
        float height() { return bottom - top; }
        float centerX() { return (left + right) / 2f; }
        float centerY() { return (top + bottom) / 2f; }
        boolean contains(float x, float y) {
            return x >= left && x < right && y >= top && y < bottom;
        }

        boolean isEmpty() { return width() <= 0 || height() <= 0; }
    }

    static final class Key {
        final int code;
        final String label;
        final Rect bounds;

        Key(int code, String label, Rect bounds) {
            this.code = code;
            this.label = label;
            this.bounds = bounds;
        }
    }

    private static final class KeySpec {
        final int code;
        final String label;
        final float weight;

        KeySpec(int code, String label, float weight) {
            this.code = code;
            this.label = label;
            this.weight = weight;
        }
    }

    final Variant variant;
    final boolean usable, fullKeyboard;
    final float width, height, fontScale;
    final float textSp = 14f;
    final float keyTextSp = 14f;
    final float modeTextSp = 12f;
    final Rect title, status, settings, option, exit, content, pad;
    final Rect leftButton, rightButton, speedButton, imeButton;
    final Rect[] modes, layerTabs;
    final Rect pageBack, pageNext, pageIndicator;
    final List<Key> keys;
    final int layer, page, pageCount;

    private PanelLayout(Builder b) {
        variant = b.variant;
        usable = b.usable;
        fullKeyboard = b.fullKeyboard;
        width = b.width;
        height = b.height;
        fontScale = b.fontScale;
        title = b.title;
        status = b.status;
        settings = b.settings;
        option = b.option;
        exit = b.exit;
        content = b.content;
        pad = b.pad;
        leftButton = b.leftButton;
        rightButton = b.rightButton;
        speedButton = b.speedButton;
        imeButton = b.imeButton;
        modes = b.modes.clone();
        layerTabs = b.layerTabs.clone();
        pageBack = b.pageBack;
        pageNext = b.pageNext;
        pageIndicator = b.pageIndicator;
        keys = Collections.unmodifiableList(new ArrayList<>(b.keys));
        layer = b.layer;
        page = b.page;
        pageCount = b.pageCount;
    }

    static PanelLayout create(float widthDp, float heightDp, float fontScale, int mode,
            boolean localKeyboard, boolean imeVisible, int layer, int page) {
        float imeClearance = (mode == 1 && localKeyboard) || (mode == 2 && imeVisible) ? 48f : 0f;
        return new Builder(widthDp, heightDp, fontScale, layer).build(mode, page, imeClearance);
    }

    /** A stable geometry record for comparing a device's layout across changes. */
    String snapshot() {
        StringBuilder out = new StringBuilder();
        out.append(variant).append(' ').append(width).append('x').append(height)
                .append(" font=").append(fontScale).append(" usable=").append(usable)
                .append(" full=").append(fullKeyboard).append(" layer=").append(layer)
                .append(" page=").append(page).append('/').append(pageCount).append('\n');
        record(out, "title", title);
        record(out, "status", status);
        record(out, "settings", settings);
        record(out, "option", option);
        record(out, "exit", exit);
        for (int i = 0; i < modes.length; i++) record(out, "mode" + i, modes[i]);
        record(out, "content", content);
        record(out, "pad", pad);
        record(out, "leftButton", leftButton);
        record(out, "rightButton", rightButton);
        record(out, "speedButton", speedButton);
        record(out, "imeButton", imeButton);
        for (int i = 0; i < layerTabs.length; i++) record(out, "layer" + i, layerTabs[i]);
        record(out, "pageBack", pageBack);
        record(out, "pageNext", pageNext);
        record(out, "pageIndicator", pageIndicator);
        for (Key key : keys) record(out, Integer.toString(key.code), key.bounds);
        return out.toString();
    }

    private static void record(StringBuilder out, String name, Rect r) {
        out.append(name).append(':').append(r.left).append(',').append(r.top)
                .append(',').append(r.right).append(',').append(r.bottom).append('\n');
    }

    private static float dimension(float value) {
        return Float.isFinite(value) && value > 0 ? value : 0;
    }

    private static KeySpec k(int code, String label) { return k(code, label, 1); }
    private static KeySpec k(int code, String label, float weight) {
        return new KeySpec(code, label, weight);
    }

    private static final KeySpec[][] FULL = {
        {k(111,"Esc"),k(131,"F1"),k(132,"F2"),k(133,"F3"),k(134,"F4"),k(135,"F5"),
            k(136,"F6"),k(137,"F7"),k(138,"F8"),k(139,"F9"),k(140,"F10"),k(141,"F11"),k(142,"F12")},
        {k(68,"`"),k(8,"1"),k(9,"2"),k(10,"3"),k(11,"4"),k(12,"5"),k(13,"6"),k(14,"7"),
            k(15,"8"),k(16,"9"),k(7,"0"),k(69,"-"),k(70,"="),k(67,"⌫",1.6f)},
        {k(61,"Tab",1.5f),k(45,"Q"),k(51,"W"),k(33,"E"),k(46,"R"),k(48,"T"),k(53,"Y"),
            k(49,"U"),k(37,"I"),k(43,"O"),k(44,"P"),k(71,"["),k(72,"]"),k(73,"\\",1.1f)},
        {k(115,"Caps",1.6f),k(29,"A"),k(47,"S"),k(32,"D"),k(34,"F"),k(35,"G"),k(36,"H"),
            k(38,"J"),k(39,"K"),k(40,"L"),k(74,";"),k(75,"'"),k(66,"Enter",2)},
        {k(59,"Shift",1.8f),k(54,"Z"),k(52,"X"),k(31,"C"),k(50,"V"),k(30,"B"),k(42,"N"),
            k(41,"M"),k(55,","),k(56,"."),k(76,"/"),k(19,"↑"),k(112,"Del",1.3f)},
        {k(113,"Ctrl",1.25f),k(117,"Win/⌘",1.6f),k(57,"Alt",1.2f),k(62,"Space",4.5f),
            k(122,"Home",1.2f),k(123,"End",1.2f),k(21,"←"),k(20,"↓"),k(22,"→")}
    };

    private static final KeySpec[] MODIFIERS = {
        k(113,"Ctrl"),k(117,"Win/⌘"),k(57,"Alt"),k(59,"Shift")
    };

    private static KeySpec[] layerKeys(int layer) {
        List<KeySpec> result = new ArrayList<>();
        for (KeySpec[] row : FULL) {
            for (KeySpec key : row) {
                if (ModifierKeys.isModifier(key.code)) continue;
                boolean letter = key.code >= 29 && key.code <= 54;
                boolean shared = key.code == 62 || key.code == 67 || key.code == 66;
                boolean symbol = (key.code >= 7 && key.code <= 16)
                        || (key.code >= 68 && key.code <= 76) || key.code == 55 || key.code == 56;
                if (shared || (layer == LETTERS && (letter || key.code == 115 || key.code == 61))
                        || (layer == SYMBOLS && symbol)
                        || (layer == FUNCTIONS && !letter && !symbol && key.code != 115 && key.code != 61)) {
                    result.add(k(key.code, key.label));
                }
            }
        }
        return result.toArray(new KeySpec[0]);
    }

    private static final class Builder {
        final float width, height, fontScale, target, gap = GAP;
        final int layer;
        Variant variant;
        boolean usable, fullKeyboard;
        int page, pageCount = 1;
        Rect title = EMPTY, status = EMPTY, settings = EMPTY, option = EMPTY, exit = EMPTY;
        Rect content = EMPTY, pad = EMPTY, leftButton = EMPTY, rightButton = EMPTY;
        Rect speedButton = EMPTY, imeButton = EMPTY, pageBack = EMPTY, pageNext = EMPTY;
        Rect pageIndicator = EMPTY;
        Rect[] modes = new Rect[0], layerTabs = new Rect[0];
        List<Key> keys = new ArrayList<>();

        Builder(float widthDp, float heightDp, float scale, int layer) {
            width = dimension(widthDp);
            height = dimension(heightDp);
            fontScale = Float.isFinite(scale) && scale > 0 ? Math.max(1f, scale) : 1f;
            target = Math.max(MIN_TARGET, (float)Math.ceil(14f * fontScale * 1.4f + 16f));
            this.layer = Math.max(LETTERS, Math.min(FUNCTIONS, layer));
            variant = width < 600 ? Variant.NARROW
                    : width >= 840 && height < 480 ? Variant.WIDE_SHORT
                    : width >= 840 || height >= 900 ? Variant.LARGE : Variant.STANDARD;
        }

        PanelLayout build(int mode, int requestedPage, float imeClearance) {
            float available = width - MARGIN * 2;
            float modeWidth = Math.max(target, 4 * 12f * fontScale + 16f);
            if (available < Math.max(target * 2 + 96f + gap * 2, modeWidth * 3 + gap * 2)
                    || height < MARGIN * 2 + target) return new PanelLayout(this);
            boolean compactHeader = variant == Variant.WIDE_SHORT && available >= 104 + 3 * modeWidth
                    + target * 2 + Math.max(140, 8 * 14 * fontScale) + gap * 6;
            boolean joinedHeader = width >= 500 && available >= 96 + target * 2
                    + Math.max(140, 8 * 14 * fontScale) + gap * 3;
            float headerHeight = compactHeader ? target
                    : target * (joinedHeader ? 2 : 3) + 20 * fontScale + gap * (joinedHeader ? 2 : 3);
            if (height < MARGIN * 2 + headerHeight + gap) return new PanelLayout(this);
            float top;
            if (compactHeader) {
                float x = MARGIN;
                title = new Rect(x, MARGIN, x + 104, MARGIN + target / 2);
                status = new Rect(x, title.bottom, title.right, MARGIN + target);
                x = title.right + gap;
                modes = row(x, MARGIN, modeWidth * 3 + gap * 2, target, 3);
                x = modes[2].right + gap;
                float optionWidth = available - (x - MARGIN) - target * 2 - gap * 2;
                option = new Rect(x, MARGIN, x + optionWidth, MARGIN + target);
                settings = new Rect(option.right + gap, MARGIN, option.right + gap + target, MARGIN + target);
                exit = new Rect(settings.right + gap, MARGIN, width - MARGIN, MARGIN + target);
                top = MARGIN + target + gap;
            } else {
                exit = new Rect(width - MARGIN - target, MARGIN, width - MARGIN, MARGIN + target);
                settings = new Rect(exit.left - gap - target, MARGIN, exit.left - gap, MARGIN + target);
                title = new Rect(MARGIN, MARGIN, joinedHeader ? MARGIN + 96 : settings.left - gap, MARGIN + target);
                top = MARGIN + target + gap;
                if (joinedHeader) {
                    option = new Rect(title.right + gap, MARGIN, settings.left - gap, MARGIN + target);
                } else {
                    option = new Rect(MARGIN, top, width - MARGIN, top + target);
                    top = option.bottom + gap;
                }
                status = new Rect(MARGIN, top, width - MARGIN, top + 20 * fontScale);
                top = status.bottom + gap;
                modes = row(MARGIN, top, available, target, 3);
                top += target + gap;
            }
            // Thor's panel uses adjustNothing. Keep its existing IME navigation strip clear.
            float bottom = Math.max(MARGIN, height - MARGIN - imeClearance);
            content = new Rect(MARGIN, Math.min(top, bottom), width - MARGIN, bottom);
            if (mode == 1) keyboard(requestedPage);
            else if (mode == 2) editor();
            else touchpad();
            return new PanelLayout(this);
        }

        void touchpad() {
            float buttonWidth = (content.width() - gap * 2) / 3;
            if (buttonWidth < target || content.height() < target + gap + 96) return;
            Rect[] buttons = row(content.left, content.bottom - target, content.width(), target, 3);
            leftButton = buttons[0];
            rightButton = buttons[1];
            speedButton = buttons[2];
            pad = new Rect(content.left, content.top, content.right, leftButton.top - gap);
            usable = true;
        }

        void editor() {
            if (content.width() < Math.max(target, 5 * 14 * fontScale + 24)
                    || content.height() < target + gap + 96) return;
            imeButton = new Rect(content.left, content.top, content.right, content.top + target);
            pad = new Rect(content.left, imeButton.bottom + gap, content.right, content.bottom);
            usable = true;
        }

        float labelWidth(String label) {
            return Math.max(target, label.length() * 14 * fontScale * 0.64f + 16);
        }

        float fullWidth() {
            float required = 0;
            for (KeySpec[] row : FULL) {
                float unit = target;
                float weights = 0;
                for (KeySpec key : row) {
                    unit = Math.max(unit, labelWidth(key.label) / key.weight);
                    weights += key.weight;
                }
                required = Math.max(required, weights * unit + (row.length - 1) * gap);
            }
            return required;
        }

        void keyboard(int requestedPage) {
            float keyboardWidth = Math.min(content.width(), Math.max(1000f, fullWidth()));
            if (keyboardWidth >= fullWidth() && content.height() >= target * FULL.length + gap * (FULL.length - 1)) {
                fullKeyboard = true;
                float keyHeight = Math.min(Math.max(target, 64f), (content.height() - gap * (FULL.length - 1)) / FULL.length);
                float left = content.centerX() - keyboardWidth / 2;
                for (int r = 0; r < FULL.length; r++) {
                    KeySpec[] row = FULL[r];
                    float weights = 0;
                    for (KeySpec key : row) weights += key.weight;
                    float unit = (keyboardWidth - gap * (row.length - 1)) / weights;
                    float x = left;
                    for (KeySpec key : row) {
                        keys.add(new Key(key.code, key.label,
                                new Rect(x, content.top + r * (keyHeight + gap), x + unit * key.weight,
                                        content.top + r * (keyHeight + gap) + keyHeight)));
                        x += unit * key.weight + gap;
                    }
                }
                usable = true;
                return;
            }
            KeySpec[] values = layerKeys(layer);
            float minimum = target;
            for (KeySpec key : values) minimum = Math.max(minimum, labelWidth(key.label));
            int columns = (int)((content.width() + gap) / (minimum + gap));
            float modifierWidth = 0;
            for (KeySpec modifier : MODIFIERS) modifierWidth += labelWidth(modifier.label);
            modifierWidth += gap * (MODIFIERS.length - 1);
            int modifierRows = content.width() >= modifierWidth ? 1 : 2;
            float widestModifier = 0;
            for (KeySpec modifier : MODIFIERS) widestModifier = Math.max(widestModifier, labelWidth(modifier.label));
            if (columns < 1 || (modifierRows == 2 && content.width() < widestModifier * 2 + gap)) return;
            // Use the rendered 14sp labels when deciding whether navigation fits beside the layers.
            float[] navigationWidths = {
                Math.max(target, 2 * 14 * fontScale + 16),
                Math.max(target, 4 * 14 * fontScale + 16),
                Math.max(target, 3 * 14 * fontScale + 16),
                Math.max(target, 3 * 14 * fontScale + 16),
                Math.max(target, 7 * 14 * fontScale * .64f + 16),
                Math.max(target, 3 * 14 * fontScale + 16)
            };
            float navigationWidth = gap * (navigationWidths.length - 1);
            for (float cell : navigationWidths) navigationWidth += cell;
            boolean joinedNavigation = width >= 500 && content.width() >= navigationWidth;
            int neededRows = (values.length + columns - 1) / columns;
            int rows = (int)((content.height() + gap) / (target + gap)) - 1 - modifierRows;
            if (neededRows > rows && !joinedNavigation) rows--;
            if (rows < 1) return;
            rows = Math.min(rows, neededRows);
            int pageSize = rows * columns;
            pageCount = (values.length + pageSize - 1) / pageSize;
            page = Math.max(0, Math.min(pageCount - 1, requestedPage));
            joinedNavigation &= pageCount > 1;
            if (joinedNavigation) {
                float extra = (content.width() - navigationWidth) / navigationWidths.length;
                Rect[] navigation = new Rect[navigationWidths.length];
                float x = content.left;
                for (int i = 0; i < navigation.length; i++) {
                    float cell = navigationWidths[i] + extra;
                    navigation[i] = new Rect(x, content.top, x + cell, content.top + target);
                    x += cell + gap;
                }
                layerTabs = new Rect[]{navigation[0], navigation[1], navigation[2]};
                pageBack = navigation[3];
                pageIndicator = navigation[4];
                pageNext = navigation[5];
            } else {
                layerTabs = row(content.left, content.top, content.width(), target, 3);
            }
            float y = content.top + target + gap;
            for (int i = page * pageSize; i < Math.min(values.length, (page + 1) * pageSize); i++) {
                int index = i - page * pageSize;
                float keyWidth = (content.width() - gap * (columns - 1)) / columns;
                float x = content.left + (index % columns) * (keyWidth + gap);
                float keyY = y + (index / columns) * (target + gap);
                keys.add(new Key(values[i].code, values[i].label, new Rect(x, keyY, x + keyWidth, keyY + target)));
            }
            y += rows * (target + gap);
            if (pageCount > 1 && !joinedNavigation) {
                Rect[] pageRow = row(content.left, y, content.width(), target, 3);
                pageBack = pageRow[0];
                pageIndicator = pageRow[1];
                pageNext = pageRow[2];
                y += target + gap;
            }
            float extra = (content.width() - modifierWidth) / MODIFIERS.length;
            float x = content.left;
            for (int i = 0; i < MODIFIERS.length; i++) {
                KeySpec modifier = MODIFIERS[i];
                float keyWidth = modifierRows == 1 ? labelWidth(modifier.label) + extra : (content.width() - gap) / 2;
                float keyY = modifierRows == 1 ? y : y + (i / 2) * (target + gap);
                if (modifierRows == 2) x = content.left + (i % 2) * (keyWidth + gap);
                keys.add(new Key(modifier.code, modifier.label, new Rect(x, keyY, x + keyWidth, keyY + target)));
                x += keyWidth + gap;
            }
            usable = true;
        }

        Rect[] row(float left, float top, float width, float height, int count) {
            Rect[] result = new Rect[count];
            float cell = (width - gap * (count - 1)) / count;
            for (int i = 0; i < count; i++) {
                float x = left + i * (cell + gap);
                result[i] = new Rect(x, top, x + cell, top + height);
            }
            return result;
        }
    }
}
