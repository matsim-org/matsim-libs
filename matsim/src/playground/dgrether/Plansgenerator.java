/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
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

package playground.dgrether;

import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.plans.PlansWriter;
import org.matsim.plans.PlansWriterHandlerImplV4;
import org.matsim.plans.Route;


/**
 * @author dgrether
 *
 */
public class Plansgenerator {

	private static final String network = "../matsimWithindayTesting/testdata/tests/withinday/network.xml";

	private static final String plansOut = "../matsimWithindayTesting/testdata/tests/withinday/newPlans.xml";


	private Plans plans;

	private void init() {
		Config config = Gbl.createConfig(null);
		config.addCoreModules();

		config.plans().setOutputVersion("v4");
		config.plans().setOutputFile(plansOut);

		this.loadNetwork(network);
	}

	private void createPlans() throws Exception {
		init();
		this.plans = new Plans(false);
		int homeEndtime = 6 * 3600;
		for (int i = 1; i <= 100; i++) {
			Person p = new Person(String.valueOf(i), null, null, "yes", "always", "yes");
			Plan plan = new Plan(p);
			p.addPlan(plan);
			//home
			homeEndtime += 0.5 * 60;
			plan.createAct("h", "-25000", "0", "1", null, Integer.toString(homeEndtime), null, null);
			//leg to work
			Leg leg = plan.createLeg("1", "car", null, null, null);
			Route route = new Route();
			route.setRoute("2 4 5");
			leg.setRoute(route);
			//work
			plan.createAct("w", "10000", "0", "20", null, null, "2:30", "true");
			//leg to work
			leg = plan.createLeg("2", "car", null, null, null);
			route = new Route();
			route.setRoute("13 14 15 1");
			leg.setRoute(route);
			plan.createAct("h", "-25000", "0", "1", null, null, null, null);



			this.plans.addPerson(p);



		}


		PlansWriter pwriter = new PlansWriter(this.plans);
		pwriter.setWriterHandler(new PlansWriterHandlerImplV4());
		pwriter.write();


	}

	protected NetworkLayer loadNetwork(String filename) {
		// - read network: which buildertype??
		NetworkLayer network = new NetworkLayer();
		Gbl.getWorld().setNetworkLayer(network);

		new MatsimNetworkReader(network).readFile(filename);

		return network;
	}








	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			new Plansgenerator().createPlans();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
