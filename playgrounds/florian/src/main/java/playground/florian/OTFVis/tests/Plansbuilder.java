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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;

public class Plansbuilder {

	private static final double SAMPLE_SIZE = 1.;
	private static final int KONT_PLAENE = 200;
	private static final int RUSH_PLAENE = 0;
	private static final String NETWORK = "./test/input/playground/florian/OTFVis/network.xml";
	private static final String OUTPUT_FOLDER = "./test/input/playground/florian/OTFVis";

	public static void main(String[] args) {

		// Oeffne Szenario
		ScenarioImpl sc = new ScenarioImpl();
		NetworkImpl net = sc.getNetwork();
		new MatsimNetworkReader(sc).readFile(NETWORK);
		Population pop = sc.getPopulation();
		PopulationFactory pb = pop.getFactory();

		// Gestalte kontinuierlichen Verkehr
		for(int i=0;i<(KONT_PLAENE+1);i++){
			Id id =new IdImpl(i);
			PersonImpl person = (PersonImpl) pb.createPerson(id);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			person.addPlan(plan);

			// Activity 1
//			Id linkId = new IdImpl((i % 4)+1);
//			Id linkId = new IdImpl((int)((Math.random()*15)+1));
			Id linkId = new IdImpl(6);
			Activity act = plan.createAndAddActivity("h", linkId);
			act.setEndTime(i*((6*60*60)/KONT_PLAENE));
			plan.addLeg(pb.createLeg(TransportMode.car));

			// Activity 2
			int j = (i % 4) + 3;
			if (j>4){j=j-4;}
//			Id linkId2 = new IdImpl(j);
			Id linkId2;
			do{
				linkId2 = new IdImpl((int)((Math.random()*15)+1));
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
			Id id =new IdImpl(i+KONT_PLAENE+1);
			PersonImpl person = (PersonImpl) pb.createPerson(id);
			PlanImpl plan = (PlanImpl) pb.createPlan();
			person.addPlan(plan);

			// Start Activity
			Id linkId = new IdImpl(15);
			Id linkId2 = new IdImpl(6);

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
		new PopulationWriter(pop,net,SAMPLE_SIZE).writeFile(OUTPUT);
		System.out.println("Output in " + OUTPUT + " is done");

	}

}
