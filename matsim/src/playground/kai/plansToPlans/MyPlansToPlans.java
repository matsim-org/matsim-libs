/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.plansToPlans;

import java.io.PrintWriter;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;

/**
 * @author kn after mrieser
 */
public class MyPlansToPlans {

	private Config config;
	private final String configfile = null;
	private final String dtdfile = null;

	public void run(final String[] args) {
		this.config = Gbl.createConfig(new String[]{"../padang/dlr-network/pconfig.xml"});
		new ConfigWriter(this.config).writeStream(new PrintWriter(System.out));

//		final World world = Gbl.getWorld();
//
//		if (this.config.world().getInputFile() != null) {
//			final MatsimWorldReader worldReader = new MatsimWorldReader(world);
//			worldReader.readFile(this.config.world().getInputFile());
//		}

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		final PopulationImpl plans = new PopulationImpl();
		plans.setIsStreaming(true);
		final PopulationReader plansReader = new MatsimPopulationReader(plans, network);
		final PopulationWriter plansWriter = new PopulationWriter(plans);
		plansWriter.startStreaming(this.config.plans().getOutputFile());
//		plans.addAlgorithm(new org.matsim.population.algorithms.XY2Links(network));
		plans.addAlgorithm(plansWriter); // planswriter must be the last algorithm added
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.closeStreaming();

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		MyPlansToPlans app = new MyPlansToPlans();
		app.run(args);
	}

}
