// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A conservative snapshot of task identities and their current window bounds. */
public final class WindowTaskState {
    private static final int MAX_DUMP = 1024 * 1024;
    private static final Pattern DISPLAY = Pattern.compile("^\\s*Display #(\\d+) \\(activities.*");
    private static final Pattern TASK = Pattern.compile("^(\\s*)\\* Task\\{[^#\\r\\n]*#(\\d+) ([^\\r\\n]*)");
    private static final Pattern ACTIVITY = Pattern.compile("ActivityRecord\\{\\S+ u\\d+ ([A-Za-z0-9_.$]+)/(\\S+?)(?:\\s|\\})");
    private static final Pattern RECT = Pattern.compile("(?:^|\\s)mBounds=Rect\\((-?\\d+), (-?\\d+) - (-?\\d+), (-?\\d+)\\)");
    private static final Pattern PACKAGE = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+");

    public static final class Bounds {
        public final int left, top, right, bottom;
        Bounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
        public boolean valid() {
            return left >= 0 && top >= 0 && right > left && bottom > top
                    && right <= 100000 && bottom <= 100000;
        }
    }

    public static final class Task {
        public final int id, displayId, parentTaskId, rootTaskId;
        public final String packageName, component, windowingMode;
        public final boolean visible;
        public final Bounds bounds;
        private Task(Builder value) {
            id = value.id;
            displayId = value.display;
            parentTaskId = value.parent;
            rootTaskId = value.root;
            packageName = value.packageName;
            component = value.component;
            windowingMode = value.mode;
            visible = value.visible;
            bounds = value.bounds;
        }
        public boolean complete() {
            return displayId >= 0 && validPackage(packageName) && !component.isEmpty()
                    && bounds != null && bounds.valid() && (splitWindow() || "fullscreen".equals(windowingMode)
                    || "freeform".equals(windowingMode) || "pinned".equals(windowingMode));
        }
        public boolean splitWindow() {
            return "multi-window".equals(windowingMode) || "split-screen-primary".equals(windowingMode)
                    || "split-screen-secondary".equals(windowingMode);
        }
    }

    private static final class Builder {
        int id, display, parent, root, indent;
        String packageName = "", component = "", mode = "";
        boolean visible, activitySeen, configurationSeen;
        Bounds bounds;
    }

    public final boolean recognized;
    public final List<Task> tasks;
    private WindowTaskState(boolean recognized, List<Task> tasks) {
        this.recognized = recognized;
        this.tasks = Collections.unmodifiableList(tasks);
    }

    public static boolean validPackage(String name) {
        return name != null && name.length() <= 255 && PACKAGE.matcher(name).matches();
    }

