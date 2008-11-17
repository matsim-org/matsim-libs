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

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationWriter;
import org.matsim.population.PopulationWriterHandlerImplV4;
import org.matsim.population.Route;
import org.matsim.population.RouteImpl;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;


/**
 * @author dgrether
 *
 */
public class Plansgenerator {

	private static final String network = "../matsimWithindayTesting/testdata/tests/withinday/network.xml";

	private static final String plansOut = "../matsimWithindayTesting/testdata/tests/withinday/newPlans.xml";


	private Population plans;

	private void init() {
		Config config = Gbl.createConfig(null);
		config.addCoreModules();

		config.plans().setOutputVersion("v4");
		config.plans().setOutputFile(plansOut);

		this.loadNetwork(network);
	}

	private void createPlans() throws Exception {
		init();
		this.plans = new Population(false);
		int homeEndtime = 6 * 3600;
		final Link link1 = ((NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE)).getLink(new IdImpl("1"));
		final Link link20 = ((NetworkLayer) Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE)).getLink(new IdImpl("20"));
		final Coord homeCoord = new CoordImpl(-25000, 0);
		final Coord workCoord = new CoordImpl(10000, 0);
		for (int i = 1; i <= 100; i++) {
			Person p = new PersonImpl(new IdImpl(i));
			Plan plan = new Plan(p);
			p.addPlan(plan);
			//home
			homeEndtime += 0.5 * 60;
			Act a = plan.createAct("h", homeCoord);
			a.setLink(link1);
			a.setEndTime(homeEndtime);
			//leg to work
			Leg leg = plan.createLeg(Mode.car);
			Route route = new RouteImpl();
			route.setRoute("2 4 5");
			leg.setRoute(route);
			//work
			a = plan.createAct("w", workCoord);
			a.setLink(link20);
			a.setDuration(2.5 * 3600);
			//leg to work
			leg = plan.createLeg(Mode.car);
			route = new RouteImpl();
			route.setRoute("13 14 15 1");
			leg.setRoute(route);
			a = plan.createAct("h", homeCoord);
			a.setLink(link1);



			this.plans.addPerson(p);



		}


		PopulationWriter pwriter = new PopulationWriter(this.plans);
		pwriter.setWriterHandler(new PopulationWriterHandlerImplV4());
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
