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
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mmoyo.Validators.PlanValidator;
import playground.mmoyo.utils.DataLoader;
import playground.mmoyo.utils.FirstPlansExtractor;
import playground.mmoyo.utils.PlansMerger;

public class OverDemandPlanCreator {
	private Population population;
	
	final String SEP = "_";
	final String home = "home";
	final String walk = "walk";
	private PlanValidator planValidator = new PlanValidator();
	
	public OverDemandPlanCreator(String planFile){
		this.population =  new DataLoader().readPopulation(planFile);		
	}

	public OverDemandPlanCreator(Population population){
		this.population = population;
	}
	
	public Population run(final int homePlanNum, final int cloneNum) {
		PopulationImpl outPop = new PopulationImpl(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())));

		if (!planValidator.hasSecqActLeg(this.population)) { 
			throw new RuntimeException("this may not work, it assumes that the first PlanElement is home!! what about fragmnted plans? or other plans at all?" );
		}	

		for (Person person : this.population.getPersons().values()) {
			// create and add "home" plan
			for (int i=0; i<homePlanNum;i++){
				createHomePlan(person);
			}
			
			//add the original
			outPop.addPerson(person);

			//create and add clones
			for (int i=0; i<cloneNum;i++){
				Id newId = new IdImpl(person.getId().toString() + SEP + (i+2));
				Person personClon = new PersonImpl(newId);
				for (Plan plan :person.getPlans()){
					personClon.addPlan(plan);
				}
				outPop.addPerson(personClon);
			}
			
		}

		return outPop;

	}
	
	private Population MergingPops(final String [] popsPaths, int homePlansNum, int clonNums){
		this.population = new PlansMerger().plansAggregator(popsPaths);
		this.population = run(homePlansNum,clonNums);
		return this.population;
	}

	/**
	 * Creates a plan whereby the agents stay at home the whole day
	 */
	private void createHomePlan(Person person){
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
		person.addPlan(homePlan);
	}
	
	public static void main(String[] args) {
		String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";

		Population multPop = null;
		String[] arraPopPaths = new String[6];
		
		//cad1   w6d0.0t1200_w10d0.0t240_w8d0.5t720
		/*
		arraPopPaths[0]= "I:/z_alltest5/output/routedPlan_walk6.0_dist0.0_tran1200.0.xml.gz";
		arraPopPaths[1]= "I:/z_alltest5/output/routedPlan_walk10.0_dist0.0_tran240.0.xml.gz";
		arraPopPaths[2]= "I:/z_alltest5/output/routedPlan_walk8.0_dist0.5_tran720.0.xml.gz";
		*/
		//cad2     w10d0.0t1020_w10d0.4t60_w8d0.0t900
		
		arraPopPaths[0]= "I:/z_alltest5/output/routedPlan_walk10.0_dist0.0_tran1020.0.xml.gz";
		arraPopPaths[1]= "I:/z_alltest5/output/routedPlan_walk10.0_dist0.4_tran60.0.xml.gz";
		arraPopPaths[2]= "I:/z_alltest5/output/routedPlan_walk8.0_dist0.0_tran900.0.xml.gz";
		
		
		//cad3    w10d0.2t780_w6d0.7t540_w8d0.4t60
		
		arraPopPaths[3]= "I:/z_alltest5/output/routedPlan_walk10.0_dist0.2_tran780.0.xml.gz";
		arraPopPaths[4]= "I:/z_alltest5/output/routedPlan_walk6.0_dist0.7_tran540.0.xml.gz";
		arraPopPaths[5]= "I:/z_alltest5/output/routedPlan_walk8.0_dist0.4_tran60.0.xml.gz";
		
		multPop =  new OverDemandPlanCreator(multPop).MergingPops(arraPopPaths, 1, 0);

		final NetworkImpl net = new DataLoader().readNetwork(networkFile);
		PopulationWriter popWriter;

		//write the plan with over demand
		popWriter= new PopulationWriter(multPop, net);
		//popWriter.write("../playgrounds/mmoyo/output/tmp/w6.0d0.0t1200.0_w10.0d0.0t240.0_w8.0d0.5t720.0_ver2_NoCLONES.xml.gz");
		//popWriter.write("../playgrounds/mmoyo/output/cadyts/w10d0.0t1020_w10d0.4t60_w8d0.0t900_NOCLONS.xml.gz");
		popWriter.write("../playgrounds/mmoyo/output/tmp/w10d0.0t1020_w10d0.4t60_w8d0.0t900_w10d0.2t780_w6d0.7t540_w8d0.4t60_NoClons.xml.gz");
		
		//write a sample
		popWriter = new PopulationWriter(new FirstPlansExtractor().run(multPop), net);
		popWriter.write("../playgrounds/mmoyo/output/cadyts/samplePlans.xml") ;
	}

}
