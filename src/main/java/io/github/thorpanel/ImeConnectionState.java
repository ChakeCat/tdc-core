// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Verify the current user's editor and keyboard within the same dump section. */
final class ImeConnectionState {
    private static final Pattern USERS = Pattern.compile("^  UserId=(\\d+)[ \\t]*$", Pattern.MULTILINE);
    private static final String SERVICE = "Input method service state for ";

    private static final class Current {
        final String server, service;
        final boolean modern;

        Current(String block, boolean modern) {
            this.modern = modern;
            Matcher start = Pattern.compile("^" + SERVICE, Pattern.MULTILINE).matcher(block);
            if (start.find()) {
                server = block.substring(0, start.start());
                service = block.substring(start.start());
            } else {
                server = block;
                service = "";
            }
        }
    }

    static int editorDisplay(String dump) {
        Current current = current(dump);
        return current == null ? -1 : clientDisplay(current);
    }

    static boolean ready(String dump, String imeId, String editorPackage, int editorDisplay,
            int keyboardDisplay) {
        if (imeId == null || editorPackage == null || editorDisplay < 0 || keyboardDisplay < 0)
            return false;
        Current current = current(dump);
        if (current == null || current.service.isEmpty() || clientDisplay(current) != editorDisplay)
            return false;
        if (current.modern) {
            // Android 16 puts binding fields under this user's binding controller.
            int start = current.server.indexOf("    mBindingController:\n");
            int end = current.server.indexOf("    mCurClient=", start + 1);
            if (start < 0 || end < 0)
                return false;
            String binding = current.server.substring(start, end);
            if (!imeId.equals(single(binding, "^      mCurId=(\\S+)[ \\t]*$"))
                    || !"true".equals(single(binding, "^      mHasMainConnection=(\\w+)[ \\t]*$"))
                    || !"true".equals(single(current.server, "^    mBoundToMethod=(\\w+)[ \\t]*$"))
                    || number(single(binding, "^      mCurTokenDisplayId=(\\d+)[ \\t]*$"))
                            != keyboardDisplay)
                return false;
        } else {
            String binding = single(current.server, "^  mCurId=([^\\r\\n]*)$");
            if (binding == null || !binding.matches(Pattern.quote(imeId)
                            + " mHaveConnection=true mBoundToMethod=true(?: |$).*")
                    || number(single(current.server, "^  mCurTokenDisplayId=(\\d+)[ \\t]*$"))
                            != keyboardDisplay)
                return false;
        }
        // Do not mix fields from separate service dumps or an old editor entry.
        if (count(current.service, "^" + SERVICE) != 1)
            return false;
        return "true".equals(single(current.service, "^  mInputStarted=(\\w+)(?: |$).*"))
                && connected(single(current.service, "^  mStartedInputConnection=(\\S+).*"))
                && editorPackage.equals(single(current.service, "^    packageName=(\\S+)(?: |$).*"));
    }

    private static boolean connected(String value) {
        return value != null && !"null".equals(value);
    }

    private static int clientDisplay(Current current) {
        String indent = current.modern ? "    " : "  ";
        String client = single(current.server, "^" + indent + "mCurClient=([^\\r\\n]*)$");
        return client == null ? -1 : number(single(client,
                "^ClientState\\{[^\\r\\n}]* (?:displayId|mSelfReportedDisplayId)=(\\d+)\\}(?:[^\\r\\n]*)$"));
    }

    private static Current current(String dump) {
        if (dump == null)
            return null;
        dump = dump.replace("\r\n", "\n");
        Matcher users = USERS.matcher(dump);
        if (!users.find()) {
            // A modern dump without its user block is incomplete, not a legacy dump.
            if (dump.contains("mCurrentImeUserId="))
                return null;
            Current legacy = new Current(dump, false);
            String server = legacy.server;
            Matcher history = Pattern.compile(
                    "^  (?:mStartInputHistory:|mSoftInputShowHideHistory:|mImeTrackerService#History:)",
                    Pattern.MULTILINE).matcher(server);
            if (history.find())
                server = server.substring(0, history.start());
            return new Current(server + legacy.service, false);
        }
        // The repository and history precede these per-user dumps in Android 16.
        String header = dump.substring(0, users.start());
        int activeUser = number(single(header, "^  mCurrentImeUserId=(\\d+)[ \\t]*$"));
        if (activeUser < 0)
            return null;
        String selected = null;
        int start = users.start(), user = number(users.group(1));
        do {
            boolean next = users.find();
            int end = next ? users.start() : dump.length();
            if (user == activeUser) {
                if (selected != null)
                    return null;
                selected = dump.substring(start, end);
            }
            if (!next)
                break;
            start = end;
            user = number(users.group(1));
        } while (true);
        return selected == null ? null : new Current(selected, true);
    }

    private static String single(String value, String expression) {
        Matcher match = Pattern.compile(expression, Pattern.MULTILINE).matcher(value);
        if (!match.find())
            return null;
        String result = match.group(1);
        return match.find() ? null : result;
    }

    private static int count(String value, String expression) {
        Matcher match = Pattern.compile(expression, Pattern.MULTILINE).matcher(value);
        int count = 0;
        while (match.find())
            count++;
        return count;
    }

    private static int number(String value) {
        try {
            return value == null ? -1 : Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }
}
