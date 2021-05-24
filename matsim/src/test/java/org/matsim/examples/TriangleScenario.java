/* *********************************************************************** *
 * project: org.matsim.*
 * TriangleScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.examples;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;

public abstract class TriangleScenario {

	public static final long CHECKSUM_WORLD_EMPTY = 4202206189L;

	private static final String studyfolder = "test/scenarios/triangle/";

	private TriangleScenario() {
	}

	//////////////////////////////////////////////////////////////////////
	// setup
	//////////////////////////////////////////////////////////////////////

	public static final void setUpScenarioConfig(final Config config) {
		config.network().setInputFile(studyfolder + "network.xml");
		config.facilities().setInputFile(studyfolder + "facilities.xml");
	}

	//////////////////////////////////////////////////////////////////////
	// write output
	//////////////////////////////////////////////////////////////////////

	public static final void writePlans(Population plans, Network network, String filename) {
		System.out.println("  writing plans xml file... ");
		new PopulationWriter(plans, network).write(filename);
		System.out.println("  done.");
	}

	public static final void writeFacilities(ActivityFacilities facilities, String filename) {
		System.out.println("  writing facilities xml file... ");
		new FacilitiesWriter(facilities).write(filename);
		System.out.println("  done.");
	}

	public static final void writeNetwork(Network network, String filename) {
		System.out.println("  writing network xml file... ");
		NetworkWriter network_writer = new NetworkWriter(network);
		network_writer.write(filename);
		System.out.println("  done.");
	}

}
