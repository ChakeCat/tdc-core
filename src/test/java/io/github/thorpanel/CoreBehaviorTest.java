// Copyright 2026 雷神的锤 contributors
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0
package io.github.thorpanel;

import java.io.File;
import java.nio.file.Path;
import org.junit.Test;

/** Run the same production behavior suites from JUnit and standalone main entry points. */
public final class CoreBehaviorTest {
    private static final String[] NO_ARGS = new String[0];

    @Test public void interaction() { InteractionTest.main(NO_ARGS); }
    @Test public void keyboardInput() { KeyboardInputTest.main(NO_ARGS); }
    @Test public void remoteLaunchPolicy() { RemoteLaunchPolicyTest.main(NO_ARGS); }
    @Test public void sessionEpoch() { SessionEpochTest.main(NO_ARGS); }
    @Test public void displayTopology() { DisplayTopologyTest.main(NO_ARGS); }
    @Test public void modePolicy() { ModePolicyTest.main(NO_ARGS); }
    @Test public void panelRegionPolicy() { PanelRegionPolicyTest.main(NO_ARGS); }
    @Test public void windowTaskState() { WindowTaskStateTest.main(NO_ARGS); }

    @Test public void panelLayout() throws Exception {
        if (System.getProperty("panel.snapshot") == null) {
            throw new AssertionError("Set panel.snapshot to tests/fixtures/panel-thor-default.txt");
        }
        PanelLayoutTest.main(NO_ARGS);
    }

    @Test public void commandRunner() throws Exception {
        // Gradle loads tests through a worker classloader. Its launcher classpath
        // alone cannot find the child main used by the existing command suite.
        String previous = System.getProperty("java.class.path");
        String tests = Path.of(CommandRunnerTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        String production = Path.of(CommandRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        System.setProperty("java.class.path", tests + File.pathSeparator + production);
        try { CommandRunnerTest.main(NO_ARGS); }
        finally {
            if (previous == null) System.clearProperty("java.class.path");
            else System.setProperty("java.class.path", previous);
        }
    }
}
