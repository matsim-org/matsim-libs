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

// $Id$

package playground.kai.plansToPlans;

import java.io.PrintWriter;
import java.util.Iterator;

import org.matsim.config.Config;
import org.matsim.config.ConfigWriter;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.MatsimPlansReader;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansReaderI;
import org.matsim.plans.PlansWriter;
import org.matsim.utils.misc.ArgumentParser;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;

/**
 * @author kn after mrieser
 */
public class MyPlansToPlans {

	private Config config;
	private String configfile = null;
	private String dtdfile = null;

	public void run(final String[] args) {
		this.config = Gbl.createConfig(new String[]{"../padang/dlr-network/pconfig.xml"});
		ConfigWriter configwriter = new ConfigWriter(this.config, new PrintWriter(System.out));
		configwriter.write();

		final World world = Gbl.getWorld();

		if (this.config.world().getInputFile() != null) {
			final MatsimWorldReader worldReader = new MatsimWorldReader(world);
			worldReader.readFile(this.config.world().getInputFile());
		}

		NetworkLayer network = new NetworkLayer();
		world.setNetworkLayer(network);
		new MatsimNetworkReader(network).readFile(this.config.network().getInputFile());

		final Plans plans = new Plans(Plans.USE_STREAMING);
		final PlansReaderI plansReader = new MatsimPlansReader(plans);
		final PlansWriter plansWriter = new PlansWriter(plans);
		plans.setPlansWriter(plansWriter);
//		plans.addAlgorithm(new org.matsim.plans.algorithms.XY2Links(network));
		plansReader.readFile(this.config.plans().getInputFile());
		plans.printPlansCount();
		plansWriter.write();

		System.out.println("done.");
	}

	public static void main(final String[] args) {
		MyPlansToPlans app = new MyPlansToPlans();
		app.run(args);
	}

}
