/* *********************************************************************** *
 * project: org.matsim.*
 * DetailedAgentsTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.Facility;
import org.matsim.households.Household;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.controler.EvacuationConstants;
import playground.christoph.evacuation.events.PersonInformationEvent;
import playground.christoph.evacuation.events.PersonInformationEventImpl;
import playground.christoph.evacuation.events.handler.PersonInformationEventHandler;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;

public class DetailedAgentsTracker implements GenericEventHandler, PersonInformationEventHandler,
		PersonDepartureEventHandler, PersonArrivalEventHandler, Wait2LinkEventHandler, LinkEnterEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		PersonStuckEventHandler, MobsimInitializedListener, IterationEndsListener {

	static final Logger log = Logger.getLogger(DetailedAgentsTracker.class);
	
	public static final String newLine = "\n";
	public static final String delimiter = "\t";
	
	private final Set<Id> A = new HashSet<Id>();
	private final Set<Id> A1 = new HashSet<Id>();
	private final Set<Id> A11 = new HashSet<Id>();
	private final Set<Id> A12 = new HashSet<Id>();
	private final Set<Id> A2 = new HashSet<Id>();
	private final Set<Id> A21 = new HashSet<Id>();
	private final Set<Id> A22 = new HashSet<Id>();
	
	private final Set<Id> B = new HashSet<Id>();
	private final Set<Id> B1 = new HashSet<Id>();
	private final Set<Id> B11 = new HashSet<Id>();
	private final Set<Id> B111 = new HashSet<Id>();
	private final Set<Id> B112 = new HashSet<Id>();
	private final Set<Id> B12 = new HashSet<Id>();
	private final Set<Id> B13 = new HashSet<Id>();
	private final Set<Id> B2 = new HashSet<Id>();
	
	private final Set<Id> C = new HashSet<Id>();
	private final Set<Id> C1 = new HashSet<Id>();
	private final Set<Id> C11 = new HashSet<Id>();
	private final Set<Id> C111 = new HashSet<Id>();
	private final Set<Id> C112 = new HashSet<Id>();
	private final Set<Id> C12 = new HashSet<Id>();
	private final Set<Id> C13 = new HashSet<Id>();
	private final Set<Id> C2 = new HashSet<Id>();
	
	private final Map<String, Integer> evacuationModesA11 = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesA21 = new TreeMap<String, Integer>();
	
	private final Map<String, Integer> evacuationModesB111Home = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesB111Evacuate = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesB112 = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesB12 = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesB2 = new TreeMap<String, Integer>();
	
	private final Map<String, Integer> evacuationModesC111Home = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesC111Evacuate = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesC112 = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesC12 = new TreeMap<String, Integer>();
	private final Map<String, Integer> evacuationModesC2 = new TreeMap<String, Integer>();
	
	private final Set<Id> informedAgents = new HashSet<Id>();
	private final Map<Id, Double> informationTime = new HashMap<Id, Double>();
	private final Set<Id> enrouteDrivers = new HashSet<Id>();
	private final Map<Id, Id> driverVehicles = new HashMap<Id, Id>();
	private final Map<Id, Id> vehiclePositions = new HashMap<Id, Id>();
	private final Map<Id, Collection<Id>> vehiclePassengers = new HashMap<Id, Collection<Id>>();
	
	private final Map<Id, Double> returnHomeTimes = new HashMap<Id, Double>();
	private final Map<Id, String> returnHomeModes = new HashMap<Id, String>();
	private final Map<Id, Double> evacuateFromHomeTimes = new HashMap<Id, Double>();
	private final Map<Id, String> evacuateFromHomeModes = new HashMap<Id, String>();
	private final Map<Id, Double> evacuateDirectlyTimes = new HashMap<Id, Double>();
	private final Map<Id, String> evacuateDirectlyModes = new HashMap<Id, String>();
	
	private final Set<Id> agentsInEvacuationArea = new HashSet<Id>();
	private final Map<Id, Double> leftEvacuationAreaTime = new HashMap<Id, Double>();
	
	private final Set<Id> stuckAgents = new HashSet<Id>();
	
	/*
	 * List of all activities and legs an agent performs after being informed. If the agent
	 * currently performs an activity/leg when being informed, the activity/leg is also included.
	 */
	private final Map<Id, List<Activity>> agentActivities = new HashMap<Id, List<Activity>>();
	private final Map<Id, List<Leg>> agentLegs = new HashMap<Id, List<Leg>>();
	
	private final Scenario scenario;
	private final HouseholdsTracker householdsTracker;
	private final DecisionDataProvider decisionDataProvider;
	private final CoordAnalyzer coordAnalyzer;
		
	private final List<String> headers = new ArrayList<String>();
	private final List<String> values = new ArrayList<String>();
	
	private final String outputCountsFile = "evacuationDecisionCounts.txt";
	private final String outputTimesFile = "evacuationDecisionTimes.txt";
	private final String outputModesFile = "evacuationDecisionModes.txt";
	private final String outputDetailsFile = "evacuationDecisionDetailed.txt";
	
	public DetailedAgentsTracker(Scenario scenario, HouseholdsTracker householdsTracker,
			DecisionDataProvider decisionDataProvider, CoordAnalyzer coordAnalyzer) {
		
		this.scenario = scenario;
		this.householdsTracker = householdsTracker;
		this.decisionDataProvider = decisionDataProvider;
		this.coordAnalyzer = coordAnalyzer;
	}
	
	@Override
	public void reset(int iteration) {
		// nothing to do here
	}

	/*
	 * PersonInformationEvent are not part of MATSim's default events. Therefore,
	 * the events file parser creates GenericEvents instead. Here, they are replaced
	 * with PersonInformationEvents.
	 */
	@Override
	public void handleEvent(GenericEvent event) {
		if (event.getEventType().equals(PersonInformationEvent.EVENT_TYPE)) {
			String personIdString = event.getAttributes().get(PersonInformationEvent.ATTRIBUTE_PERSON);
			Id<Person> personId = Id.create(personIdString, Person.class);
			handleEvent(new PersonInformationEventImpl(event.getTime(), personId));
		}
	}

	@Override
	public void handleEvent(PersonInformationEvent event) {
		Id personId = event.getPersonId();
		
		informedAgents.add(personId);
		informationTime.put(personId, event.getTime());
		
		AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
		Position position = agentPosition.getPositionType();
		if (position == Position.FACILITY) {
			Id householdId = householdsTracker.getPersonsHouseholdId(personId);
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
			Id homeFacilityId = hdd.getHomeFacilityId();
			
			if (agentPosition.getPositionId().equals(homeFacilityId)) A.add(personId);
			else {
				Facility facility = scenario.getActivityFacilities().getFacilities().get(agentPosition.getPositionId());
				boolean isAffected = coordAnalyzer.isFacilityAffected(facility);
				if (isAffected) B.add(personId);
				else C.add(personId);
			}
		} else if (position == Position.LINK) {
			Link link = scenario.getNetwork().getLinks().get(agentPosition.getPositionId());
			boolean isAffected = coordAnalyzer.isLinkAffected(link);
			if (isAffected) B.add(personId);
			else C.add(personId);
		} else if (position == Position.VEHICLE) {
			Id linkId = vehiclePositions.get(agentPosition.getPositionId());
			
			// If the vehicle has not been used so far, it is parked at the households home facility link.
			if (linkId == null) {
				Id householdId = householdsTracker.getPersonsHouseholdId(personId);
				HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
				linkId = hdd.getHomeLinkId();
			}
				
			Link link = scenario.getNetwork().getLinks().get(linkId);		
			boolean isAffected = coordAnalyzer.isLinkAffected(link);
			if (isAffected) B.add(personId);
			else C.add(personId);
		} else {
			log.warn("Undefined position of agent " + personId + " at time " + event.getTime());
		}

	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		Id personId = event.getPersonId();
		
		if (event.getLegMode().equals(TransportMode.car)) {
			this.enrouteDrivers.add(personId);
		}
//		this.enrouteDrivers.remove(event.getPersonId());
		
		List<Leg> legs = agentLegs.get(personId);
		
		LegImpl leg = new LegImpl(event.getLegMode());
        leg.setDepartureTime(event.getTime());
        leg.setArrivalTime(Time.UNDEFINED_TIME);
		
		legs.add(leg);
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		Id personId = event.getPersonId();

		this.enrouteDrivers.remove(event.getPersonId());
//		if (event.getLegMode().equals(TransportMode.car)) {
//			this.enrouteDrivers.add(personId);
//		}
		
		List<Leg> legs = agentLegs.get(personId);
		
		// If the agent is not informed yet, remove entries from the list.
		if (!informedAgents.contains(event.getPersonId())) legs.clear();
		else {
			LegImpl leg = (LegImpl) legs.get(legs.size() - 1);
			leg.setArrivalTime(event.getTime());			
		}
	}
	
	@Override
	public void handleEvent(Wait2LinkEvent event) {
		if (this.enrouteDrivers.contains(event.getPersonId())) {
			vehiclePositions.put(event.getVehicleId(), event.getLinkId());
			if (event.getVehicleId() == null) log.warn("null vehicleId was found!");			
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		
		Id personId = event.getPersonId();
		Id vehicleId = event.getVehicleId();
		Id linkId = event.getLinkId();
		
		boolean isDriver = this.enrouteDrivers.contains(personId);
		if (isDriver) {
			vehiclePositions.put(vehicleId, linkId);
			if (event.getVehicleId() == null) log.warn("null vehicleId was found!");
		}
		
		Link link = scenario.getNetwork().getLinks().get(linkId);
		boolean wasAffected = this.agentsInEvacuationArea.contains(personId);
		boolean isAffected = this.coordAnalyzer.isLinkAffected(link);
				
		checkAndUpdateAffected(personId, event.getTime(), wasAffected, isAffected);
		
		// if its a driver, also adapt passengers
		if (isDriver) {
			Collection<Id> passengers = this.vehiclePassengers.get(event.getVehicleId());
			for (Id passengerId : passengers) {
				checkAndUpdateAffected(passengerId, event.getTime(), wasAffected, isAffected);
			}
		}
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id personId = event.getPersonId();
		List<Activity> activities = agentActivities.get(personId);
				
        ActivityImpl activity = new ActivityImpl(event.getActType(), event.getLinkId());
        activity.setFacilityId(event.getFacilityId());
        activity.setStartTime(event.getTime());
        activity.setEndTime(Time.UNDEFINED_TIME);
		
		activities.add(activity);
		
		Facility facility = scenario.getActivityFacilities().getFacilities().get(event.getFacilityId());
		
		/*
		 * Workaround for runs 01a to 32a. There, dummy facility are used for pickup activities. They are not included in the
		 * facilities file and therefore would produce a null pointer exception. Therefore, we ignore those events.
		 */
		if (facility == null) {
			String facilityIdString = event.getFacilityId().toString();
			if (facilityIdString.contains("_pickup")) return;
		}
		
		boolean isAffected = this.coordAnalyzer.isFacilityAffected(facility);
		boolean wasAffected = this.agentsInEvacuationArea.contains(event.getPersonId());
		
		checkAndUpdateAffected(personId, event.getTime(), wasAffected, isAffected);
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id personId = event.getPersonId();
		List<Activity> activities = agentActivities.get(personId);
		
		// If the agent is not informed yet, remove entries from the list.
		if (!informedAgents.contains(event.getPersonId())) activities.clear();
		else {
			Activity activity = activities.get(activities.size() - 1);
			activity.setEndTime(event.getTime());			
		}
		
		Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
		boolean isAffected = this.coordAnalyzer.isLinkAffected(link);
		boolean wasAffected = this.agentsInEvacuationArea.contains(event.getPersonId());
		
		checkAndUpdateAffected(personId, event.getTime(), wasAffected, isAffected);
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Collection<Id> agentsInVehicle = this.vehiclePassengers.get(event.getVehicleId());
		if (agentsInVehicle == null) {
			agentsInVehicle = new LinkedHashSet<Id>();
			this.vehiclePassengers.put(event.getVehicleId(), agentsInVehicle);
		}
		agentsInVehicle.add(event.getPersonId());
		
		if (this.enrouteDrivers.contains(event.getPersonId())) this.driverVehicles.put(event.getPersonId(), event.getVehicleId());
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Collection<Id> agentsInVehicle = this.vehiclePassengers.get(event.getVehicleId());
		agentsInVehicle.remove(event.getPersonId());
		
		this.driverVehicles.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.stuckAgents.add(event.getPersonId());
		
		/*
		 * Seems that stuck events have not been created for passengers. Therefore,
		 * mark them manually as stuck. 
		 */
		if (this.enrouteDrivers.contains(event.getPersonId())) {
			Id vehicleId = this.driverVehicles.remove(event.getPersonId());
			Collection<Id> agentsInVehicle = this.vehiclePassengers.get(vehicleId);
			if (agentsInVehicle != null) this.stuckAgents.addAll(agentsInVehicle);
		}
	}
	
	private void checkAndUpdateAffected(Id personId, double time, boolean wasAffected, boolean isAffected) {	
		if (wasAffected && !isAffected) {
			this.leftEvacuationAreaTime.put(personId, time);
			this.agentsInEvacuationArea.remove(personId);
		}
		else if (!wasAffected && isAffected) {
			this.leftEvacuationAreaTime.remove(personId);
			this.agentsInEvacuationArea.add(personId);
		}
	}
	
	/*
	 * This cannot be implemented as a BeforeMobsimListener since the 
	 * DecisionDataProvider cannot provide any data at that time. 
	 * 
	 */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		
		this.agentActivities.clear();
		this.agentLegs.clear();
		
		// initialize agentActivities and agentLegs maps and set of affected agents
		for (Household household : ((ScenarioImpl) this.scenario).getHouseholds().getHouseholds().values()) {
			
			Id householdId = household.getId(); 
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			Id homeLinkId = hdd.getHomeLinkId();
			Id homeFacilityId = hdd.getHomeFacilityId();
			Facility homeFacility = this.scenario.getActivityFacilities().getFacilities().get(homeFacilityId);
			boolean isAffected = this.coordAnalyzer.isFacilityAffected(homeFacility);
			
			for (Id personId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(personId);
				Plan plan = person.getSelectedPlan();				
				
				ActivityImpl firstActivity = new ActivityImpl(((Activity) plan.getPlanElements().get(0)).getType(), homeLinkId);
				firstActivity.setFacilityId(homeFacilityId);
				firstActivity.setStartTime(0.0);
				firstActivity.setEndTime(Time.UNDEFINED_TIME);
				
				List<Activity> activities = new ArrayList<Activity>();
				activities.add(firstActivity);
				agentActivities.put(personId, activities);
				
				agentLegs.put(personId, new ArrayList<Leg>());
				
				if (isAffected) this.agentsInEvacuationArea.add(personId);
				
			}
			
		}
	}
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		this.analyzeResultsAndWriteFiles(event.getControler().getControlerIO());
	}
	
	public void analyzeResultsAndWriteFiles(OutputDirectoryHierarchy outputDirectoryHierarchy) {
		/*
		 * Create results.
		 */
		analyzeA();
		analyzeB();
		analyzeC();
		
		/*
		 * Write results to files.
		 */
		String countsFileName = outputDirectoryHierarchy.getIterationFilename(0, outputCountsFile);
		String timesFileName = outputDirectoryHierarchy.getIterationFilename(0, outputTimesFile);
		String modesFileName = outputDirectoryHierarchy.getIterationFilename(0, outputModesFile);
		String detailsFileName = outputDirectoryHierarchy.getIterationFilename(0, outputDetailsFile);
		writeResultsToFiles(countsFileName, timesFileName, modesFileName, detailsFileName);
	}
	
	private void analyzeA() {
		/*
		 * Split A into agents who's home facility is affected and is not affected.
		 * Then, split A1 and A2 into agents who stay at home and who stay not at home.
		 */
		for (Id personId : A) {
			AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
			Position position = agentPosition.getPositionType();
			Id householdId = householdsTracker.getPersonsHouseholdId(personId);
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);

			if (hdd.isHomeFacilityIsAffected()) {
				A1.add(personId);
				
				if (position == Position.FACILITY) {
					Id homeFacilityId = hdd.getHomeFacilityId();
					
					if (agentPosition.getPositionId().equals(homeFacilityId)) {
						A12.add(personId);
					}
					else {
						A11.add(personId);
						
						List<Leg> legs = agentLegs.get(personId);
						Set<String> modes = new TreeSet<String>();
						for (Leg leg : legs) modes.add(leg.getMode());
						String modesString = CollectionUtils.setToString(modes);
						this.evacuateFromHomeModes.put(personId, modesString);
						Integer count = evacuationModesA11.get(modesString);
						if (count == null) evacuationModesA11.put(modesString, 1);
						else evacuationModesA11.put(modesString, count + 1);
					}
				} else {
					A11.add(personId);
					
					List<Leg> legs = agentLegs.get(personId);
					Set<String> modes = new TreeSet<String>();
					for (Leg leg : legs) modes.add(leg.getMode());
					String modesString = CollectionUtils.setToString(modes);
					
					// only define the evacuation modes if the agent has left the evacuation area 
					if (this.leftEvacuationAreaTime.containsKey(personId)) {
						this.evacuateFromHomeModes.put(personId, modesString);
						Integer count = evacuationModesA11.get(modesString);
						if (count == null) evacuationModesA11.put(modesString, 1);
						else evacuationModesA11.put(modesString, count + 1);
					}
					
					log.warn("Agent is not in a facility when simulation ends: " + personId);
				}
			} else {
				A2.add(personId);
				
				if (position == Position.FACILITY) {
					Id homeFacilityId = hdd.getHomeFacilityId();
					
					if (agentPosition.getPositionId().equals(homeFacilityId)) {
						A22.add(personId);
					}
					else {
						A21.add(personId);
						
						List<Leg> legs = agentLegs.get(personId);
						Set<String> modes = new TreeSet<String>();
						for (Leg leg : legs) modes.add(leg.getMode());
						String modesString = CollectionUtils.setToString(modes);
						this.evacuateFromHomeModes.put(personId, modesString);
						Integer count = evacuationModesA21.get(modesString);
						if (count == null) evacuationModesA21.put(modesString, 1);
						else evacuationModesA21.put(modesString, count + 1);
					}
				} else {
					A21.add(personId);
					
					List<Leg> legs = agentLegs.get(personId);
					Set<String> modes = new TreeSet<String>();
					for (Leg leg : legs) modes.add(leg.getMode());
					String modesString = CollectionUtils.setToString(modes);
					
					// only define the evacuation modes if the agent has left the evacuation area
					if (this.leftEvacuationAreaTime.containsKey(personId)) {
						this.evacuateFromHomeModes.put(personId, modesString);
						Integer count = evacuationModesA21.get(modesString);
						if (count == null) evacuationModesA21.put(modesString, 1);
						else evacuationModesA21.put(modesString, count + 1);
					}
					
					log.warn("Agent is not in a facility when simulation ends: " + personId);
				}
			}
		}
		log.info("\tA\t" + "total at home:\t" + A.size());
		headers.add("A");
		values.add(String.valueOf(A.size()));
		
		log.info("\tA1\t" + "at home, home affected:\t" + A1.size());
		log.info("\tA11\t" + "at home, home affected, evacuate:\t" + A11.size());
		log.info("\tA12\t" + "at home, home affected, stay there:\t" + A12.size());
		headers.add("A1");
		headers.add("A11");
		headers.add("A12");
		values.add(String.valueOf(A1.size()));
		values.add(String.valueOf(A11.size()));
		values.add(String.valueOf(A12.size()));

		log.info("\tA2\t" + "at home, home not affected:\t" + A2.size());		
		log.info("\tA21\t" + "at home, home not affected, evacuate:\t" + A21.size());
		log.info("\tA22\t" + "at home, home not affected, stay there:\t" + A22.size());
		headers.add("A2");
		headers.add("A21");
		headers.add("A22");
		values.add(String.valueOf(A2.size()));
		values.add(String.valueOf(A21.size()));
		values.add(String.valueOf(A22.size()));
		
		for (String modes : evacuationModesA11.keySet()) {
			log.info("\tA11\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesA11.get(modes));			
		}
		for (String modes : evacuationModesA21.keySet()) {
			log.info("\tA21\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesA21.get(modes));			
		}
	}
	
	private void analyzeB() {
		
		for (Id personId : B) {
			AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
			Position position = agentPosition.getPositionType();
			
			Id householdId = householdsTracker.getPersonsHouseholdId(personId);
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
			Id homeFacilityId = hdd.getHomeFacilityId();
			
			if (hdd.isHomeFacilityIsAffected()) {
				B1.add(personId);
				
				if (position == Position.FACILITY) {
										
					List<Activity> list = this.agentActivities.get(personId);
					List<Activity> copy = new ArrayList<Activity>();
					
					// ignore pick-up and drop-off activities
					for (Activity activity : list) {
						if (activity.getType().equals(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE)) continue;
						else if (activity.getType().equals(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE)) continue;
						
						copy.add(activity);
					}
					
					boolean returnsHome = false;
					boolean endsAtHome = false;
					for (int i = 0; i < copy.size(); i++) {
						Activity activity = copy.get(i);
						if (activity.getFacilityId().equals(homeFacilityId)) returnsHome = true;
					}
					Activity lastActivity = copy.get(copy.size() - 1);
					if (lastActivity.getFacilityId().equals(homeFacilityId)) endsAtHome = true;
					
					if (returnsHome || endsAtHome) {
						B11.add(personId);
						if (endsAtHome) {
							B112.add(personId);
							
							List<Leg> legs = agentLegs.get(personId);
							Set<String> modes = new TreeSet<String>();
							for (Leg leg : legs) modes.add(leg.getMode());
							String modesString = CollectionUtils.setToString(modes);
							this.returnHomeModes.put(personId, modesString);
							this.returnHomeTimes.put(personId, lastActivity.getStartTime());
							Integer count = evacuationModesB112.get(modesString);
							if (count == null) evacuationModesB112.put(modesString, 1);
							else evacuationModesB112.put(modesString, count + 1);
						}
						else {
							B111.add(personId);
							
							/*
							 * Split "returning home" and "evacuate from home" trips.
							 */
							boolean foundHomeActivity = false;
							double endHomeActivityTime = Double.MAX_VALUE;
							for (int i = copy.size() - 1; i >= 0; i--) {
								Activity activity = copy.get(i);
								if (activity.getType().equals(EvacuationConstants.MEET_ACTIVITY)) {
									endHomeActivityTime = activity.getEndTime();
									this.returnHomeTimes.put(personId, activity.getStartTime());
									foundHomeActivity = true;
									break;
								}
							}
							if (!foundHomeActivity) {
								throw new RuntimeException("Could not identify agent's return home activity: " + personId);
							}
									
							List<Leg> legs = agentLegs.get(personId);
							Set<String> modesHome = new TreeSet<String>();
							Set<String> modesEvacuate = new TreeSet<String>();
							
							for (Leg leg : legs) {
								if (leg.getDepartureTime() < endHomeActivityTime) modesHome.add(leg.getMode());
								else modesEvacuate.add(leg.getMode());
							}
							
							String modesString;
							Integer count;
							
							modesString = CollectionUtils.setToString(modesHome);
							this.returnHomeModes.put(personId, modesString);
							count = evacuationModesB111Home.get(modesString);
							if (count == null) evacuationModesB111Home.put(modesString, 1);
							else evacuationModesB111Home.put(modesString, count + 1);
							
							modesString = CollectionUtils.setToString(modesEvacuate);
							this.evacuateFromHomeModes.put(personId, modesString);
							count = evacuationModesB111Evacuate.get(modesString);
							if (count == null) evacuationModesB111Evacuate.put(modesString, 1);
							else evacuationModesB111Evacuate.put(modesString, count + 1);
						}
					} else {
						B12.add(personId);
						
						List<Leg> legs = agentLegs.get(personId);
						Set<String> modes = new TreeSet<String>();
						for (Leg leg : legs) modes.add(leg.getMode());
						String modesString = CollectionUtils.setToString(modes);
						this.evacuateDirectlyModes.put(personId, modesString);
						Integer count = evacuationModesB12.get(modesString);
						if (count == null) evacuationModesB12.put(modesString, 1);
						else evacuationModesB12.put(modesString, count + 1);
					}
					
				} else {
					/*
					 * The person might have returned home and then started to evacuate. Then,
					 * the person's behaviour is B111. Otherwise we do not know. It could be
					 * either B111, B112 or B12.
					 */					
					List<Activity> list = this.agentActivities.get(personId);
					List<Activity> copy = new ArrayList<Activity>();
					
					// ignore pick-up and drop-off activities
					for (Activity activity : list) {
						if (activity.getType().equals(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE)) continue;
						else if (activity.getType().equals(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE)) continue;
						
						copy.add(activity);
					}
					
					boolean returnsHome = false;
					for (int i = 0; i < copy.size(); i++) {
						Activity activity = copy.get(i);
						if (activity.getFacilityId().equals(homeFacilityId)) returnsHome = true;
					}
					
					/*
					 * If the person returned home (and we know that the person is currently en-route),
					 * the person is currently evacuating, i.e. its behaviour type is B111.
					 */
					if (returnsHome) {
						B11.add(personId);
						B111.add(personId);
							
						/*
						 * Split "returning home" and "evacuate from home" trips.
						 */
						boolean foundHomeActivity = false;
						double endHomeActivityTime = Double.MAX_VALUE;
						for (int i = copy.size() - 1; i >= 0; i--) {
							Activity activity = copy.get(i);
							if (activity.getType().equals(EvacuationConstants.MEET_ACTIVITY)) {
								endHomeActivityTime = activity.getEndTime();
								this.returnHomeTimes.put(personId, activity.getStartTime());
								foundHomeActivity = true;
								break;
							}
						}
						if (!foundHomeActivity) {
							throw new RuntimeException("Could not identify agent's return home activity: " + personId);
						}
								
						List<Leg> legs = agentLegs.get(personId);
						Set<String> modesHome = new TreeSet<String>();
						Set<String> modesEvacuate = new TreeSet<String>();
						
						for (Leg leg : legs) {
							if (leg.getDepartureTime() < endHomeActivityTime) modesHome.add(leg.getMode());
							else modesEvacuate.add(leg.getMode());
						}
						
						String modesString;
						Integer count;
						
						modesString = CollectionUtils.setToString(modesHome);
						this.returnHomeModes.put(personId, modesString);
						count = evacuationModesB111Home.get(modesString);
						if (count == null) evacuationModesB111Home.put(modesString, 1);
						else evacuationModesB111Home.put(modesString, count + 1);
						
						/*
						 * If the person already left the evacuation area, we also set the used transport modes.
						 * Otherwise we do not set them since this would end up in a situation where evacuation
						 * modes are defined but no evacuation time is known.
						 */
//						if (this.evacuateFromHomeTimes.containsKey(personId)) {
						if (this.leftEvacuationAreaTime.containsKey(personId)) {
							modesString = CollectionUtils.setToString(modesEvacuate);
							this.evacuateFromHomeModes.put(personId, modesString);
							count = evacuationModesB111Evacuate.get(modesString);
							if (count == null) evacuationModesB111Evacuate.put(modesString, 1);
							else evacuationModesB111Evacuate.put(modesString, count + 1);							
						}
					} else {
						B13.add(personId);
						log.warn("Agent is not in a facility when simulation ends: " + personId);
					}
				}
				
			} else {
				B2.add(personId);
				
				List<Leg> legs = agentLegs.get(personId);
				Set<String> modes = new TreeSet<String>();
				for (Leg leg : legs) modes.add(leg.getMode());
				String modesString = CollectionUtils.setToString(modes);
				this.evacuateDirectlyModes.put(personId, modesString);
				Integer count = evacuationModesB2.get(modesString);
				if (count == null) evacuationModesB2.put(modesString, 1);
				else evacuationModesB2.put(modesString, count + 1);
			}
		}
		log.info("\tB\t" + "total in evacuation area and not at home:\t" + B.size());
		headers.add("B");
		values.add(String.valueOf(B.size()));
		
		log.info("\tB1\t" + "in evacuation area and not at home, home affected:\t" + B1.size());
		log.info("\tB11\t" + "in evacuation area and not at home, home affected, return home:\t" + B11.size());
		log.info("\tB111\t" + "in evacuation area and not at home, home affected, return home, then evacuate:\t" + B111.size());
		log.info("\tB112\t" + "in evacuation area and not at home, home affected, return home and stay there:\t" + B112.size());
		log.info("\tB12\t" + "in evacuation area and not at home, home affected, evacuate immediately:\t" + B12.size());
		log.info("\tB13\t" + "in evacuation area and not at home, home affected, still en-route (i.e. stuck/undefined):\t" + B13.size());
		headers.add("B1");
		headers.add("B11");
		headers.add("B111");
		headers.add("B112");
		headers.add("B12");
		headers.add("B13");
		values.add(String.valueOf(B1.size()));
		values.add(String.valueOf(B11.size()));
		values.add(String.valueOf(B111.size()));
		values.add(String.valueOf(B112.size()));
		values.add(String.valueOf(B12.size()));
		values.add(String.valueOf(B13.size()));
		
		log.info("\tB2\t" + "in evacuation area and not at home, home not affected:\t" + B2.size());
		headers.add("B2");
		values.add(String.valueOf(B2.size()));
		
		for (String modes : evacuationModesB111Home.keySet()) {
			log.info("\tB111 (return home)\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesB111Home.get(modes));			
		}
		for (String modes : evacuationModesB111Evacuate.keySet()) {
			log.info("\tB111 (evacuate from home)\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesB111Evacuate.get(modes));			
		}
		for (String modes : evacuationModesB112.keySet()) {
			log.info("\tB112\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesB112.get(modes));			
		}
		for (String modes : evacuationModesB12.keySet()) {
			log.info("\tB12\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesB12.get(modes));			
		}
		for (String modes : evacuationModesB2.keySet()) {
			log.info("\tB2\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesB2.get(modes));			
		}
	}

	private void analyzeC() {
		
		for (Id personId : C) {
			AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
			Position position = agentPosition.getPositionType();
			
			Id householdId = householdsTracker.getPersonsHouseholdId(personId);
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
			Id homeFacilityId = hdd.getHomeFacilityId();
			
			if (hdd.isHomeFacilityIsAffected()) {
				C1.add(personId);
				
				if (position == Position.FACILITY) {
										
					List<Activity> list = this.agentActivities.get(personId);
					List<Activity> copy = new ArrayList<Activity>();
					
					// ignore pick-up and drop-off activities
					for (Activity activity : list) {
						if (activity.getType().equals(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE)) continue;
						else if (activity.getType().equals(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE)) continue;
						
						copy.add(activity);
					}
					
					boolean returnsHome = false;
					boolean endsAtHome = false;
					for (int i = 0; i < copy.size(); i++) {
						Activity activity = copy.get(i);
						if (activity.getFacilityId().equals(homeFacilityId)) returnsHome = true;
					}
					Activity lastActivity = copy.get(copy.size() - 1);
					if (lastActivity.getFacilityId().equals(homeFacilityId)) endsAtHome = true;
					
					if (returnsHome || endsAtHome) {
						C11.add(personId);
						if (endsAtHome) {
							C112.add(personId);
							
							List<Leg> legs = agentLegs.get(personId);
							Set<String> modes = new TreeSet<String>();
							for (Leg leg : legs) modes.add(leg.getMode());
							String modesString = CollectionUtils.setToString(modes);
							this.returnHomeModes.put(personId, modesString);
							this.returnHomeTimes.put(personId, lastActivity.getStartTime());
							Integer count = evacuationModesC112.get(modesString);
							if (count == null) evacuationModesC112.put(modesString, 1);
							else evacuationModesC112.put(modesString, count + 1);
						}
						else {
							C111.add(personId);
							
							/*
							 * Split "returning home" and "evacuate from home" trips.
							 */
							boolean foundHomeActivity = false;
							double endHomeActivityTime = Double.MAX_VALUE;
							for (int i = copy.size() - 1; i >= 0; i--) {
								Activity activity = copy.get(i);
								if (activity.getType().equals(EvacuationConstants.MEET_ACTIVITY)) {
									endHomeActivityTime = activity.getEndTime();
									this.returnHomeTimes.put(personId, activity.getStartTime());
									foundHomeActivity = true;
									break;
								}
							}
							if (!foundHomeActivity) {
								throw new RuntimeException("Could not identify agent's return home activity: " + personId);
							}
														
							List<Leg> legs = agentLegs.get(personId);
							Set<String> modesHome = new TreeSet<String>();
							Set<String> modesEvacuate = new TreeSet<String>();
							
							for (Leg leg : legs) {
								if (leg.getDepartureTime() < endHomeActivityTime) modesHome.add(leg.getMode());
								else modesEvacuate.add(leg.getMode());
							}
							
							String modesString;
							Integer count;
							
							modesString = CollectionUtils.setToString(modesHome);
							this.returnHomeModes.put(personId, modesString);
							count = evacuationModesC111Home.get(modesString);
							if (count == null) evacuationModesC111Home.put(modesString, 1);
							else evacuationModesC111Home.put(modesString, count + 1);
														
							modesString = CollectionUtils.setToString(modesEvacuate);
							this.evacuateFromHomeModes.put(personId, modesString);
							count = evacuationModesC111Evacuate.get(modesString);
							if (count == null) evacuationModesC111Evacuate.put(modesString, 1);
							else evacuationModesC111Evacuate.put(modesString, count + 1);
						}
					} else {
						C12.add(personId);
						
						List<Leg> legs = agentLegs.get(personId);
						Set<String> modes = new TreeSet<String>();
						for (Leg leg : legs) modes.add(leg.getMode());
						String modesString = CollectionUtils.setToString(modes);
						this.evacuateDirectlyModes.put(personId, modesString);
						Integer count = evacuationModesC12.get(modesString);
						if (count == null) evacuationModesC12.put(modesString, 1);
						else evacuationModesC12.put(modesString, count + 1);
					}
					
				} else {
					/*
					 * The person might have returned home and then started to evacuate. Then,
					 * the person's behaviour is C111. Otherwise we do not know. It could be
					 * either C111, C112 or C12.
					 */
					List<Activity> list = this.agentActivities.get(personId);
					List<Activity> copy = new ArrayList<Activity>();
					
					// ignore pick-up and drop-off activities
					for (Activity activity : list) {
						if (activity.getType().equals(PassengerQNetsimEngine.PICKUP_ACTIVITY_TYPE)) continue;
						else if (activity.getType().equals(PassengerQNetsimEngine.DROP_OFF_ACTIVITY_TYPE)) continue;
						
						copy.add(activity);
					}
					
					boolean returnsHome = false;
					for (int i = 0; i < copy.size(); i++) {
						Activity activity = copy.get(i);
						if (activity.getFacilityId().equals(homeFacilityId)) returnsHome = true;
					}
					
					/*
					 * If the person returned home (and we know that the person is currently en-route),
					 * the person is currently evacuating, i.e. its behaviour type is C111.
					 */
					if (returnsHome) {
						C11.add(personId);
						C111.add(personId);
						
						/*
						 * Split "returning home" and "evacuate from home" trips.
						 */
						boolean foundHomeActivity = false;
						double endHomeActivityTime = Double.MAX_VALUE;
						for (int i = copy.size() - 1; i >= 0; i--) {
							Activity activity = copy.get(i);
							if (activity.getType().equals(EvacuationConstants.MEET_ACTIVITY)) {
								endHomeActivityTime = activity.getEndTime();
								this.returnHomeTimes.put(personId, activity.getStartTime());
								foundHomeActivity = true;
								break;
							}
						}
						if (!foundHomeActivity) {
							throw new RuntimeException("Could not identify agent's return home activity: " + personId);
						}
													
						List<Leg> legs = agentLegs.get(personId);
						Set<String> modesHome = new TreeSet<String>();
						Set<String> modesEvacuate = new TreeSet<String>();
						
						for (Leg leg : legs) {
							if (leg.getDepartureTime() < endHomeActivityTime) modesHome.add(leg.getMode());
							else modesEvacuate.add(leg.getMode());
						}
						
						String modesString;
						Integer count;
						
						modesString = CollectionUtils.setToString(modesHome);
						this.returnHomeModes.put(personId, modesString);
						count = evacuationModesC111Home.get(modesString);
						if (count == null) evacuationModesC111Home.put(modesString, 1);
						else evacuationModesC111Home.put(modesString, count + 1);
													
						/*
						 * If the person already left the evacuation area, we also set the used transport modes.
						 * Otherwise we do not set them since this would end up in a situation where evacuation
						 * modes are defined but no evacuation time is known.
						 */
//						if (this.evacuateFromHomeTimes.containsKey(personId)) {
						if (this.leftEvacuationAreaTime.containsKey(personId)) {
							modesString = CollectionUtils.setToString(modesEvacuate);
							this.evacuateFromHomeModes.put(personId, modesString);
							count = evacuationModesC111Evacuate.get(modesString);
							if (count == null) evacuationModesC111Evacuate.put(modesString, 1);
							else evacuationModesC111Evacuate.put(modesString, count + 1);							
						}
					} else {
						C13.add(personId);
						log.warn("Agent is not in a facility when simulation ends: " + personId);
					}					
				}
				
			} else {
				C2.add(personId);
				
				List<Leg> legs = agentLegs.get(personId);
				Set<String> modes = new TreeSet<String>();
				for (Leg leg : legs) modes.add(leg.getMode());
				String modesString = CollectionUtils.setToString(modes);
				this.evacuateDirectlyModes.put(personId, modesString);
				Integer count = evacuationModesC2.get(modesString);
				if (count == null) evacuationModesC2.put(modesString, 1);
				else evacuationModesC2.put(modesString, count + 1);
			}
		}
		log.info("\tC\t" + "total outside evacuation area and not at home:\t" + C.size());
		headers.add("C");
		values.add(String.valueOf(C.size()));
		
		log.info("\tC1\t" + "outside evacuation area and not at home, home affected:\t" + C1.size());
		log.info("\tC11\t" + "outside evacuation area and not at home, home affected, return home:\t" + C11.size());
		log.info("\tC111\t" + "outside evacuation area and not at home, home affected, return home, then evacuate:\t" + C111.size());
		log.info("\tC112\t" + "outside evacuation area and not at home, home affected, return home and stay there:\t" + C112.size());
		log.info("\tC12\t" + "outside evacuation area and not at home, home affected, evacuate immediately:\t" + C12.size());
		log.info("\tC13\t" + "outside evacuation area and not at home, home affected, still en-route (i.e. stuck/undefined):\t" + C13.size());
		headers.add("C1");
		headers.add("C11");
		headers.add("C111");
		headers.add("C112");
		headers.add("C12");
		headers.add("C13");
		values.add(String.valueOf(C1.size()));
		values.add(String.valueOf(C11.size()));
		values.add(String.valueOf(C111.size()));
		values.add(String.valueOf(C112.size()));
		values.add(String.valueOf(C12.size()));
		values.add(String.valueOf(C13.size()));
		
		log.info("\tC2\t" + "outside evacuation area and not at home, home not affected:\t" + C2.size());
		headers.add("C2");
		values.add(String.valueOf(C2.size()));

		for (String modes : evacuationModesC111Home.keySet()) {
			log.info("\tC111 (return home)\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesC111Home.get(modes));			
		}
		for (String modes : evacuationModesC111Evacuate.keySet()) {
			log.info("\tC111 (evacuate from home)\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesC111Evacuate.get(modes));			
		}
		for (String modes : evacuationModesC112.keySet()) {
			log.info("\tC112\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesC112.get(modes));			
		}
		for (String modes : evacuationModesC12.keySet()) {
			log.info("\tC12\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesC12.get(modes));			
		}
		for (String modes : evacuationModesC2.keySet()) {
			log.info("\tC2\t" + "mode(s):\t" + modes + "\tcount:\t" + evacuationModesC2.get(modes));			
		}
	}
	
	private void writeResultsToFiles(String countsFileName, String timesFileName, String modesFileName, 
			String detailsFileName) {
		
		/*
		 * Counts
		 */
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(countsFileName);
			
			writer.write("TYPE");
			writer.write(delimiter);
			for (int i = 0; i < headers.size(); i++) {
				String header = headers.get(i);
				writer.write(header);
				if (i < headers.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			
			writer.write("ALL");
			writer.write(delimiter);
			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				writer.write(value);
				if (i < values.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			
			List<String> valuesImmediately = new ArrayList<String>();
			List<String> valuesLater = new ArrayList<String>();
			List<String> valuesNever = new ArrayList<String>();
			List<String> valuesUndefined = new ArrayList<String>();

			List<Set<Id>> sets = new ArrayList<Set<Id>>();
			sets.add(A); sets.add(A1); sets.add(A11); sets.add(A12); sets.add(A2); sets.add(A21); sets.add(A22);
			sets.add(B); sets.add(B1); sets.add(B11); sets.add(B111); sets.add(B112); sets.add(B12); sets.add(B13); sets.add(B2);
			sets.add(C); sets.add(C1); sets.add(C11); sets.add(C111); sets.add(C112); sets.add(C12); sets.add(C13); sets.add(C2);
			
			for (Set<Id> set : sets) {
				int immediately = 0;
				int later = 0;
				int never = 0;
				int undefined = 0;
				for (Id id : set) {
					Id householdId = this.decisionDataProvider.getPersonDecisionData(id).getHouseholdId();
					EvacuationDecision decision = this.decisionDataProvider.getHouseholdDecisionData(householdId).getEvacuationDecision();
					
					if (decision == EvacuationDecision.IMMEDIATELY) {
						immediately++;
					} else if (decision == EvacuationDecision.LATER) {
						later++;
					} else if (decision == EvacuationDecision.NEVER) {
						never++;
					} else if (decision == EvacuationDecision.UNDEFINED) {
						undefined++;
					}
				}
				valuesImmediately.add(String.valueOf(immediately));
				valuesLater.add(String.valueOf(later));
				valuesNever.add(String.valueOf(never));
				valuesUndefined.add(String.valueOf(undefined));				
			}
			
			List<Tuple<String, List<String>>> lists = new ArrayList<Tuple<String, List<String>>>();
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.IMMEDIATELY.toString(), valuesImmediately));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.LATER.toString(), valuesLater));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.NEVER.toString(), valuesNever));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.UNDEFINED.toString(), valuesUndefined));
			
			for (Tuple<String, List<String>> tuple : lists) {
				writer.write(tuple.getFirst());
				writer.write(delimiter);
				
				List<String> list = tuple.getSecond();
				for (int i = 0; i < list.size(); i++) {
					String value = list.get(i);
					writer.write(value);
					if (i < list.size() - 1) writer.write(delimiter);
					else writer.write(newLine);
				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		/*
		 * Modes
		 */
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(modesFileName);
			
			Set<String> modes = new TreeSet<String>();
			modes.add(TransportMode.bike);
			modes.add(TransportMode.car);
			modes.add(TransportMode.ride);
			modes.add(TransportMode.pt);
			modes.add(TransportMode.walk);
			modes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);
			modes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE + "," + TransportMode.walk);
			
			List<String> modeHeaders = new ArrayList<String>();
			List<String> modeValuesOverall = new ArrayList<String>();
			List<String> modeValuesImmediately = new ArrayList<String>();
			List<String> modeValuesLater = new ArrayList<String>();
			List<String> modeValuesNever = new ArrayList<String>();
			List<String> modeValuesUndefined = new ArrayList<String>();
			
			Map<String, Map<String, Integer>> maps = new LinkedHashMap<String, Map<String, Integer>>();
			maps.put("A11", evacuationModesA11);
			maps.put("A21", evacuationModesA21);
			
			maps.put("B111Home", evacuationModesB111Home);
			maps.put("B111Evacuate", evacuationModesB111Evacuate);
			maps.put("B112", evacuationModesB112);
			maps.put("B12", evacuationModesB12);
			maps.put("B2", evacuationModesB2);
			
			maps.put("C111Home", evacuationModesC111Home);
			maps.put("C111Evacuate", evacuationModesC111Evacuate);
			maps.put("C112", evacuationModesC112);
			maps.put("C12", evacuationModesC12);
			maps.put("C2", evacuationModesC2);
			
			for (Entry<String, Map<String, Integer>> entry : maps.entrySet()) {
				
				String string = entry.getKey();
				Map<String, Integer> map =  entry.getValue();
				
				Set<String> occurringModes = map.keySet();
				for (String mode : occurringModes) {
					if (!modes.contains(mode)) {
						throw new RuntimeException("Mode " + mode + " was found but was not expected!");
					}
				}
				
				for (String mode : modes) {
					Integer count = map.get(mode);
					if (count == null) count = 0;
					modeHeaders.add(string + "_" + mode);
					modeValuesOverall.add(String.valueOf(count));
				}
			}
			
			List<Tuple<Set<Id>, Map<Id, String>>> tuples = new ArrayList<Tuple<Set<Id>, Map<Id, String>>>();
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(A11, evacuateFromHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(A21, evacuateFromHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(B111, returnHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(B111, evacuateFromHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(B112, returnHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(B12, evacuateDirectlyModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(B2, evacuateDirectlyModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(C111, returnHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(C111, evacuateFromHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(C112, returnHomeModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(C12, evacuateDirectlyModes));
			tuples.add(new Tuple<Set<Id>, Map<Id, String>>(C2, evacuateDirectlyModes));
			
			for(Tuple<Set<Id>, Map<Id, String>> tuple : tuples) {
				
				Set<Id> set = tuple.getFirst();
				Map<Id, String> map = tuple.getSecond();
				
				Map<String, Integer> countsImmediately = new LinkedHashMap<String, Integer>();
				Map<String, Integer> countsLater = new LinkedHashMap<String, Integer>();
				Map<String, Integer> countsNever = new LinkedHashMap<String, Integer>();
				Map<String, Integer> countsUndefined = new LinkedHashMap<String, Integer>();
				
				for (String mode : modes) countsImmediately.put(mode, 0);
				for (String mode : modes) countsLater.put(mode, 0);
				for (String mode : modes) countsNever.put(mode, 0);
				for (String mode : modes) countsUndefined.put(mode, 0);
				
				for (Id id : set) {
					
					String mode;
					
					// Check whether there is an entry in the modes map.
					mode = map.get(id);
					if (mode == null) {
						log.warn("No mode(s) for agent " + id.toString() + " was found. Ignore agent for mode analysis!");
						continue;
					}
					
					if (!modes.contains(mode)) {
						throw new RuntimeException("Mode " + mode + " was found but was not expected!");
					}
					
					Id householdId = this.decisionDataProvider.getPersonDecisionData(id).getHouseholdId();
					EvacuationDecision decision = this.decisionDataProvider.getHouseholdDecisionData(householdId).getEvacuationDecision();
					
					if (decision == EvacuationDecision.IMMEDIATELY) {
						int count = countsImmediately.get(mode);
						countsImmediately.put(mode, count + 1);
					} else if (decision == EvacuationDecision.LATER) {
						int count = countsLater.get(mode);
						countsLater.put(mode, count + 1);
					} else if (decision == EvacuationDecision.NEVER) {
						int count = countsNever.get(mode);
						countsNever.put(mode, count + 1);
					} else if (decision == EvacuationDecision.UNDEFINED) {
						int count = countsUndefined.get(mode);
						countsUndefined.put(mode, count + 1);
					}
				}
				
				for (String mode : modes) {
					modeValuesImmediately.add(String.valueOf(countsImmediately.get(mode)));
					modeValuesLater.add(String.valueOf(countsLater.get(mode)));
					modeValuesNever.add(String.valueOf(countsNever.get(mode)));
					modeValuesUndefined.add(String.valueOf(countsUndefined.get(mode)));				
				}
			}		

			writer.write("TYPE");
			writer.write(delimiter);
			for (int i = 0; i < modeHeaders.size(); i++) {
				String header = modeHeaders.get(i);
				writer.write(header);
				if (i < modeHeaders.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			
			List<Tuple<String, List<String>>> lists = new ArrayList<Tuple<String, List<String>>>();
			lists.add(new Tuple<String, List<String>>("ALL", modeValuesOverall));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.IMMEDIATELY.toString(), modeValuesImmediately));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.LATER.toString(), modeValuesLater));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.NEVER.toString(), modeValuesNever));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.UNDEFINED.toString(), modeValuesUndefined));
			
			for (Tuple<String, List<String>> tuple : lists) {
				writer.write(tuple.getFirst());
				writer.write(delimiter);
				
				List<String> list = tuple.getSecond();
				for (int i = 0; i < list.size(); i++) {
					String value = list.get(i);
					writer.write(value);
					if (i < list.size() - 1) writer.write(delimiter);
					else writer.write(newLine);
				}				
			}

			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		/*
		 * Details
		 */
		try {
			Map<Id, String> evacuationTypes = new HashMap<Id, String>();
			for (Id personId : scenario.getPopulation().getPersons().keySet()) {
				String evacuationType = "UNDEFINED";
				
				if (A11.contains(personId)) evacuationType = "A11";
				else if (A12.contains(personId)) evacuationType = "A12";
				else if (A21.contains(personId)) evacuationType = "A21";
				else if (A22.contains(personId)) evacuationType = "A22";
				
				else if (B111.contains(personId)) evacuationType = "B111";
				else if (B112.contains(personId)) evacuationType = "B112";
				else if (B12.contains(personId)) evacuationType = "B12";
				else if (B13.contains(personId)) evacuationType = "B13";
				else if (B2.contains(personId)) evacuationType = "B2";
				
				else if (C111.contains(personId)) evacuationType = "C111";
				else if (C112.contains(personId)) evacuationType = "C112";
				else if (C12.contains(personId)) evacuationType = "C12";
				else if (C13.contains(personId)) evacuationType = "C13";
				else if (C2.contains(personId)) evacuationType = "C2";
				
				evacuationTypes.put(personId, evacuationType);
			}
			
			// assign evacuation left times to evacuation types
			for (Entry<Id, Double> entry : this.leftEvacuationAreaTime.entrySet()) {
				
				// ignore agents who left the evacuation area before the evacuation has started
				if (entry.getValue() < EvacuationConfig.evacuationTime) {					
					this.evacuateDirectlyModes.remove(entry.getKey());
					continue;
				}
				
				// ignore agents who left the evacuation area before they were informed
				if (entry.getValue() < this.informationTime.get(entry.getKey())) {					
					this.evacuateDirectlyModes.remove(entry.getKey());
					continue;
				}
				
				String evacuationType = evacuationTypes.get(entry.getKey());
				
				// ignore agents with undefined evacuation strategy
				if (evacuationType.equals("B13")) continue;
				else if (evacuationType.equals("C13")) continue;
				
				// assign left times
				if (evacuationType.equals("A11")) this.evacuateFromHomeTimes.put(entry.getKey(), entry.getValue());
				else if (evacuationType.equals("B111")) this.evacuateFromHomeTimes.put(entry.getKey(), entry.getValue());
				else if (evacuationType.equals("B12")) this.evacuateDirectlyTimes.put(entry.getKey(), entry.getValue());
				else if (evacuationType.equals("B2")) this.evacuateDirectlyTimes.put(entry.getKey(), entry.getValue());
				else if (evacuationType.equals("C111")) this.evacuateFromHomeTimes.put(entry.getKey(), entry.getValue());
				else if (evacuationType.equals("C12")) this.evacuateDirectlyTimes.put(entry.getKey(), entry.getValue());
				else if (evacuationType.equals("C2")) this.evacuateDirectlyTimes.put(entry.getKey(), entry.getValue());
				else {
					log.info("agentId: " + entry.getKey().toString());
					log.info("time: " + entry.getValue());
					log.info("informationTime: " + informationTime.get(entry.getKey()));

					throw new RuntimeException("Found an evacuation area left time for unexpected evacuation type: " 
							+ evacuationType);
				}
			}

			BufferedWriter writer = IOUtils.getBufferedWriter(detailsFileName);
			
			writer.write("agentId");
			writer.write(delimiter);
			writer.write("evacuationType");
			writer.write(delimiter);
			writer.write("returnHomeTimes");
			writer.write(delimiter);
			writer.write("returnHomeModes");
			writer.write(delimiter);
			writer.write("evacuateFromHomeTimes");
			writer.write(delimiter);
			writer.write("evacuateFromHomeModes");
			writer.write(delimiter);
			writer.write("evacuateDirectlyTimes");
			writer.write(delimiter);
			writer.write("evacuateDirectlyModes");
			writer.write(newLine);
			
			for (Id personId : scenario.getPopulation().getPersons().keySet()) {
				writer.write(personId.toString());
				writer.write(delimiter);
				
				String evacuationType = evacuationTypes.get(personId);
				writer.write(evacuationType);
				
				// ignore stuck agents
				if (this.stuckAgents.contains(personId)) {
					writer.write(delimiter);
					
					Double time;
					String mode;
					
					time = this.returnHomeTimes.get(personId);
					if (time != null) writer.write(time.toString());
					else writer.write("-1");
					writer.write(delimiter);
					mode = this.returnHomeModes.get(personId);
					if (mode != null) writer.write(mode);
					else writer.write(PersonStuckEvent.EVENT_TYPE);
					writer.write(delimiter);
					
					time = this.evacuateFromHomeTimes.get(personId);
					if (time != null) writer.write(time.toString());
					else writer.write("-1");
					writer.write(delimiter);
					mode = this.evacuateFromHomeModes.get(personId);
					if (mode != null) writer.write(mode);
					else writer.write(PersonStuckEvent.EVENT_TYPE);
					writer.write(delimiter);
					
					time = this.evacuateDirectlyTimes.get(personId);
					if (time != null) writer.write(time.toString());
					else writer.write("-1");
					writer.write(delimiter);
					mode = this.evacuateDirectlyModes.get(personId);
					if (mode != null) writer.write(mode);
					else writer.write(PersonStuckEvent.EVENT_TYPE);
					writer.write(newLine);
					
					continue;
				}
				
				writer.write(delimiter);
				Double returnHomeTime = this.returnHomeTimes.get(personId);
				if (returnHomeTime == null) writer.write("-1");
				else writer.write(returnHomeTime.toString());
				
				writer.write(delimiter);
				String returnHomeMode = this.returnHomeModes.get(personId);
				if (returnHomeMode == null) writer.write("-1");
				else writer.write(returnHomeMode);
								
				writer.write(delimiter);
				Double evacuateFromHomeTime = this.evacuateFromHomeTimes.get(personId);
				if (evacuateFromHomeTime == null) writer.write("-1");
				else writer.write(evacuateFromHomeTime.toString());
				
				writer.write(delimiter);
				String evacuateFromHomeMode = this.evacuateFromHomeModes.get(personId);
				if (evacuateFromHomeMode == null) writer.write("-1");
				else writer.write(evacuateFromHomeMode);
				
				writer.write(delimiter);
				Double evacuateDirectlyTime = this.evacuateDirectlyTimes.get(personId);
				if (evacuateDirectlyTime == null) writer.write("-1");
				else writer.write(evacuateDirectlyTime.toString());
				
				writer.write(delimiter);
				String evacuateDirectlyMode = this.evacuateDirectlyModes.get(personId);
				if (evacuateDirectlyMode == null) writer.write("-1");
				else writer.write(evacuateDirectlyMode);
				writer.write(newLine);
				
				// consistency checks
				if (returnHomeTime != null && returnHomeMode == null) {
					throw new RuntimeException("Found return home time but no return home modes for agent " +
							personId.toString() + " using evacuation strategy " + evacuationType);
				}
				if (returnHomeTime == null && returnHomeMode != null) {
					throw new RuntimeException("Found return home modes but no return home time for agent " +
							personId.toString() + " using evacuation strategy " + evacuationType);
				}
				
				if (evacuateFromHomeTime != null && evacuateFromHomeMode == null) {
					throw new RuntimeException("Found evacuate from home time but no evacuate from home modes for agent " +
							personId.toString() + " using evacuation strategy " + evacuationType);
				}
				if (evacuateFromHomeTime == null && evacuateFromHomeMode != null) {
					throw new RuntimeException("Found evacuate from home modes but no evacuate from home time for agent " +
							personId.toString() + " using evacuation strategy " + evacuationType);
				}
				
				if (evacuateDirectlyTime != null && evacuateDirectlyMode == null) {
					throw new RuntimeException("Found evacuate directly time but no evacuate directly modes for agent " +
							personId.toString() + " using evacuation strategy " + evacuationType);
				}
				// This might occur - but we are not interested in those agents anyway.
//				if (evacuateDirectlyTime == null && evacuateDirectlyMode != null) {
//					throw new RuntimeException("Found evacuate directly modes but no evacuate directly time for agent " +
//							personId.toString() + " using evacuation strategy " + evacuationType);
//				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		/*
		 * Times
		 */
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(timesFileName);
						
			Set<String> modes = new TreeSet<String>();
			modes.add(TransportMode.bike);
			modes.add(TransportMode.car);
			modes.add(TransportMode.ride);
			modes.add(TransportMode.pt);
			modes.add(TransportMode.walk);
			modes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);
			modes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE + "," + TransportMode.walk);
						
			List<String> modeHeaders = new ArrayList<String>();
			List<String> modeValuesOverall = new ArrayList<String>();
			List<String> modeValuesImmediately = new ArrayList<String>();
			List<String> modeValuesLater = new ArrayList<String>();
			List<String> modeValuesNever = new ArrayList<String>();
			List<String> modeValuesUndefined = new ArrayList<String>();
			List<String> modeCountsOverall = new ArrayList<String>();
			List<String> modeCountsImmediately = new ArrayList<String>();
			List<String> modeCountsLater = new ArrayList<String>();
			List<String> modeCountsNever = new ArrayList<String>();
			List<String> modeCountsUndefined = new ArrayList<String>();
			
			Map<String, Map<String, List<Tuple<Id, Double>>>> maps = new LinkedHashMap<String, Map<String, List<Tuple<Id, Double>>>>();
			maps.put("A11", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("A21", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			
			maps.put("B111Home", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("B111Evacuate", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("B112", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("B12", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("B2", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			
			maps.put("C111Home", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("C111Evacuate", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("C112", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("C12", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
			maps.put("C2", new LinkedHashMap<String, List<Tuple<Id, Double>>>());
						
			for (Entry<String, Map<String, List<Tuple<Id, Double>>>> entry : maps.entrySet()) {
				
				Map<String, List<Tuple<Id, Double>>> map =  entry.getValue();				
				for (String mode : modes) {
					map.put(mode, new ArrayList<Tuple<Id, Double>>());
				}
			}
			
			// get return home times
			for (Entry<Id, Double> e : this.returnHomeTimes.entrySet()) {
				Id id = e.getKey();
				double returnHomeTime = e.getValue();
				String mode = this.returnHomeModes.get(id);
				
				if (B111.contains(id)) {
					maps.get("B111Home").get(mode).add(new Tuple<Id, Double>(id, returnHomeTime));
				} else if (B112.contains(id)) {
					maps.get("B112").get(mode).add(new Tuple<Id, Double>(id, returnHomeTime));
				} else if (C111.contains(id)) {
					maps.get("C111Home").get(mode).add(new Tuple<Id, Double>(id, returnHomeTime));
				} else if (C112.contains(id)) {
					maps.get("C112").get(mode).add(new Tuple<Id, Double>(id, returnHomeTime));
				} else {
					throw new RuntimeException("Unexpected behaviour found for agent " + id.toString());
				}
			}
			
			// get evacuate from home times
			for (Entry<Id, Double> e : this.evacuateFromHomeTimes.entrySet()) {
				Id id = e.getKey();
				double evacuateFromHomeTime = e.getValue();
				String mode = this.evacuateFromHomeModes.get(id);
				
				// If the agent got stuck within the evacuation area, ignore it since evacuation time is not known. 
				if (mode == null && this.stuckAgents.contains(id)) continue;
				
				if (A11.contains(id)) {
					maps.get("A11").get(mode).add(new Tuple<Id, Double>(id, evacuateFromHomeTime));
				} else if (B111.contains(id)) {
					maps.get("B111Evacuate").get(mode).add(new Tuple<Id, Double>(id, evacuateFromHomeTime));
				} else if (C111.contains(id)) {
					maps.get("C111Evacuate").get(mode).add(new Tuple<Id, Double>(id, evacuateFromHomeTime));
				} else {
					throw new RuntimeException("Unexpected behaviour found for agent " + id.toString());
				}
			}
			
			// get evacuate directly times
			for (Entry<Id, Double> e : this.evacuateDirectlyTimes.entrySet()) {
				Id id = e.getKey();
				double evacuateDirectlyTime = e.getValue();
				String mode = this.evacuateDirectlyModes.get(id);
				
				if (B12.contains(id)) {
					maps.get("B12").get(mode).add(new Tuple<Id, Double>(id, evacuateDirectlyTime));
				} else if (B2.contains(id)) {
					maps.get("B2").get(mode).add(new Tuple<Id, Double>(id, evacuateDirectlyTime));
				} else if (C12.contains(id)) {
					maps.get("C12").get(mode).add(new Tuple<Id, Double>(id, evacuateDirectlyTime));
				} else if (C2.contains(id)) {
					maps.get("C2").get(mode).add(new Tuple<Id, Double>(id, evacuateDirectlyTime));
				} else {
					throw new RuntimeException("Unexpected behaviour found for agent " + id.toString());
				}
			}				
			
			for (Entry<String, Map<String, List<Tuple<Id, Double>>>> entry : maps.entrySet()) {
				
				String string = entry.getKey();
				Map<String, List<Tuple<Id, Double>>> map =  entry.getValue();
				
				for (String mode : modes) {
					List<Tuple<Id, Double>> list = map.get(mode);
					
					double avgTimeOverall = Double.NaN;
					double avgTimeImmediately = Double.NaN;
					double avgTimeLater = Double.NaN;
					double avgTimeNever = Double.NaN;
					double avgTimeUndefined = Double.NaN;
					
					int sampleSizeOverall = 0;
					int sampleSizeImmediately = 0;
					int sampleSizeLater = 0;
					int sampleSizeNever = 0;
					int sampleSizeUndefined = 0;
					
					if (list != null) {
						avgTimeOverall = 0.0;
						avgTimeImmediately = 0.0;
						avgTimeLater = 0.0;
						avgTimeNever = 0.0;
						avgTimeUndefined = 0.0;
						// subtract evacuationStartTime to get "real" evacuationTime
						for (Tuple<Id, Double> tuple : list) {
							Id id = tuple.getFirst();
							double time = tuple.getSecond();
							avgTimeOverall += (time - EvacuationConfig.evacuationTime);
							
							Id householdId = this.decisionDataProvider.getPersonDecisionData(id).getHouseholdId();
							EvacuationDecision decision = this.decisionDataProvider.getHouseholdDecisionData(householdId).getEvacuationDecision();
							
							if (decision == EvacuationDecision.IMMEDIATELY) {
								avgTimeImmediately += (time - EvacuationConfig.evacuationTime);
								sampleSizeImmediately++;
							} else if (decision == EvacuationDecision.LATER) {
								avgTimeLater += (time - EvacuationConfig.evacuationTime);
								sampleSizeLater++;
							} else if (decision == EvacuationDecision.NEVER) {
								avgTimeNever += (time - EvacuationConfig.evacuationTime);
								sampleSizeNever++;
							} else if (decision == EvacuationDecision.UNDEFINED) {
								avgTimeUndefined += (time - EvacuationConfig.evacuationTime);
								sampleSizeUndefined++;
							}
						}
						sampleSizeOverall = list.size();
						
						if (sampleSizeOverall > 0) avgTimeOverall /= sampleSizeOverall;
						if (sampleSizeImmediately > 0) avgTimeImmediately /= sampleSizeImmediately;
						if (sampleSizeLater > 0) avgTimeLater /= sampleSizeLater;
						if (sampleSizeNever > 0) avgTimeNever /= sampleSizeNever;
						if (avgTimeUndefined > 0) avgTimeUndefined /= sampleSizeUndefined;
					}
					
					modeHeaders.add(string + "_" + mode);
					
					modeValuesOverall.add(String.valueOf(avgTimeOverall));
					modeValuesImmediately.add(String.valueOf(avgTimeImmediately));
					modeValuesLater.add(String.valueOf(avgTimeLater));
					modeValuesNever.add(String.valueOf(avgTimeNever));
					modeValuesUndefined.add(String.valueOf(avgTimeUndefined));
					
					modeCountsOverall.add(String.valueOf(sampleSizeOverall));
					modeCountsImmediately.add(String.valueOf(sampleSizeImmediately));
					modeCountsLater.add(String.valueOf(sampleSizeLater));
					modeCountsNever.add(String.valueOf(sampleSizeNever));
					modeCountsUndefined.add(String.valueOf(sampleSizeUndefined));
					
					// only for debugging
//					log.info(string + "_" + mode + "_" + String.valueOf(list.size()));
				}
			}
			
			writer.write("TYPE");
			writer.write(delimiter);
			for (int i = 0; i < modeHeaders.size(); i++) {
				String header = modeHeaders.get(i);
				writer.write(header);
				if (i < modeHeaders.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			
			List<Tuple<String, List<String>>> lists = new ArrayList<Tuple<String, List<String>>>();
			lists.add(new Tuple<String, List<String>>("ALL", modeValuesOverall));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.IMMEDIATELY.toString(), modeValuesImmediately));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.LATER.toString(), modeValuesLater));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.NEVER.toString(), modeValuesNever));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.UNDEFINED.toString(), modeValuesUndefined));
			
			for (Tuple<String, List<String>> tuple : lists) {
				writer.write(tuple.getFirst());
				writer.write(delimiter);
				
				List<String> list = tuple.getSecond();
				for (int i = 0; i < list.size(); i++) {
					String value = list.get(i);
					writer.write(value);
					if (i < list.size() - 1) writer.write(delimiter);
					else writer.write(newLine);
				}				
			}
			
			writer.write(newLine);
			
			lists.clear();
			lists.add(new Tuple<String, List<String>>("ALL", modeCountsOverall));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.IMMEDIATELY.toString(), modeCountsImmediately));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.LATER.toString(), modeCountsLater));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.NEVER.toString(), modeCountsNever));
			lists.add(new Tuple<String, List<String>>(EvacuationDecision.UNDEFINED.toString(), modeCountsUndefined));
			
			for (Tuple<String, List<String>> tuple : lists) {
				writer.write(tuple.getFirst());
				writer.write(delimiter);
				
				List<String> list = tuple.getSecond();
				for (int i = 0; i < list.size(); i++) {
					String value = list.get(i);
					writer.write(value);
					if (i < list.size() - 1) writer.write(delimiter);
					else writer.write(newLine);
				}
			}
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
