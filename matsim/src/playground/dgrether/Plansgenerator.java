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

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.population.PopulationWriterHandlerImplV4;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;


/**
 * @author dgrether
 *
 */
public class Plansgenerator {

	private static final String networkFilename = "../matsimWithindayTesting/testdata/tests/withinday/network.xml";

	private static final String plansOut = "../matsimWithindayTesting/testdata/tests/withinday/newPlans.xml";

	private NetworkLayer network;
	private Population plans;

	private void init() {
		Config config = Gbl.createConfig(null);
		config.addCoreModules();

		config.plans().setOutputVersion("v4");
		config.plans().setOutputFile(plansOut);

		this.network = this.loadNetwork(networkFilename);
	}

	private void createPlans() throws Exception {
		init();
		this.plans = new PopulationImpl();
		int homeEndtime = 6 * 3600;
		final LinkImpl link1 = network.getLink(new IdImpl("1"));
		final LinkImpl link20 = network.getLink(new IdImpl("20"));
		final Coord homeCoord = new CoordImpl(-25000, 0);
		final Coord workCoord = new CoordImpl(10000, 0);
		for (int i = 1; i <= 100; i++) {
			PersonImpl p = new PersonImpl(new IdImpl(i));
			PlanImpl plan = new org.matsim.core.population.PlanImpl(p);
			p.addPlan(plan);
			//home
			homeEndtime += 0.5 * 60;
			ActivityImpl a = plan.createActivity("h", homeCoord);
			a.setLink(link1);
			a.setEndTime(homeEndtime);
			//leg to work
			LegImpl leg = plan.createLeg(TransportMode.car);
			NetworkRoute route = new NodeNetworkRoute(link1, link20);
			route.setNodes(link1, NetworkUtils.getNodes(network, "2 4 5"), link20);
			leg.setRoute(route);
			//work
			a = plan.createActivity("w", workCoord);
			a.setLink(link20);
			a.setDuration(2.5 * 3600);
			//leg to work
			leg = plan.createLeg(TransportMode.car);
			route = new NodeNetworkRoute(link20, link1);
			route.setNodes(link20, NetworkUtils.getNodes(network, "13 14 15 1"), link1);
			leg.setRoute(route);
			a = plan.createActivity("h", homeCoord);
			a.setLink(link1);



			this.plans.addPerson(p);



		}


		PopulationWriter pwriter = new PopulationWriter(this.plans);
		pwriter.setWriterHandler(new PopulationWriterHandlerImplV4());
		pwriter.write();


	}

	protected NetworkLayer loadNetwork(final String filename) {
		// - read network: which buildertype??
		NetworkLayer network = new NetworkLayer();

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
