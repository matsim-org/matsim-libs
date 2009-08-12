/* *********************************************************************** *
 * project: org.matsim.*
 * BKickTestScenarioPlansGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.income;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationBuilder;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.households.HouseholdBuilder;
import org.matsim.households.Households;

import playground.dgrether.DgPaths;
import playground.dgrether.utils.IdFactory;


/**
 * @author dgrether
 *
 */
public class BKickTestScenarioPlansGenerator {
  
	private static final Logger log = Logger.getLogger(BKickTestScenarioPlansGenerator.class);
	
	private static final String NETWORK	 = DgPaths.SHAREDSVN + "studies/dgrether/oneRouteTwoModeIncomeTest/network.xml";
	
	private static final String PLANSOUT = DgPaths.SHAREDSVN + "studies/dgrether/oneRouteTwoModeIncomeTest/plans";
	
	private int plansCount = 100;
	
	private List<Id> idList = new ArrayList<Id>();
	
	private double medianIncome = 43;
	
	public BKickTestScenarioPlansGenerator() {
		IdFactory.generateIds(20, idList);
		
		ScenarioImpl sc = new ScenarioImpl();
		sc.getConfig().network().setInputFile(NETWORK);
		sc.getConfig().scenario().setUseHouseholds(true);
		ScenarioLoader scloader = new ScenarioLoader(sc);
		scloader.loadScenario();
		NetworkLayer net = sc.getNetwork();
		
		PopulationImpl pop = sc.getPopulation();
		PopulationBuilder b = pop.getBuilder();
		
		Households hhs = sc.getHouseholds();
		HouseholdBuilder hhbuilder = hhs.getBuilder();
				
		for (int i = 0; i < plansCount; i++) {
			Person p = b.createPerson(sc.createId(""+i));
			pop.addPerson(p);
			Plan plan = b.createPlan();
			//home
			ActivityImpl acth = new ActivityImpl("h", net.getLinks().get(idList.get(1)));
			plan.addActivity(acth);
			
			//work
			
			//home
				
			
			
			
		}
		String popfile = PLANSOUT + plansCount + ".xml";
		PopulationWriter writer = new PopulationWriter(pop, popfile);
		log.info("Written plans to: " + popfile);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new BKickTestScenarioPlansGenerator();
		
		
	}

}
