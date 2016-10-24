/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.santiago.SantiagoScenarioConstants.SubpopulationName;
import playground.santiago.SantiagoScenarioConstants.SubpopulationValues;


public class PopulationAnalysis {

	private static final String runPath = "../../runs-svn/santiago/run11c/";
	
	private static final String analysisPath = runPath+ "analysis/";
	
	public static void main(String[] args) {
		
		createDir(new File(analysisPath));

		getPersonsWithNegativeScores();
		
		//Input
		Population pop1 = getPopulationWithCarLegWOCarAvail(runPath+ "input/plans_final.xml.gz", runPath + "input/agentAttributes.xml", 
				analysisPath + "carUseWOCarAvailable_InputData.txt");
		
		//Output
		Population pop2 = getPopulationWithCarLegWOCarAvail(runPath + "output/output_plans.xml.gz", runPath + "output/output_personAttributes.xml.gz", 
				analysisPath + "carUseWOCarAvailable_OutputData.txt");
		
		new PopulationWriter(pop1).write(analysisPath + "carUseWOCarAvailable_InputPopulation.xml");
		new PopulationWriter(pop2).write(analysisPath + "carUseWOCarAvailable_OutputPopulation.xml");
		
		System.out.println("### Done. ###");
		
	}
	
	private static void getPersonsWithNegativeScores(){
		
		String plansFile = runPath + "output/output_plans.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(plansFile);
		
		Set<Person> plansWithNegativeScores = new HashSet<Person>();
		
		for(Person person : scenario.getPopulation().getPersons().values()){
			
			if(person.getSelectedPlan().getScore() < 0){
				
				plansWithNegativeScores.add(person);
				
			}
			
		}
		BufferedWriter writer = IOUtils.getBufferedWriter(analysisPath + "negativeScores.txt");
		
		try {
			
			for(Person person : plansWithNegativeScores){
				
				writer.write(person.getId().toString() + "\t" + person.getSelectedPlan().getScore());
				
				writer.newLine();
				
			}
			
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

	private static Population getPopulationWithCarLegWOCarAvail(String plansFile, String attributesFile, String outputFile){
				
		Map<String, Boolean> agentIdString2CarAvail = new HashMap<String, Boolean>(); //car availability of all car users in selected plan
		Map<Id<Person>, Person> carUsersId2Person = new TreeMap<Id<Person>, Person>(); 
		Map<Id<Person>, Person> carUsersWoCarAvailable = new TreeMap<Id<Person>, Person>();
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(plansFile);
	
		//collect agents using car on their tour
		for(Person person : scenario.getPopulation().getPersons().values()){
			for (PlanElement pe : person.getSelectedPlan().getPlanElements()){
					if (pe instanceof Leg){
						Leg leg = (Leg) pe;
						if (leg.getMode() == TransportMode.car){ 
							agentIdString2CarAvail.put(person.getId().toString(), false); // Assumption, that carAvail is false; carAvail will be checked later.
							carUsersId2Person.put(person.getId(), person);
						}
					}
			}
		}
		System.out.println("Number of agents using car: " + agentIdString2CarAvail.size());
		
		//read attributes -- carAvail?
		ObjectAttributes attributes = new ObjectAttributes();
		ObjectAttributesXmlReader attrReader = new ObjectAttributesXmlReader(attributes);
		attrReader.readFile(attributesFile);
		
		//write car availability to car users.
		for (String agentIdString : agentIdString2CarAvail.keySet()) {
			System.out.println(agentIdString + ": " +  attributes.getAttribute(agentIdString , SubpopulationName.carUsers));
			boolean carAvail = SubpopulationValues.carAvail.equals(attributes.getAttribute(agentIdString , SubpopulationName.carUsers));
			agentIdString2CarAvail.put(agentIdString, carAvail);
		}
		
		//Count car users with car avail
		int countCarUserswithCarAvail = 0;
		for (String agentIdString : agentIdString2CarAvail.keySet()) {
			if (agentIdString2CarAvail.get(agentIdString) == true) {
				countCarUserswithCarAvail ++;
			}
		}
		System.out.println(countCarUserswithCarAvail + " of " + agentIdString2CarAvail.size() + " car users have a car available.");
		System.out.println("car user set size: " + carUsersId2Person.size());
		
		//separate all car users without car available
		for (Id<Person> carUserId : carUsersId2Person.keySet()) {
			if (agentIdString2CarAvail.get(carUserId.toString()) == false) {
				carUsersWoCarAvailable.put(carUserId, carUsersId2Person.get(carUserId));
			}
		}
		
		//write list of car users without car available
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			writer.write("There are " + carUsersWoCarAvailable.size() + " agents using a car, but having no car available: ");
			writer.newLine();
			
			for (Id<Person> carUserWoCarAvailId : carUsersWoCarAvailable.keySet()) {
					writer.write(carUserWoCarAvailId.toString());
					writer.newLine();
			}
			writer.flush();
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//write population of car users without car available
		Population carUsersWoCarPop = scenario.getPopulation() ;
		carUsersWoCarPop.getPersons().clear();
		for (Person carUserWoCarAvail : carUsersWoCarAvailable.values()){
			carUsersWoCarPop.addPerson(carUserWoCarAvail);
		}
		
		return carUsersWoCarPop;
		
	}
	
	private static void createDir(File file) {
		System.out.println("Directory " + file + " created: "+ file.mkdirs());	
	}
	
}