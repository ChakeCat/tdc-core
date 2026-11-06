// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/** The background follows the keyboard, except while explicitly opening or in settings. */
final class DockVisibility {
    private boolean keyboard,opening,settings;
    private int editorDisplay;
    void editorDisplay(int display){editorDisplay=display;}
    boolean upperEditor(){return editorDisplay==0;}
    void beginOpen(){opening=true;settings=false;}
    void finishOpen(){opening=false;}
    void showSettings(){settings=true;opening=false;}
    void dismissSettings(){settings=false;}
    void keyboard(boolean visible){keyboard=visible;}
    boolean visible(){return upperEditor()&&!settings&&(opening||keyboard);}
}
