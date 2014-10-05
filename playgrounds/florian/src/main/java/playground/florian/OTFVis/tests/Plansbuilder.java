/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.florian.OTFVis.tests;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class Plansbuilder {

	private static final double SAMPLE_SIZE = 1.;
	private static final int KONT_PLAENE = 200;
	private static final int RUSH_PLAENE = 0;
	private static final String NETWORK = "./test/input/playground/florian/OTFVis/network.xml";
	private static final String OUTPUT_FOLDER = "./test/input/playground/florian/OTFVis";

	public static void main(String[] args) {

		// Oeffne Szenario
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(NETWORK);
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		// Gestalte kontinuierlichen Verkehr
		for(int i=0;i<(KONT_PLAENE+1);i++){
			Id<Person> id =Id.create(i, Person.class);
			PersonImpl person = (PersonImpl) pb.createPerson(id);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			person.addPlan(plan);

			// Activity 1
//			Id linkId = Id.create((i % 4)+1);
//			Id linkId = Id.create((int)((Math.random()*15)+1));
			Id<Link> linkId = Id.create(6, Link.class);
			Activity act = plan.createAndAddActivity("h", linkId);
			act.setEndTime(i*((6*60*60)/KONT_PLAENE));
			plan.addLeg(pb.createLeg(TransportMode.car));

			// Activity 2
			int j = (i % 4) + 3;
			if (j>4){j=j-4;}
//			Id linkId2 = Id.create(j);
			Id<Link> linkId2;
			do{
				linkId2 = Id.create((int)((Math.random()*15)+1), Link.class);
			}while((linkId2.equals(linkId)));
			Activity act2 = plan.createAndAddActivity("w", linkId2);
			act2.setStartTime(i*((6*60*60)/KONT_PLAENE));
			act2.setEndTime(i*((6*60*60)/KONT_PLAENE)+6*60*60);
			plan.addLeg(pb.createLeg(TransportMode.car));

			// Activity 3
			Activity act3 = plan.createAndAddActivity("h", linkId);
			act3.setStartTime(i*((6*60*60)/KONT_PLAENE)+43200);

			pop.addPerson(person);
		}

		//	Gestalte RushHourVerkehr
		for(int i=0; i<(RUSH_PLAENE + 1);i++){
			Id<Person> id =Id.create(i+KONT_PLAENE+1, Person.class);
			PersonImpl person = (PersonImpl) pb.createPerson(id);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			person.addPlan(plan);

			// Start Activity
			Id<Link> linkId = Id.create(15, Link.class);
			Id<Link> linkId2 = Id.create(6, Link.class);

			Activity act = plan.createAndAddActivity("h", linkId);
			act.setEndTime(0);
			plan.addLeg(pb.createLeg(TransportMode.car));

			// Tages Activity
			for(int j = 0; j<16;j++){
				Activity act2 = plan.createAndAddActivity("w", linkId2);
				act2.setStartTime(j*3600);
				act2.setEndTime(j*3600 + 1800);
				plan.addLeg(pb.createLeg(TransportMode.car));

				Activity act2b = plan.createAndAddActivity("h", linkId);
				act2b.setStartTime(j*3600 + 1800);
				act2b.setEndTime(j*3600 + 3600);
				plan.addLeg(pb.createLeg(TransportMode.car));
			}

			// End Activity
			Activity act3 = plan.createAndAddActivity("w", linkId2);
			act3.setStartTime(61200);

			pop.addPerson(person);

		}

		// Output
		Long size = Math.round((KONT_PLAENE+RUSH_PLAENE) * SAMPLE_SIZE);
		String OUTPUT = OUTPUT_FOLDER + "/plans" + size.toString() + ".xml";
		new PopulationWriter(pop,net,SAMPLE_SIZE).write(OUTPUT);
		System.out.println("Output in " + OUTPUT + " is done");

	}

}
