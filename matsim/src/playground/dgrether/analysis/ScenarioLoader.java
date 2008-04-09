/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package playground.dgrether.analysis;

import java.util.Date;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;


/**
 * @author dgrether
 *
 */
public class ScenarioLoader {


	private static final Logger log = Logger.getLogger(ScenarioLoader.class);

	private NetworkLayer networkLayer;

	private Plans plans;


	public ScenarioLoader(String config) {
		Gbl.createConfig(new String[] {config});
		this.networkLayer = loadNetwork(Gbl.getConfig().network().getInputFile());
		this.loadWorld();
		this.plans = this.loadPopulation();
	}

	public void setPlans(Plans p) {
		this.plans = p;
	}

	public Plans getPlans() {
		return this.plans;
	}

	public World getWorld() {
		return Gbl.getWorld();
	}

	public NetworkLayer getNetwork() {
		return this.networkLayer;
	}

	private Plans loadPopulation() {
		Plans population = new Plans(Plans.NO_STREAMING);
		printNote("", "  reading plans xml file... ");
		PlansReaderI plansReader = new MatsimPlansReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		printNote("", "  done");

		return population;
	}

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	private NetworkLayer loadNetwork(final String networkFile) {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(networkFile);
		printNote("", "  done");

		return network;
	}

	private void loadWorld() {
		if (Gbl.getConfig().world().getInputFile() != null) {
			printNote("", "  reading world xml file... ");
			final MatsimWorldReader worldReader = new MatsimWorldReader(Gbl.getWorld());
			worldReader.readFile(Gbl.getConfig().world().getInputFile());
			printNote("", "  done");
		} else {
			printNote("","  No World input file given in config.xml!");
		}
	}


	/**
	 * an internal routine to generated some (nicely?) formatted output. This
	 * helps that status output looks about the same every time output is written.
	 *
	 * @param header
	 *          the header to print, e.g. a module-name or similar. If empty
	 *          <code>""</code>, no header will be printed at all
	 * @param action
	 *          the status message, will be printed together with a timestamp
	 */
	private final void printNote(final String header, final String action) {
		if (header != "") {
			System.out.println();
			System.out
					.println("===============================================================");
			System.out.println("== " + header);
			System.out
					.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()));
		}
		if (header != "") {
			System.out.println();
		}
	}


}
