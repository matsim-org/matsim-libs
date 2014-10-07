/* *********************************************************************** *
 * project: org.matsim.*
 * OverDemandPlanCreator.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mmoyo.Validators.PlanValidator;
import playground.mmoyo.algorithms.PersonClonner;
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
        ScenarioImpl sc = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()));
        Population outPop = PopulationUtils.createPopulation(sc.getConfig(), sc.getNetwork());

		if (!planValidator.hasSecqActLeg(this.population)) { 
			throw new RuntimeException("this may not work, it assumes that the first PlanElement is home!! what about fragmnted plans? or other plans at all?" );
		}	

		HomePlanCreator homePlanCreator = new HomePlanCreator(this.population);
		PersonClonner clonner = new PersonClonner();
		for (Person person : this.population.getPersons().values()) {
			// create and add "home" plan
			for (int i=0; i<homePlanNum;i++){
				homePlanCreator.run(person);
			}
			
			//add the original
			outPop.addPerson(person);

			//create and add clones
			for (int i=0; i<cloneNum;i++){
				Id<Person> newId = Id.create(person.getId().toString() + SEP + (i+2), Person.class);
				/*old, simple, ineffective cloning 
				Person personClon = new PersonImpl(newId);
				for (Plan plan :person.getPlans()){
					personClon.addPlan(plan);
				}*/
				Person personClon = clonner.run(person, newId);
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

	public static void main(String[] args) {
		String networkFile;
		Population pop = null;
		String popFilePath = null;
		
		if (args.length>0){
			popFilePath= args[0];
			networkFile = args[1];
		}else{
			String[] popFilePathArray = new String[0];
			popFilePathArray[0]= "../../";
			popFilePathArray[1]= "../../";
			popFilePathArray[2]= "../../";
			networkFile = "../../";
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
