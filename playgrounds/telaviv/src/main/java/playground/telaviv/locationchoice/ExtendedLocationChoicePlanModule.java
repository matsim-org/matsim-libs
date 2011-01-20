/* *********************************************************************** *
 * project: org.matsim.*
 * ExtendedLocationChoicePlanModule
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

package playground.telaviv.locationchoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.telaviv.facilities.Emme2FacilitiesCreator;
import playground.telaviv.population.Emme2Person;
import playground.telaviv.population.Emme2PersonFileParser;
import playground.telaviv.zones.ZoneMapping;

/*
 * Calculates the probabilities dynamically, depending on the travel times
 * in the scenario and the departure time. 
 */
public class ExtendedLocationChoicePlanModule extends AbstractMultithreadedModule {

	private static final Logger log = Logger.getLogger(ExtendedLocationChoicePlanModule.class);
	
	private String populationFile = "../../matsim/mysimulations/telaviv/population/PB1000_10.txt";
	
	private Scenario scenario;
	
	private Emme2FacilitiesCreator facilitiesCreator = null;
	private ExtendedLocationChoiceProbabilityCreator extendedLocationChoiceProbabilityCreator = null;
	private ZoneMapping zoneMapping = null;
	private Map<Id, List<Integer>> shoppingActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Id, List<Integer>> otherActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Id, List<Integer>> workActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	private Map<Id, List<Integer>> educationActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
		
	public ExtendedLocationChoicePlanModule(Scenario scenario, PersonalizableTravelTime travelTime) {
		super(scenario.getConfig().global());
		this.scenario = scenario;
		
		log.info("Creating ZoneMapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		log.info("done.");
		
		log.info("Creating FacilitiesCreator...");
		facilitiesCreator = new Emme2FacilitiesCreator(scenario, zoneMapping);
		log.info("done.");
		
		log.info("Creating ExtendedLocationChoiceProbabilityCreator...");
		extendedLocationChoiceProbabilityCreator = new ExtendedLocationChoiceProbabilityCreator(scenario, travelTime);
		log.info("done.");
		
		log.info("Parsing population file...");
		Map<Integer, Emme2Person> personMap = new Emme2PersonFileParser(populationFile).readFile();
		log.info("done.");
		
		log.info("Filter persons with activities...");
		filterPersons(personMap);
		log.info("done.");
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ExtendedLocationChoicePlanAlgorithm(scenario, facilitiesCreator, extendedLocationChoiceProbabilityCreator, zoneMapping,
				shoppingActivities, otherActivities, workActivities, educationActivities);
	}

	@Override
	public void prepareReplanning() {
		super.prepareReplanning();
		
		extendedLocationChoiceProbabilityCreator.calculateDynamicProbabilities();
		extendedLocationChoiceProbabilityCreator.calculateTotalProbabilities();
	}
	
	public Map<Id, List<Integer>> getShoppingActivities() {
		return this.shoppingActivities;
	}
	
	public Map<Id, List<Integer>> getOtherActivities() {
		return this.otherActivities;
	}
	
	public Map<Id, List<Integer>> getWorkActivities() {
		return this.workActivities;
	}
	
	public Map<Id, List<Integer>> getEducationActivities() {
		return this.educationActivities;
	}
	
	/*
	 * Encoding: 
	 * none: 1
	 * work: 2
	 * education: 3
	 * shopping: 4
	 * other: 5
	 */
	private void filterPersons(Map<Integer, Emme2Person> personMap) {
		shoppingActivities = new HashMap<Id, List<Integer>>();
		otherActivities = new HashMap<Id, List<Integer>>();
		workActivities = new HashMap<Id, List<Integer>>();
		educationActivities = new HashMap<Id, List<Integer>>();
		
		Iterator<Emme2Person> iter = personMap.values().iterator();
		while (iter.hasNext()) {
			Emme2Person emme2Person = iter.next();
			
			int primaryMainActivityType = emme2Person.MAINACTPRI;
			int secondaryMainActivityType = emme2Person.MAINACTSEC;
			
			
//			/*
//			 * If no Main Activity is Shopping, we remove the Agent from the Map.
//			 */
//			if (primaryMainActivityType != 4 && secondaryMainActivityType != 4) {
//				iter.remove();
//				continue;
//			}
			
			boolean primaryPreStop = (emme2Person.INTSTOPPR == 2 || emme2Person.INTSTOPPR == 4);
			boolean primaryPostStop = (emme2Person.INTSTOPPR == 3 || emme2Person.INTSTOPPR == 4);
			boolean secondaryPreStop = (emme2Person.INTSTOPSEC == 2 || emme2Person.INTSTOPSEC == 4);
			boolean secondaryPostStop = (emme2Person.INTSTOPSEC == 3 || emme2Person.INTSTOPSEC == 4);
			
			List<Integer> shoppingIndices = new ArrayList<Integer>();
			List<Integer> otherIndices = new ArrayList<Integer>();
			List<Integer> workIndices = new ArrayList<Integer>();
			List<Integer> educationIndices = new ArrayList<Integer>();
			
			int index = 0;	// home activity is the first PlanElement
			if (primaryPreStop) index = index + 2;	// Activity before Primary Main Activity
			
			// Primary Main Activity
			index = index + 2;
			if (primaryMainActivityType == 2) workIndices.add(index);
			else if (primaryMainActivityType == 3) educationIndices.add(index);
			else if (primaryMainActivityType == 4) shoppingIndices.add(index);
			else if (primaryMainActivityType == 5) otherIndices.add(index);
			
			if (primaryPostStop) index = index + 2;	// Activity after Primary Main Activity
			
			index = index + 2;	// Home Activity after Main Trip
			
			if (secondaryPreStop) index = index + 2;	// Activity before Secondary Main Activity
			
			// Secondary Main Activity
			index = index + 2;
//			if (secondaryMainActivityType == 4) shoppingIndices.add(index);
			if (secondaryMainActivityType == 2) workIndices.add(index);
			else if (secondaryMainActivityType == 3) educationIndices.add(index);
			else if (secondaryMainActivityType == 4) shoppingIndices.add(index);
			else if (secondaryMainActivityType == 5) otherIndices.add(index);
			
			if (secondaryPostStop) index = index + 2;
			
			// Add Person Information to the Map
			Id id = scenario.createId(String.valueOf(emme2Person.PERSONID));
			
			shoppingActivities.put(id, shoppingIndices);
			otherActivities.put(id, otherIndices);
			workActivities.put(id, workIndices);
			educationActivities.put(id, educationIndices);
		}
	}	
}
