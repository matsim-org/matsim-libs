/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPlansGenerator.java
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

package playground.gregor.evacuation.scenarioGenerator;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.utils.misc.Time;
import org.matsim.world.World;

public class EvacuationPlansGenerator {
	
	private static Logger log = Logger.getLogger(EvacuationPlansGenerator.class);
	private final Population plans;
	private final NetworkLayer network;
	private final static String saveLinkId = "el1";
	
	public EvacuationPlansGenerator(Population population, NetworkLayer network) {
		this.plans = population;
		this.network = network;
				
	}

	private void run() {
		
		createEvacuationPlans();
		
	}
	
	
	/**
	 * Generates an evacuation plan for all agents inside the evacuation area.
	 * Agents outside the evacuation are will be removed from the plans.
	 *
	 * @param plans
	 * @param network
	 */
	private void createEvacuationPlans() {

		PlansCalcRoute router = new PlansCalcRoute(network, new FreespeedTravelTimeCost(), new FreespeedTravelTimeCost());

		/* all persons that want to start on an already deleted link will be excluded from the
		 *simulation.     */
		log.info("  - removing all persons outside the evacuation area");
		Iterator<Person> it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			Id id = ((Act)pers.getPlans().get(0).getActsLegs().get(0)).getLink().getId();

			if (network.getLink(id) == null) {
				it.remove();
			}
		}

		// the remaining persons plans will be routed
		log.info("  - generating evacuation plans for the remaining persons");
		it = plans.getPersons().values().iterator();
		while (it.hasNext()) {
			Person pers = it.next();

			if (pers.getPlans().size() != 1 ) {
				Gbl.errorMsg("For each agent only one initial evacuation plan is allowed!");
			}

			Plan plan = pers.getPlans().get(0);

			if (plan.getActsLegs().size() != 1 ) {
				Gbl.errorMsg("For each initial evacuation plan only one Act is allowed - and no Leg at all");
			}

			Leg leg = new Leg(1,"car",0.0,0.0,0.0);
			plan.addLeg(leg);

			Act actB = new Act("h", 12000.0, -12000.0, network.getLink(saveLinkId), Time.UNDEFINED_TIME, Time.UNDEFINED_TIME, 0.0, true);
			plan.addAct(actB);

			router.run(plan);
		}

	}
	public static void main(String [] args) {
		String plans = "./networks/padang_plans_v20080618_reduced.xml";
		String netfile = "./networks/padang_net_evac_v20080618.xml";
		String change = "./networks/padang_change_evac_v20080618.xml";
		String plansout = "./networks/padang_plans_v20080618_reduced_it0.xml";
		
		World world = Gbl.createWorld();
		Gbl.createConfig(null);
		
		log.info("loading network from " + netfile);
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		new MatsimNetworkReader(network).readFile(netfile);
//		network.setNetworkChangeEvents(new NetworkChangeEventsParser(network).parseEvents(change));
		world.setNetworkLayer(network);
		world.complete();
		log.info("done.");
		

		log.info("loading population from " + plans);
		Population population = new Population(false);
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(plans);
		log.info("done.");
		
		new EvacuationPlansGenerator(population,network).run();
		
		new PopulationWriter(population,plansout, "v4").write();
		
	}


}
