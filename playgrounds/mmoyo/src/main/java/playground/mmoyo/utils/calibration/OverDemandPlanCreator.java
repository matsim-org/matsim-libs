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

import java.io.File;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
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
import playground.mmoyo.utils.FirstPersonsExtractor;
import playground.mmoyo.utils.PlansMerger;

/**creates clones and stay home plans for each person*/
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
	public void createHomePlan(Person person){
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
		String networkFile;
		Population pop = null;
		String popFilePath = null;
		
		if (args.length>0){
			popFilePath= args[0];
			networkFile = args[1];
		}else{
			networkFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			/*
			String[] popFilePathArray = new String[0];
			popFilePathArray[0]= "../../input/ro/routedPlan_walk6.0_dist0.0_tran1200.0.xml.gz";
			popFilePathArray[1]= "../../input/ro/routedPlan_walk8.0_dist0.5_tran720.0.xml.gz";
			popFilePathArray[2]= "../../input/ro/routedPlan_walk10.0_dist0.0_tran240.0.xml.gz";
			*/
			popFilePath = "../../input/juli/addhome/routedTrackedAndMerged.xml.gz";
		}
		
		DataLoader dataLoader = new DataLoader();
		Scenario scn = dataLoader.readNetwork_Population(networkFile, popFilePath); 
		
		pop = scn.getPopulation();
		pop =  new OverDemandPlanCreator(pop).run(1, 0);

		//write the plan with over demand
		Network net = scn.getNetwork();
		File file = new File(popFilePath);
		String outputFile = file.getParent() + File.separatorChar + file.getName() + "overDemandPlan.xml.gz";		
		PopulationWriter popWriter= new PopulationWriter(pop, net);
		popWriter.write(outputFile);	
		
		//write a sample
		popWriter = new PopulationWriter(new FirstPersonsExtractor().run(pop, 5), net);
		popWriter.write(file.getParent() + File.separatorChar + file.getName() + "overDemandPlanSample.xml") ;
	}

}
