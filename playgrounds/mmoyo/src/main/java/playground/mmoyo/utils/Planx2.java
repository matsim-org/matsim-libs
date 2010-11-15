/* *********************************************************************** *
 * project: org.matsim.*
 * Planx2.java
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

package playground.mmoyo.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

public class Planx2 {

	final String SEPARATOR = "_2";
	final String home = "home";
	final String walk = "walk";
	
	private void run(String planFile, String netFile, String outPlanFile) {
		DataLoader dataLoader = new DataLoader();
		Population population = dataLoader.readPopulation(planFile);
		NetworkImpl net = dataLoader.readNetwork(netFile);
		PopulationImpl newPopulation = new PopulationImpl(new ScenarioImpl());

		for (Person person : population.getPersons().values()) {
			// create "home" plan
			Plan homePlan = new PlanImpl();
			Coord homeCoord = ((ActivityImpl) person.getSelectedPlan()
					.getPlanElements().get(0)).getCoord();
			ActivityImpl homeAct = new ActivityImpl(home, homeCoord);
			homeAct.setEndTime(3600.0);
			homePlan.addActivity(homeAct);
			
			Leg leg = population.getFactory().createLeg("walk");
			leg.setTravelTime(10.0);
			homePlan.addLeg(leg);
			
			homeAct = new ActivityImpl(home, homeCoord);
			homeAct.setStartTime(85500.0);//85500 = 23:45 hr
			homePlan.addActivity(homeAct);

			person.addPlan(homePlan);

			Id newId = new IdImpl(person.getId().toString() + SEPARATOR);
			Person personClon = new PersonImpl(newId);
			personClon.addPlan(person.getSelectedPlan());
			personClon.addPlan(homePlan);

			newPopulation.addPerson(person);
			newPopulation.addPerson(personClon);
		}

		// write plan
		System.out.println("writing output plan file...");
		new PopulationWriter(newPopulation, net).write(outPlanFile);
		System.out.println("Done");

	}

	public static void main(String[] args) {
		String network = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";

//		String planFile ="../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/minTransfersRoutes_plan.xml.gz";
//		String outPlanFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/doubleMinTransfersRoutes_plan2.xml";

		String planFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/1plan.xml";
		String outPlanFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/double1_plan.xml";

		new Planx2().run(planFile, network, outPlanFile);
	}

}
