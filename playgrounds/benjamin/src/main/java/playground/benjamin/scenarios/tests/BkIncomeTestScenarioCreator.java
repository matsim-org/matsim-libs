/* *********************************************************************** *
 * project: org.matsim.*																															*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.benjamin.BkPaths;


/**
 * @author dgrether
 *
 */
public class BkIncomeTestScenarioCreator {

	private static final Logger log = Logger.getLogger(BkIncomeTestScenarioCreator.class);

	private final Id<Link> id1 = Id.create(1, Link.class);
	private final Id<Link> id2 = Id.create(2, Link.class);
	private final Id<Link> id3 = Id.create(3, Link.class);
	private final Id<Link> id4 = Id.create(4, Link.class);
	private final Id<Link> id5 = Id.create(5, Link.class);
	private final Id<Link> id6 = Id.create(6, Link.class);
	private final Id<Link> id7 = Id.create(7, Link.class);

	private final MutableScenario scenario;

	public BkIncomeTestScenarioCreator(MutableScenario scenario) {
		this.scenario = scenario;
	}


	public Population createPlans() {
		double firstHomeEndTime = 6.0 * 3600.0;
		double homeEndTime = firstHomeEndTime;
		log.info("starting plans creation...");

		Population pop = this.scenario.getPopulation();
		PopulationFactory builder = pop.getFactory();

		for (int i = 1; i <= 2000; i++) {
			Person p = builder.createPerson(Id.create(i, Person.class));

			//adding carPlan to person
			Plan plan = builder.createPlan();
			p.addPlan(plan);

			Activity act1 = builder.createActivityFromLinkId("h", id1);
			act1.setEndTime(homeEndTime);
			plan.addActivity(act1);

			Leg leg1Car = builder.createLeg(TransportMode.car);
			LinkNetworkRouteImpl routeCar = new LinkNetworkRouteImpl(id1, id4);
			//this would be so nice
			List<Id<Link>> linkidsCar = new ArrayList<Id<Link>>();
			linkidsCar.add(id2);
			linkidsCar.add(id3);
			routeCar.setLinkIds(id1, linkidsCar, id4);
			leg1Car.setRoute(routeCar);
			plan.addLeg(leg1Car);

			Activity act2 = builder.createActivityFromLinkId("w", id4);
			act2.setStartTime(7.0 * 3600.0);
			act2.setEndTime(15.0 * 3600.0);
//			act2.setDuration(8.0 * 3600.0);
			plan.addActivity(act2);

			Leg leg2Car = builder.createLeg(TransportMode.car);
			routeCar = new LinkNetworkRouteImpl(id4, id1);
			//in a beautiful world we would do...
			linkidsCar = new ArrayList<Id<Link>>();
			linkidsCar.add(id5);
			linkidsCar.add(id6);
			linkidsCar.add(id7);
			routeCar.setLinkIds(id4, linkidsCar, id1);
			leg2Car.setRoute(routeCar);
			plan.addLeg(leg2Car);

			Activity act3 = builder.createActivityFromLinkId("h", id1);
			plan.addActivity(act3);

			//adding ptPlan to person
			plan = builder.createPlan();
			p.addPlan(plan);

			plan.addActivity(act1);

			Leg leg1Pt = builder.createLeg(TransportMode.pt);
//			BasicRouteImpl routePt = new BasicRouteImpl(id1, id4);
//			List<Id> linkidsPt = new ArrayList<Id>();
//			routePt.setLinkIds(linkidsPt);
//			leg1Pt.setRoute(routePt);
			plan.addLeg(leg1Pt);

			plan.addActivity(act2);

			Leg leg2Pt = builder.createLeg(TransportMode.pt);
//			routePt = new BasicRouteImpl(id4, id1);
//			linkidsPt = new ArrayList<Id>();
//			routePt.setLinkIds(linkidsPt);
//			leg2Pt.setRoute(routePt);
			plan.addLeg(leg2Pt);

			plan.addActivity(act3);

			pop.addPerson(p);
//			homeEndTime++;
		}
		log.info("created population...");
		return pop;
	}


	/**
	 * @param args
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		String outfile = BkPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/plans.xml";
		String networkFile = BkPaths.SHAREDSVN + "studies/bkick/oneRouteTwoModeIncomeTest/network.xml";
		Network uselessNetwork = scenario.getNetwork();
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

		BkIncomeTestScenarioCreator pc = new BkIncomeTestScenarioCreator(scenario);
		Population pop = pc.createPlans();
		new PopulationWriter(pop, uselessNetwork).write(outfile);
		log.info("plans written");
		log.info("finished!");
	}

}
