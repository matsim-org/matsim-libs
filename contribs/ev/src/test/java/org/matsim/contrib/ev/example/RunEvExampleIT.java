package org.matsim.contrib.ev.example;

import org.junit.Test;

public class RunEvExampleIT {

    @Test
    public void run() {
		new RunEvExample().run("config.xml");
    }
}
