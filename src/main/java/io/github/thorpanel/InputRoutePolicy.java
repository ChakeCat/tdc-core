// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

/** Opening/cancelling the system picker does not change the user's routing preference. */
final class InputRoutePolicy {
    static final int NONE=0, OPENED=1, CLOSED=2;
    private boolean shown,armed;
    InputRoutePolicy(boolean initiallyShown){shown=initiallyShown;armed=!initiallyShown;}
    int update(boolean visible){
        if(visible==shown)return NONE;
        shown=visible;
        if(!armed){if(!visible)armed=true;return NONE;}
        return visible?OPENED:CLOSED;
    }
    static boolean explicitExternalChoice(String selected){
        return selected!=null&&!selected.startsWith("io.github.thorpanel/");
    }
}
