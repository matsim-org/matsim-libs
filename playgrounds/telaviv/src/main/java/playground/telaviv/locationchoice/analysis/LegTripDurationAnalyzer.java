/* *********************************************************************** *
 * project: org.matsim.*
 * ShoppingLegTripDurationAnalyzer.java
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

package playground.telaviv.locationchoice.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.core.utils.misc.RouteUtils;

import playground.telaviv.locationchoice.ExtendedLocationChoicePlanModule;

public class LegTripDurationAnalyzer implements AgentDepartureEventHandler, AgentArrivalEventHandler {

	private static final Logger log = Logger.getLogger(LegTripDurationAnalyzer.class);
	
	private static String basePath = "../../matsim/mysimulations/telaviv/";
//	private static String runPath = "output_JDEQSim_with_location_choice/";
	private static String runPath = "output_JDEQSim_with_location_choice_without_TravelTime/";
//	private static String runPath = "output_without_location_choice_0.10/";
	
	private static String networkFile = basePath + "input/network.xml";
	private static String populationFile = basePath + runPath + "ITERS/it.100/100.plans.xml.gz";
	private String eventsFile = basePath + runPath + "ITERS/it.100/100.events.txt.gz";
	
	private String shoppingOutFileCar = basePath + runPath + "100.shoppingLegsCar.txt";
	private String otherOutFileCar = basePath + runPath + "100.otherLegsCar.txt";
	private String workOutFileCar = basePath + runPath + "100.workLegsCar.txt";
	private String educationOutFileCar = basePath + runPath + "100.educationLegsCar.txt";

	private String delimiter = "\t";
	private Charset charset = Charset.forName("UTF-8");
	
	private Scenario scenario;
	
	private Map<Id, List<Integer>> shoppingActivities;	// <PersonId, List<Index of Shopping Activity>>
	private Map<Id, List<Integer>> otherActivities;	// <PersonId, List<Index of Other Activity>
	private Map<Id, List<Integer>> workActivities;	// <PersonId, List<Index of Work Activity>>
	private Map<Id, List<Integer>> educationActivities;	// <PersonId, List<Index of Education Activity>>
	
	private Map<Id, Integer> legCounter;	// <PersonId, currently performed Leg Index>
	private Map<Id, Integer> shoppingLegCounter;	// <PersonId, currently performed Leg Index>
	private Map<Id, Integer> otherLegCounter;	// <PersonId, currently performed Leg Index>
	private Map<Id, Integer> workLegCounter;	// <PersonId, currently performed Leg Index>
	private Map<Id, Integer> educationLegCounter;	// <PersonId, currently performed Leg Index>
	
	private Map<Id, Double> departures;
	
	private List<Leg> shoppingToLeg;
	private List<Leg> shoppingFromLeg;
	private List<Leg> otherToLeg;
	private List<Leg> otherFromLeg;
	private List<Leg> workToLeg;
	private List<Leg> workFromLeg;
	private List<Leg> educationToLeg;
	private List<Leg> educationFromLeg;
	
	public static void main(String[] args) {
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		// load network
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		// load population
		new MatsimPopulationReader(scenario).readFile(populationFile);

		new LegTripDurationAnalyzer(scenario);
	}
	
	public LegTripDurationAnalyzer(Scenario scenario) {
		this.scenario = scenario;
		
		log.info("Identifying activities...");
		ExtendedLocationChoicePlanModule elcpm = new ExtendedLocationChoicePlanModule(scenario, null);
		shoppingActivities = elcpm.getShoppingActivities();
		otherActivities = elcpm.getOtherActivities();
		workActivities = elcpm.getWorkActivities();
		educationActivities = elcpm.getEducationActivities();
		log.info("done. Found " + shoppingActivities.size() + " shopping Activities.");
		log.info("done. Found " + otherActivities.size() + " other Activities.");
		log.info("done. Found " + workActivities.size() + " work Activities.");
		log.info("done. Found " + educationActivities.size() + " education Activities.");
		
		log.info("reading events...");
		readEvents();
		log.info("done.");
		
		/*
		 * Error checking - did we miss some Trips???
		 */
		for (Entry<Id, List<Integer>> entry : shoppingActivities.entrySet()) {
			if (entry.getValue().size() > 0) {
				log.error("Why are shopping indices left??? " + entry.getKey());
			}
		}
		for (Entry<Id, List<Integer>> entry : otherActivities.entrySet()) {
			if (entry.getValue().size() > 0) {
				log.error("Why are other indices left??? " + entry.getKey());
			}
		}
		for (Entry<Id, List<Integer>> entry : workActivities.entrySet()) {
			if (entry.getValue().size() > 0) {
				log.error("Why are work indices left??? " + entry.getKey());
			}
		}
		for (Entry<Id, List<Integer>> entry : educationActivities.entrySet()) {
			if (entry.getValue().size() > 0) {
				log.error("Why are education indices left??? " + entry.getKey());
			}
		}
		
		analyzeResults(shoppingToLeg, shoppingFromLeg, "shopping");
		analyzeResults(otherToLeg, otherFromLeg, "other");
		analyzeResults(workToLeg, workFromLeg, "work");
		analyzeResults(educationToLeg, educationFromLeg, "education");
		
		log.info("Writing car shopping legs to file...");
		List<Leg> shoppingLegsCar = new ArrayList<Leg>();
		for (Leg leg : shoppingToLeg) if (leg.getMode().equals(TransportMode.car)) shoppingLegsCar.add(leg);
		for (Leg leg : shoppingFromLeg) if (leg.getMode().equals(TransportMode.car)) shoppingLegsCar.add(leg);
		writeFile(shoppingLegsCar, shoppingOutFileCar);
		log.info("done.");
		
		log.info("Writing car other legs to file...");
		List<Leg> otherLegsCar = new ArrayList<Leg>();
		for (Leg leg : otherToLeg) if (leg.getMode().equals(TransportMode.car)) otherLegsCar.add(leg);
		for (Leg leg : otherFromLeg) if (leg.getMode().equals(TransportMode.car)) otherLegsCar.add(leg);
		writeFile(otherLegsCar, otherOutFileCar);
		log.info("done.");
		
		log.info("Writing car work legs to file...");
		List<Leg> workLegsCar = new ArrayList<Leg>();
		for (Leg leg : workToLeg) if (leg.getMode().equals(TransportMode.car)) workLegsCar.add(leg);
		for (Leg leg : workFromLeg) if (leg.getMode().equals(TransportMode.car)) workLegsCar.add(leg);
		writeFile(workLegsCar, workOutFileCar);
		log.info("done.");
		
		log.info("Writing car education legs to file...");
		List<Leg> educationLegsCar = new ArrayList<Leg>();
		for (Leg leg : educationToLeg) if (leg.getMode().equals(TransportMode.car)) educationLegsCar.add(leg);
		for (Leg leg : educationFromLeg) if (leg.getMode().equals(TransportMode.car)) educationLegsCar.add(leg);
		writeFile(educationLegsCar, educationOutFileCar);
		log.info("done.");
	}
	
	private void readEvents() {	
		reset(0);
		
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(this);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(eventsFile);
	}

	private void writeFile(List<Leg> legs, String outFile) {
		FileOutputStream fos = null; 
		OutputStreamWriter osw = null; 
	    BufferedWriter bw = null;
		
	    try {
			fos = new FileOutputStream(outFile);
			osw = new OutputStreamWriter(fos, charset);
			bw = new BufferedWriter(osw);
			
			// write Header
			bw.write("depaturetime" + delimiter + "arrivaltime" + "\n");
			
			// write Values
			for (Leg leg : legs) {
				bw.write(String.valueOf(leg.getDepartureTime()));
				bw.write(delimiter);
				bw.write(String.valueOf(leg.getDepartureTime() + leg.getTravelTime()));
				bw.write("\n");
			}
			
			bw.close();
			osw.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		
		if (legCounter.containsKey(event.getPersonId())) {
			int count = legCounter.get(event.getPersonId());
			
			// increase leg count
			count++;
			legCounter.put(event.getPersonId(), count);
			
			departures.put(event.getPersonId(), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		
		List<Integer> shoppingIndices = null;
		List<Integer> otherIndices = null;
		List<Integer> workIndices = null;
		List<Integer> educationIndices = null;
		
		boolean handled = false;
		
		if (departures.get(event.getPersonId()) != null) {
			double departureTime = departures.remove(event.getPersonId());
			
			if (!handled && (shoppingIndices = shoppingActivities.get(event.getPersonId())) != null) {
				handled = handleArrival(event.getPersonId(), event.getTime(), departureTime, shoppingIndices, shoppingToLeg, shoppingFromLeg);			
			}
			
			if (!handled && (otherIndices = otherActivities.get(event.getPersonId())) != null) {
				handled = handleArrival(event.getPersonId(), event.getTime(), departureTime, otherIndices, otherToLeg, otherFromLeg);
			}
			
			if (!handled && (workIndices = workActivities.get(event.getPersonId())) != null) {
				handled = handleArrival(event.getPersonId(), event.getTime(), departureTime, workIndices, workToLeg, workFromLeg);
			}
			
			if (!handled && (educationIndices = educationActivities.get(event.getPersonId())) != null) {
				handled = handleArrival(event.getPersonId(), event.getTime(), departureTime, educationIndices, educationToLeg, educationFromLeg);
			}			
		}
		
		// increase leg count
		int count = legCounter.get(event.getPersonId());
		count++;
		legCounter.put(event.getPersonId(), count);
	}

	private boolean handleArrival(Id person_id, double time, double departureTime, List<Integer> indices, List<Leg> toLegs, List<Leg> fromLegs) {
		int count = legCounter.get(person_id);
		
		boolean handled = false;
		
		if (indices.contains(count + 1)) {
			Leg toLeg = (Leg) scenario.getPopulation().getPersons().get(person_id).getSelectedPlan().getPlanElements().get(count);
			toLeg.setDepartureTime(departureTime);
			toLeg.setTravelTime(time - departureTime);
			
			toLegs.add(toLeg);
			handled = true;
		}
		else if (indices.contains(count - 1)) {
			Leg fromLeg = (Leg) scenario.getPopulation().getPersons().get(person_id).getSelectedPlan().getPlanElements().get(count);
			fromLeg.setDepartureTime(departureTime);
			fromLeg.setTravelTime(time - departureTime);

			fromLegs.add(fromLeg);			
			indices.remove((Object)(count - 1));
			handled = true;
		}
		
		return handled;
	}
	
	@Override
	public void reset(int iteration) {
		legCounter = new HashMap<Id, Integer>();
		shoppingLegCounter = new HashMap<Id, Integer>();
		otherLegCounter = new HashMap<Id, Integer>();
		workLegCounter = new HashMap<Id, Integer>();
		educationLegCounter = new HashMap<Id, Integer>();
	
		for (Id id : scenario.getPopulation().getPersons().keySet()) legCounter.put(id, 0);
		for (Id id : shoppingActivities.keySet()) shoppingLegCounter.put(id, 0);
		for (Id id : otherActivities.keySet()) otherLegCounter.put(id, 0);
		for (Id id : workActivities.keySet()) workLegCounter.put(id, 0);
		for (Id id : educationActivities.keySet()) educationLegCounter.put(id, 0);
	
		departures = new HashMap<Id, Double>();
		
		shoppingToLeg = new ArrayList<Leg>();
		shoppingFromLeg = new ArrayList<Leg>();
		otherToLeg = new ArrayList<Leg>();
		otherFromLeg = new ArrayList<Leg>();
		workToLeg = new ArrayList<Leg>();
		workFromLeg = new ArrayList<Leg>();
		educationToLeg = new ArrayList<Leg>();
		educationFromLeg = new ArrayList<Leg>();
	}
	
	private void analyzeResults(List<Leg> toLegs, List<Leg> fromLegs, String activityType) {
		
		List<Leg> toLegsPt = new ArrayList<Leg>();
		List<Leg> toLegsCar = new ArrayList<Leg>();
		List<Leg> toLegsUndefined = new ArrayList<Leg>();
		List<Leg> fromLegsPt = new ArrayList<Leg>();
		List<Leg> fromLegsCar = new ArrayList<Leg>();
		List<Leg> fromLegsUndefined = new ArrayList<Leg>();
		
		for (Leg leg : toLegs) {
			if (leg.getMode().equals(TransportMode.car)) toLegsCar.add(leg);
			else if (leg.getMode().equals(TransportMode.pt)) toLegsPt.add(leg);
			else toLegsUndefined.add(leg);
		}
		for (Leg leg : fromLegs) {
			if (leg.getMode().equals(TransportMode.car)) fromLegsCar.add(leg);
			else if (leg.getMode().equals(TransportMode.pt)) fromLegsPt.add(leg);
			else fromLegsUndefined.add(leg);			
		}
		
//		double toTravelTimesPt = 0.0;
		double toTravelTimesCar = 0.0;
//		double toTravelTimesUndefined = 0.0;
		
//		double toDistancesPt = 0.0;
		double toDistancesCar = 0.0;
//		double toDistancesUndefined = 0.0;
		
//		double fromTravelTimesPt = 0.0;
		double fromTravelTimesCar = 0.0;
//		double fromTravelTimesUndefined = 0.0;
		
//		double fromDistancesPt = 0.0;
		double fromDistancesCar = 0.0;
//		double fromDistancesUndefined = 0.0;
		
//		for (Leg leg : toLegsPt) {
//			toTravelTimesPt = toTravelTimesPt + leg.getTravelTime();
//			toDistancesPt = toDistancesPt + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of " + activityType + "-to-legsPt = " + toLegsPt.size());
//		log.info("mean " + activityType + "-to-traveltimesPt = " + toTravelTimesPt / toLegsPt.size());
//		log.info("mean " + activityType + "-to-distancesPt = " + toDistancesPt / toLegsPt.size());
		log.info("");
		
		for (Leg leg : toLegsCar) {
			toTravelTimesCar = toTravelTimesCar + leg.getTravelTime();
			toDistancesCar = toDistancesCar + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
		}
		log.info("number of " + activityType + "-to-legsCar = " + toLegsCar.size());
		log.info("mean " + activityType + "-to-traveltimesCar = " + toTravelTimesCar / toLegsCar.size());
		log.info("mean " + activityType + "-to-distancesCar = " + toDistancesCar / toLegsCar.size());
		log.info("");
		
//		for (Leg leg : toLegsUndefined) {
//			toTravelTimesUndefined = toTravelTimesUndefined + leg.getTravelTime();
//			toDistancesUndefined = toDistancesUndefined + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of " + activityType + "-to-legsUndefined = " + toLegsUndefined.size());
//		log.info("mean " + activityType + "-to-traveltimesUndefined = " + toTravelTimesUndefined / toLegsUndefined.size());
//		log.info("mean " + activityType + "-to-distancesUndefined = " + toDistancesUndefined / toLegsUndefined.size());
		log.info("");
		
//		for (Leg leg : fromLegsPt) {
//			fromTravelTimesPt = fromTravelTimesPt + leg.getTravelTime();
//			fromDistancesPt = fromDistancesPt + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of " + activityType + "-from-legsPt = " + fromLegsPt.size());
//		log.info("mean " + activityType + "-from-traveltimesPt = " + fromTravelTimesPt / fromLegsPt.size());
//		log.info("mean " + activityType + "-from-distancesPt = " + fromDistancesPt / fromLegsPt.size());
		log.info("");
		
		for (Leg leg : fromLegsCar) {
			fromTravelTimesCar = fromTravelTimesCar + leg.getTravelTime();
			fromDistancesCar = fromDistancesCar + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
		}
		
		log.info("number of " + activityType + "-from-legsCar = " + fromLegsCar.size());
		log.info("mean " + activityType + "-from-traveltimesCar = " + fromTravelTimesCar / fromLegsCar.size());
		log.info("mean " + activityType + "-from-distancesCar = " + fromDistancesCar / fromLegsCar.size());
		log.info("");
		
//		for (Leg leg : fromLegsUndefined) {
//			fromTravelTimesUndefined = fromTravelTimesUndefined + leg.getTravelTime();
//			fromDistancesUndefined = fromDistancesUndefined + RouteUtils.calcDistance((NetworkRoute)leg.getRoute(), scenario.getNetwork());
//		}
		log.info("number of " + activityType + "-from-legsUndefined = " + fromLegsUndefined.size());
//		log.info("mean " + activityType + "-from-traveltimesUndefined = " + fromTravelTimesUndefined / fromLegsUndefined.size());
//		log.info("mean " + activityType + "-from-distancesUndefined = " + fromDistancesUndefined / fromLegsUndefined.size());
		log.info("");
	}
}