    public static WindowTaskState parse(String dump) {
        List<Builder> all = new ArrayList<>();
        List<Builder> stack = new ArrayList<>();
        if (dump == null || dump.length() > MAX_DUMP || !dump.contains("ACTIVITY MANAGER ACTIVITIES")) {
            return new WindowTaskState(false, Collections.emptyList());
        }
        int display = -1;
        boolean sawDisplay = false;
        try {
            for (String line : dump.split("\\r?\\n")) {
                Matcher match = DISPLAY.matcher(line);
                if (match.matches()) {
                    display = Integer.parseInt(match.group(1));
                    sawDisplay = true;
                    stack.clear();
                    continue;
                }
                // Later supervisor sections repeat task IDs in a different tree format.
                if (sawDisplay && !line.isEmpty() && !Character.isWhitespace(line.charAt(0))) break;
                match = TASK.matcher(line);
                if (match.matches()) {
                    if (display < 0 || all.size() >= 512) return new WindowTaskState(false, Collections.emptyList());
                    int indent = match.group(1).length();
                    while (!stack.isEmpty() && stack.get(stack.size() - 1).indent >= indent) stack.remove(stack.size() - 1);
                    Builder task = new Builder();
                    task.id = Integer.parseInt(match.group(2));
                    for (Builder previous : all) if (previous.id == task.id) return new WindowTaskState(false, Collections.emptyList());
                    task.display = display;
                    task.indent = indent;
                    task.parent = stack.isEmpty() ? -1 : stack.get(stack.size() - 1).id;
                    task.root = stack.isEmpty() ? task.id : stack.get(0).id;
                    String attributes = match.group(3);
                    task.visible = "true".equals(attribute(attributes, "visible"));
                    task.mode = attribute(attributes, "mode");
                    String root = attribute(attributes, "rootTaskId");
                    if (!root.isEmpty()) task.root = Integer.parseInt(root);
                    all.add(task);
                    stack.add(task);
                    continue;
                }
                if (stack.isEmpty()) continue;
                Builder task = stack.get(stack.size() - 1);
                String trimmed = line.trim();
                if (!task.activitySeen && trimmed.startsWith("mBounds=")) {
                    task.bounds = readBounds(trimmed);
                }
                if (!task.activitySeen && (trimmed.startsWith("topResumedActivity=") || trimmed.startsWith("* Hist"))) {
                    Matcher activity = ACTIVITY.matcher(trimmed);
                    if (activity.find()) {
                        task.packageName = activity.group(1);
                        String name = activity.group(2);
                        if (name.startsWith(".")) name = task.packageName + name;
                        task.component = task.packageName + "/" + name;
                        task.activitySeen = true;
                    }
                }
                if (!task.configurationSeen && task.activitySeen && trimmed.startsWith("CurrentConfiguration=")) {
                    if (task.bounds == null) task.bounds = readBounds(trimmed);
                    String mode = attribute(trimmed, "mWindowingMode");
                    // The activity and task must agree. During transitions, wait for a complete snapshot.
                    if (!mode.isEmpty() && !mode.equals(task.mode)) task.mode = "";
                    task.configurationSeen = true;
                }
            }
        } catch (NumberFormatException invalid) {
            return new WindowTaskState(false, Collections.emptyList());
        }
        List<Task> tasks = new ArrayList<>();
        for (Builder value : all) tasks.add(new Task(value));
        return new WindowTaskState(sawDisplay && !tasks.isEmpty(), tasks);
    }

    private static String attribute(String line, String name) {
        Matcher match = Pattern.compile("(?:^|\\s)" + name + "=([^\\s}]+)").matcher(line);
        return match.find() ? match.group(1) : "";
    }

    private static Bounds readBounds(String line) {
        Matcher rect = RECT.matcher(line);
        return rect.find() ? new Bounds(Integer.parseInt(rect.group(1)), Integer.parseInt(rect.group(2)),
                Integer.parseInt(rect.group(3)), Integer.parseInt(rect.group(4))) : null;
    }

    public Task task(int id) {
        for (Task task : tasks) if (task.id == id) return task;
        return null;
    }

    public Task frontTask(int displayId) {
        for (Task task : tasks) if (task.displayId == displayId && task.visible && task.complete()) return task;
        return null;
    }

    public Task packageTask(String packageName, int displayId) {
        Task fallback = null;
        for (Task task : tasks) {
            if (task.displayId != displayId || !packageName.equals(task.packageName) || !task.complete()) continue;
            if (task.visible) return task;
            if (fallback == null) fallback = task;
        }
        return fallback;
    }

    public boolean pairedAbove(String remotePackage, int panelId, String panelPackage) {
        if (!recognized || !validPackage(remotePackage) || remotePackage.equals(panelPackage)) return false;
        Task panel = task(panelId);
        if (panel == null || !panelPackage.equals(panel.packageName) || !panel.complete()
                || !panel.visible || !panel.splitWindow()) return false;
        Task remote = packageTask(remotePackage, panel.displayId);
        if (remote == null || remote.id == panel.id || !remote.visible || !remote.splitWindow()) return false;
        if ("multi-window".equals(panel.windowingMode) || "multi-window".equals(remote.windowingMode)) {
            if (!"multi-window".equals(panel.windowingMode) || !"multi-window".equals(remote.windowingMode)
                    || panel.parentTaskId < 0 || remote.parentTaskId < 0 || panel.rootTaskId != remote.rootTaskId
                    || panel.parentTaskId == remote.parentTaskId) return false;
        } else if (panel.windowingMode.equals(remote.windowingMode)) return false;
        Bounds upper = remote.bounds, lower = panel.bounds;
        int gap = lower.top - upper.bottom;
        int tolerance = Math.max(16, Math.min(64, Math.min(upper.bottom - upper.top, lower.bottom - lower.top) / 10));
        return upper.top < lower.top && gap >= 0 && gap <= tolerance
                && Math.abs(upper.left - lower.left) <= 2 && Math.abs(upper.right - lower.right) <= 2;
    }
}
