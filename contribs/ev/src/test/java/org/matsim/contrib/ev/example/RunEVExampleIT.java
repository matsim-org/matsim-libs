package org.matsim.contrib.ev.example;

import org.junit.Test;

public class RunEVExampleIT {

    @Test
    public void run() {
        new RunEVExample().run("config.xml");
    }
}