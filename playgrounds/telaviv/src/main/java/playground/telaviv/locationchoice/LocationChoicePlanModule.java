/* *********************************************************************** *
 * project: org.matsim.*
 * LocationChoicePlanModule.java
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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.telaviv.population.ParsedPerson;
import playground.telaviv.population.PersonFileParser;
import playground.telaviv.zones.ZoneMapping;

/*
 * Use fixed probabilities over the day which are read from a text file.
 */
public class LocationChoicePlanModule extends AbstractMultithreadedModule {

	private static final Logger log = Logger.getLogger(LocationChoicePlanModule.class);
	
	private String populationFile = "../../matsim/mysimulations/telaviv/population/PB1000_10.txt";
	
	private Scenario scenario;
	
	private LocationChoiceProbabilityCreator locationChoiceProbabilityCreator = null;
	private ZoneMapping zoneMapping = null;
	private Map<Id, List<Integer>> shoppingActivities = null;	// <PersonId, List<Index in the Plan's PlanElementsList>
	
	public LocationChoicePlanModule(Scenario scenario) {
		super(scenario.getConfig().global());
		this.scenario = scenario;
		
		log.info("Creating ZoneMapping...");
		zoneMapping = new ZoneMapping(scenario, TransformationFactory.getCoordinateTransformation("EPSG:2039", "WGS84"));
		log.info("done.");
		
		log.info("Creating LocationChoiceProbabilityCreator...");
		locationChoiceProbabilityCreator = new LocationChoiceProbabilityCreator(scenario);
		log.info("done.");
		
		log.info("Parsing population file...");
		Map<Integer, ParsedPerson> personMap;
		try {
			personMap = new PersonFileParser(populationFile, ",", true).readFile();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		log.info("done.");
		
		log.info("Filter persons with shopping activities...");
		filterPersons(personMap);
		log.info("done.");
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new LocationChoicePlanAlgorithm(scenario, locationChoiceProbabilityCreator, zoneMapping, 
				shoppingActivities);
	}

	public Map<Id, List<Integer>> getShoppingActivities() {
		return this.shoppingActivities;
	}
	
	private void filterPersons(Map<Integer, ParsedPerson> personMap) {
		shoppingActivities = new HashMap<Id, List<Integer>>();
		
		Iterator<ParsedPerson> iter = personMap.values().iterator();
		while (iter.hasNext()) {
			ParsedPerson emme2Person = iter.next();
			
			int primaryMainActivityType = emme2Person.MAINACTPRI;
			int secondaryMainActivityType = emme2Person.MAINACTSEC;
			
			/*
			 * Shopping is encoded as "4"
			 * If no Main Activity is Shopping, we remove the Agent from the Map.
			 */
			if (primaryMainActivityType != 4 && secondaryMainActivityType != 4) {
				iter.remove();
				continue;
			}
			
			boolean primaryPreStop = (emme2Person.INTSTOPPR == 2 || emme2Person.INTSTOPPR == 4);
			boolean primaryPostStop = (emme2Person.INTSTOPPR == 3 || emme2Person.INTSTOPPR == 4);
			boolean secondaryPreStop = (emme2Person.INTSTOPSEC == 2 || emme2Person.INTSTOPSEC == 4);
			boolean secondaryPostStop = (emme2Person.INTSTOPSEC == 3 || emme2Person.INTSTOPSEC == 4);
			
			List<Integer> indices = new ArrayList<Integer>();
			
			int index = 0;	// home activity is the first PlanElement
			if (primaryPreStop) index = index + 2;	// Activity before Primary Main Activity
			
			// Primary Main Activity
			index = index + 2;
			if (primaryMainActivityType == 4) indices.add(index);
			
			if (primaryPostStop) index = index + 2;	// Activity after Primary Main Activity
			
			index = index + 2;	// Home Activity after Main Trip
			
			if (secondaryPreStop) index = index + 2;	// Activity before Secondary Main Activity
			
			// Secondary Main Activity
			index = index + 2;
			if (secondaryMainActivityType == 4) indices.add(index);
			
			if (secondaryPostStop) index = index + 2;
			
			// Add Person Information to the Map
			Id<Person> id = Id.create(String.valueOf(emme2Person.PERSONID), Person.class);
			shoppingActivities.put(id, indices);
		}
	}
}