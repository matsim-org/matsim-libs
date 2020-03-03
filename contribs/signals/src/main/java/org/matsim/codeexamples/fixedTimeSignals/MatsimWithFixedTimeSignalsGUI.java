/**
 *
 */
package org.matsim.codeexamples.fixedTimeSignals;

import org.matsim.run.gui.Gui;

/**
 * Starts a MATSim GUI that is able to simulate fixed-time traffic signals 
 * and by default runs the example that can be found in examples/tutorial/example90TrafficLights with lanes.
 *
 * @author tthunig
 */
public class MatsimWithFixedTimeSignalsGUI {

    public static void main(String[] args) {
        Gui.show("MATSim GUI from example project", RunSignalSystemsExample.class);
    }

}
