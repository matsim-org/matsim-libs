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

package playground.mmoyo.utils.calibration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;

import playground.mmoyo.utils.DataLoader;

public class OverDemandPlanCreator {
	private Population population;
	
	final String SEP = "_";
	final String home = "home";
	final String walk = "walk";
	
	public OverDemandPlanCreator(String planFile){
		this.population =  new DataLoader().readPopulation(planFile);		
	}

	public OverDemandPlanCreator(Population population){
		this.population = population;
	}
	
	public Population run(int cloneNum, int homePlanNum) {
		PopulationImpl outPop = new PopulationImpl(new ScenarioImpl());

		for (Person person : this.population.getPersons().values()) {
			// create and add "home" plan
			Plan homePlan = new PlanImpl();
			Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
			ActivityImpl homeAct = new ActivityImpl(home, homeCoord);
			homeAct.setEndTime(3600.0);
			homePlan.addActivity(homeAct);
			Leg leg = this.population.getFactory().createLeg("walk");
			leg.setTravelTime(10.0);
			homePlan.addLeg(leg);
			homeAct = new ActivityImpl(home, homeCoord);
			homeAct.setStartTime(85500.0);//85500 = 23:45 hr
			homePlan.addActivity(homeAct);
			for (int i=1; i<=homePlanNum;i++){
				person.addPlan(homePlan);	
			}
			
			//add the original
			outPop.addPerson(person);

			//create and add clones
			for (int i=1; i<=cloneNum;i++){
				Id newId = new IdImpl(person.getId().toString() + SEP + (i+1));
				Person personClon = new PersonImpl(newId);
				for (Plan plan :person.getPlans()){
					personClon.addPlan(plan);
				}
				outPop.addPerson(personClon);
			}
			
		}

		return outPop;

	}

	public static void main(String[] args) {
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";

		String popFile ="../playgrounds/mmoyo/output/doubPlan/routedPlan_walk10.0_dist0.0_tran1200.0.xml.gz";
		String outPlanFile = "../playgrounds/mmoyo/output/doubPlan/doubledPlan_walk10.0_dist0.0_tran1200.0.xml.gz";

		//String planFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/1plan.xml";
		//String outPlanFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/calibration/inputPlans/double1_plan.xml";

		ScenarioImpl scn = new DataLoader().readNetwork_Population(networkFile, popFile);
		Population multPop = new OverDemandPlanCreator(scn.getPopulation()).run(2,1);

		// write plan
		System.out.println("writing output plan file...");
		new PopulationWriter(multPop, scn.getNetwork()).write(outPlanFile);
		System.out.println("Done");
	}

}
