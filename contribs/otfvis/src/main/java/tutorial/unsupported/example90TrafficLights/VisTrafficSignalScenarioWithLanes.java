/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTrafficSignalScenarioWithLanes
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package tutorial.unsupported.example90TrafficLights;

import org.matsim.contrib.otfvis.OTFVis;

import tutorial.trafficsignals.RunCreateTrafficSignalScenarioWithLanesExample;

/**
 * This class contains a simple example how to visualize a scenario with lanes and signalized intersections.
 * 
 * @author dgrether
 * 
 * @see org.matsim.signals
 * @see http://matsim.org/node/384
 */
public class VisTrafficSignalScenarioWithLanes {

	private void run() {
		String configFile = new RunCreateTrafficSignalScenarioWithLanesExample().run();
		OTFVis.playConfig(configFile);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new VisTrafficSignalScenarioWithLanes().run();
	}

}
