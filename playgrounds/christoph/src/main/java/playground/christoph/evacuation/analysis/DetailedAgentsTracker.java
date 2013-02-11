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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.GenericEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.GenericEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEventImpl;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEventImpl;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.controler.PrepareEvacuationScenario;
import playground.christoph.evacuation.events.PersonInformationEvent;
import playground.christoph.evacuation.events.PersonInformationEventImpl;
import playground.christoph.evacuation.events.handler.PersonInformationEventHandler;
import playground.christoph.evacuation.mobsim.AgentPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.OldPassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PanicModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel;
import playground.christoph.evacuation.withinday.replanning.replanners.CurrentLegToMeetingPointReplanner;
import playground.christoph.evacuation.withinday.replanning.replanners.DropOffAgentReplanner;
import playground.christoph.evacuation.withinday.replanning.replanners.PickupAgentReplanner;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import com.vividsolutions.jts.geom.Geometry;

public class DetailedAgentsTracker implements GenericEventHandler, PersonInformationEventHandler,
		AgentDepartureEventHandler, AgentArrivalEventHandler, AgentWait2LinkEventHandler, LinkEnterEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler {

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
	private final Set<Id> B113 = new HashSet<Id>();
	private final Set<Id> B12 = new HashSet<Id>();
	private final Set<Id> B2 = new HashSet<Id>();
	
	private final Set<Id> C = new HashSet<Id>();
	private final Set<Id> C1 = new HashSet<Id>();
	private final Set<Id> C11 = new HashSet<Id>();
	private final Set<Id> C111 = new HashSet<Id>();
	private final Set<Id> C112 = new HashSet<Id>();
	private final Set<Id> C113 = new HashSet<Id>();
	private final Set<Id> C12 = new HashSet<Id>();
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
	private final Set<Id> enrouteDrivers = new HashSet<Id>();
	private final Map<Id, Id> vehiclePositions = new HashMap<Id, Id>();
	
	/*
	 * List of all activities and legs an agent performs after being informed. If the agent
	 * currently performs an activity/leg when being informed, the activity/leg is also included.
	 */
	private final Map<Id, List<Activity>> agentActivities = new HashMap<Id, List<Activity>>();
	private final Map<Id, List<Leg>> agentLegs = new HashMap<Id, List<Leg>>();
	
	private final CoordAnalyzer coordAnalyzer;
	private final HouseholdsTracker householdsTracker;
	private final DecisionDataProvider decisionDataProvider;
	
	private final Config config;
	private final Scenario scenario;
	private final EventsManager eventsManager;
	
	private final List<String> headers = new ArrayList<String>();
	private final List<String> values = new ArrayList<String>();
	
	private final String outputCountsFile = "evacuationDecisionCounts.txt";
	private final String outputModesFile = "evacuationDecisionModes.txt";
	
	public static void main(String[] args) {
		String configFile;
		String evacuationConfigFile;
		String outputPath;

		configFile = "../../matsim/mysimulations/census2000V2/output_10pct_evac/evac.1.output_config.xml.gz";
		evacuationConfigFile = "../../matsim/mysimulations/census2000V2/config_evacuation.xml";
		outputPath = "../../matsim/mysimulations/census2000V2/output_10pct_evac/";

//		if (args.length != 3) return;
//		else {
//			configFile = args[0];
//			evacuationConfigFile = args[1];
//			outputPath = args[2];
//		}

		new DetailedAgentsTracker(configFile, evacuationConfigFile, outputPath);
	}
	
	public DetailedAgentsTracker(String configFile, String evacuationConfigFile, String outputPath) {
		
		config = ConfigUtils.loadConfig(configFile);
		
		// ensure vehicles are not loaded (in an output config file, "useVehicles" is set to true, but we do not need them)
		config.scenario().setUseVehicles(false);
		
		scenario = ScenarioUtils.loadScenario(config);
		eventsManager = EventsUtils.createEventsManager();
				
		new EvacuationConfigReader().readFile(evacuationConfigFile);
		EvacuationConfig.printConfig();
		
		// load household object attributes
		ObjectAttributes householdObjectAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(householdObjectAttributes).parse(EvacuationConfig.householdObjectAttributesFile);
		
		/*
		 * Prepare the scenario:
		 * 	- connect facilities to network
		 * 	- add exit links to network
		 * 	- add pickup facilities
		 *  - add z Coordinates to network
		 */
		new PrepareEvacuationScenario().prepareScenario(scenario);
		
		/*
		 * Create two OutputDirectoryHierarchies that point to the analyzed run's output directory.
		 * Since we do not want to overwrite existing results we add an additional prefix
		 * to the re-created outputs.
		 */
//		DummyController dummyInputController = new DummyController(scenario, outputPath);
		OutputDirectoryHierarchy dummyInputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		if (scenario.getConfig().controler().getRunId() != null) {
			dummyInputDirectoryHierarchy = new OutputDirectoryHierarchy(outputPath, scenario.getConfig().controler().getRunId(), true);
		} else {
			dummyInputDirectoryHierarchy = new OutputDirectoryHierarchy(outputPath, null, true);
		}
		
		// add another string to the runId to not overwrite old files
		String runId = scenario.getConfig().controler().getRunId();
		scenario.getConfig().controler().setRunId(runId + ".postprocessed");
//		DummyController dummyOutputController = new DummyController(scenario, outputPath);
		OutputDirectoryHierarchy dummyOutputDirectoryHierarchy;
		if (outputPath == null) outputPath = scenario.getConfig().controler().getOutputDirectory();
		if (outputPath.endsWith("/")) {
			outputPath = outputPath.substring(0, outputPath.length() - 1);
		}
		if (scenario.getConfig().controler().getRunId() != null) {
			dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(outputPath, scenario.getConfig().controler().getRunId(), true);
		} else {
			dummyOutputDirectoryHierarchy = new OutputDirectoryHierarchy(outputPath, null, true);
		}
		
		List<MobsimListener> mobsimListeners = new ArrayList<MobsimListener>();
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		Geometry affectedArea = util.mergeGeometries(features);
		
		coordAnalyzer = new CoordAnalyzer(affectedArea);

		householdsTracker = new HouseholdsTracker(scenario);
		decisionDataProvider = new DecisionDataProvider();
		
		/*
		 * Create a DecisionDataGrabber and run notifyMobsimInitialized(...)
		 * which inserts decision data into the DecisionDataProvider.
		 */
		DecisionDataGrabber decisionDataGrabber = new DecisionDataGrabber(scenario, decisionDataProvider, coordAnalyzer, 
				householdsTracker, householdObjectAttributes);	
		
		householdsTracker.notifyMobsimInitialized(null);
		decisionDataGrabber.notifyMobsimInitialized(null);
		
		// read people in panic from file
		String panicFile = dummyInputDirectoryHierarchy.getIterationFilename(0, PanicModel.panicModelFile);
		PanicModel panicModel = new PanicModel(decisionDataProvider, EvacuationConfig.panicShare);
		panicModel.readDecisionsFromFile(panicFile);
		panicModel.printStatistics();
		
		// read pickup behavior from file
		String pickupFile = dummyInputDirectoryHierarchy.getIterationFilename(0, PickupModel.pickupModelFile);
		PickupModel pickupModel = new PickupModel(decisionDataProvider);
		pickupModel.readDecisionsFromFile(pickupFile);
		pickupModel.printStatistics();
		
		// read evacuation decisions from file
		String evacuationDecisionFile = dummyInputDirectoryHierarchy.getIterationFilename(0, EvacuationDecisionModel.evacuationDecisionModelFile);
		EvacuationDecisionModel evacuationDecisionModel = new EvacuationDecisionModel(scenario, MatsimRandom.getLocalInstance(), decisionDataProvider);
		evacuationDecisionModel.readDecisionsFromFile(evacuationDecisionFile);
		evacuationDecisionModel.printStatistics();
		
		// initialize agentActivities and agentLegs maps
		for (Id personId : scenario.getPopulation().getPersons().keySet()) {
			Id householdId = householdsTracker.getPersonsHouseholdId(personId);
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
			
			Id homeLinkId = hdd.getHomeLinkId();
			Id homeFacilityId = hdd.getHomeFacilityId();
            ActivityImpl firstActivity = new ActivityImpl("home".intern(), homeLinkId);
            firstActivity.setFacilityId(homeFacilityId);
            firstActivity.setStartTime(0.0);
            firstActivity.setEndTime(Time.UNDEFINED_TIME);

            List<Activity> activities = new ArrayList<Activity>();
            activities.add(firstActivity);
			agentActivities.put(personId, activities);
			
			agentLegs.put(personId, new ArrayList<Leg>());
		}
		
		// Create the set of analyzed modes.
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.bike);
		analyzedModes.add(TransportMode.car);
		analyzedModes.add(TransportMode.pt);
		analyzedModes.add(TransportMode.ride);
		analyzedModes.add(TransportMode.walk);
		analyzedModes.add(OldPassengerDepartureHandler.passengerTransportMode);
		
		/*
		 * Class to create dummy MobsimAfterSimStepEvents.
		 * Has to be added as first Listener to the EventsManager!
		 */
		AfterSimStepEventsCreator afterSimStepEventsCreator = new AfterSimStepEventsCreator(mobsimListeners);
		eventsManager.addHandler(afterSimStepEventsCreator);
		
		eventsManager.addHandler(householdsTracker);
		eventsManager.addHandler(this);
		
		// MobsimInitializedListener
		MobsimInitializedEvent<Mobsim> mobsimInitializedEvent = new MobsimInitializedEventImpl<Mobsim>(null);
		for(MobsimListener mobsimListener : mobsimListeners) {
			if (mobsimListener instanceof MobsimInitializedListener) {
				((MobsimInitializedListener) mobsimListener).notifyMobsimInitialized(mobsimInitializedEvent);
			}
		}

		String eventsFile = dummyInputDirectoryHierarchy.getIterationFilename(0, Controler.FILENAME_EVENTS_XML);
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);

		/*
		 * Create results.
		 */
		analyzeA();
		analyzeB();
		analyzeC();
		
		/*
		 * Write results to files.
		 */
		String countsFileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, outputCountsFile);
		String modesFileName = dummyOutputDirectoryHierarchy.getIterationFilename(0, outputModesFile);
		writeResultsToFiles(countsFileName, modesFileName);
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
					Integer count = evacuationModesA11.get(modesString);
					if (count == null) evacuationModesA11.put(modesString, 1);
					else evacuationModesA11.put(modesString, count + 1);
					
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
					Integer count = evacuationModesA21.get(modesString);
					if (count == null) evacuationModesA21.put(modesString, 1);
					else evacuationModesA21.put(modesString, count + 1);
					
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
						if (activity.getType().equals(PickupAgentReplanner.activityType)) continue;
						else if (activity.getType().equals(DropOffAgentReplanner.activityType)) continue;
						
						copy.add(activity);
					}
					
					boolean returnsHome = false;
					boolean endsAtHome = false;
					for (int i = 0; i < copy.size() - 1; i++) {
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
								if (activity.getType().equals(CurrentLegToMeetingPointReplanner.activityType)) {
									endHomeActivityTime = activity.getEndTime();
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
							count = evacuationModesB111Home.get(modesString);
							if (count == null) evacuationModesB111Home.put(modesString, 1);
							else evacuationModesB111Home.put(modesString, count + 1);
							
							modesString = CollectionUtils.setToString(modesEvacuate);
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
						Integer count = evacuationModesB12.get(modesString);
						if (count == null) evacuationModesB12.put(modesString, 1);
						else evacuationModesB12.put(modesString, count + 1);
					}
					
				} else {
					B113.add(personId);
					log.warn("Agent is not in a facility when simulation ends: " + personId);
				}
				
			} else {
				B2.add(personId);
				
				List<Leg> legs = agentLegs.get(personId);
				Set<String> modes = new TreeSet<String>();
				for (Leg leg : legs) modes.add(leg.getMode());
				String modesString = CollectionUtils.setToString(modes);
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
		log.info("\tB113\t" + "in evacuation area and not at home, home affected, still en-route (i.e. stuck/undefined):\t" + B113.size());
		log.info("\tB12\t" + "in evacuation area and not at home, home affected, evacuate immediately:\t" + B12.size());
		headers.add("B1");
		headers.add("B11");
		headers.add("B111");
		headers.add("B112");
		headers.add("B113");
		headers.add("B12");
		values.add(String.valueOf(B1.size()));
		values.add(String.valueOf(B11.size()));
		values.add(String.valueOf(B111.size()));
		values.add(String.valueOf(B112.size()));
		values.add(String.valueOf(B113.size()));
		values.add(String.valueOf(B12.size()));
		
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
						if (activity.getType().equals(PickupAgentReplanner.activityType)) continue;
						else if (activity.getType().equals(DropOffAgentReplanner.activityType)) continue;
						
						copy.add(activity);
					}
					
					boolean returnsHome = false;
					boolean endsAtHome = false;
					for (int i = 0; i < copy.size() - 1; i++) {
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
								if (activity.getType().equals(CurrentLegToMeetingPointReplanner.activityType)) {
									endHomeActivityTime = activity.getEndTime();
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
							count = evacuationModesC111Home.get(modesString);
							if (count == null) evacuationModesC111Home.put(modesString, 1);
							else evacuationModesC111Home.put(modesString, count + 1);
							
							modesString = CollectionUtils.setToString(modesEvacuate);
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
						Integer count = evacuationModesC12.get(modesString);
						if (count == null) evacuationModesC12.put(modesString, 1);
						else evacuationModesC12.put(modesString, count + 1);
					}
					
				} else {
					C113.add(personId);
					log.warn("Agent is not in a facility when simulation ends: " + personId);
				}
				
			} else {
				C2.add(personId);
				
				List<Leg> legs = agentLegs.get(personId);
				Set<String> modes = new TreeSet<String>();
				for (Leg leg : legs) modes.add(leg.getMode());
				String modesString = CollectionUtils.setToString(modes);
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
		log.info("\tC113\t" + "outside evacuation area and not at home, home affected, still en-route (i.e. stuck/undefined):\t" + C113.size());
		log.info("\tC12\t" + "outside evacuation area and not at home, home affected, evacuate immediately:\t" + C12.size());
		headers.add("C1");
		headers.add("C11");
		headers.add("C111");
		headers.add("C112");
		headers.add("C113");
		headers.add("C12");
		values.add(String.valueOf(C1.size()));
		values.add(String.valueOf(C11.size()));
		values.add(String.valueOf(C111.size()));
		values.add(String.valueOf(C112.size()));
		values.add(String.valueOf(C113.size()));
		values.add(String.valueOf(C12.size()));
		
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
	
	private void writeResultsToFiles(String countsFileName, String modesFileName) {
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(countsFileName);
			
			for (int i = 0; i < headers.size(); i++) {
				String header = headers.get(i);
				writer.write(header);
				if (i < headers.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			
			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				writer.write(value);
				if (i < values.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
		
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(modesFileName);
						
			Set<String> modes = new TreeSet<String>();
			modes.add(TransportMode.bike);
			modes.add(TransportMode.car);
			modes.add(TransportMode.ride);
			modes.add(TransportMode.pt);
			modes.add(TransportMode.walk);
			modes.add(OldPassengerDepartureHandler.passengerTransportMode);
			modes.add(OldPassengerDepartureHandler.passengerTransportMode + "," + TransportMode.walk);
						
			List<String> modeHeaders = new ArrayList<String>();
			List<String> modeValues = new ArrayList<String>();
			
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
					modeValues.add(String.valueOf(count));
				}
			}
			
			for (int i = 0; i < modeHeaders.size(); i++) {
				String header = modeHeaders.get(i);
				writer.write(header);
				if (i < modeHeaders.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			
			for (int i = 0; i < modeValues.size(); i++) {
				String value = modeValues.get(i);
				writer.write(value);
				if (i < modeValues.size() - 1) writer.write(delimiter);
				else writer.write(newLine);
			}
			writer.flush();
			writer.close();
		} catch (IOException e) {
			Gbl.errorMsg(e);
		}
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
			Id personId = scenario.createId(personIdString);
			handleEvent(new PersonInformationEventImpl(event.getTime(), personId));
		}
	}

	@Override
	public void handleEvent(PersonInformationEvent event) {
		Id personId = event.getPersonId();
		
		informedAgents.add(personId);
		
		AgentPosition agentPosition = householdsTracker.getAgentPosition(personId);
		Position position = agentPosition.getPositionType();
		if (position == Position.FACILITY) {
			Id householdId = householdsTracker.getPersonsHouseholdId(personId);
			HouseholdDecisionData hdd = decisionDataProvider.getHouseholdDecisionData(householdId);
			Id homeFacilityId = hdd.getHomeFacilityId();
			
			if (agentPosition.getPositionId().equals(homeFacilityId)) A.add(personId);
			else {
				Facility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(agentPosition.getPositionId());
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
	public void handleEvent(AgentDepartureEvent event) {
		this.enrouteDrivers.remove(event.getPersonId());
		
		Id personId = event.getPersonId();
		List<Leg> legs = agentLegs.get(personId);
		
		LegImpl leg = new LegImpl(event.getLegMode());
        leg.setDepartureTime(event.getTime());
        leg.setArrivalTime(Time.UNDEFINED_TIME);
		
		legs.add(leg);
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		Id personId = event.getPersonId();

		if (event.getLegMode().equals(TransportMode.car)) {
			this.enrouteDrivers.add(personId);
		}
		
		List<Leg> legs = agentLegs.get(personId);
		
		// If the agent is not informed yet, remove entries from the list.
		if (!informedAgents.contains(event.getPersonId())) legs.clear();
		else {
			LegImpl leg = (LegImpl) legs.get(legs.size() - 1);
			leg.setArrivalTime(event.getTime());			
		}
	}
	
	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		if (this.enrouteDrivers.contains(event.getPersonId())) {
			vehiclePositions.put(event.getVehicleId(), event.getLinkId());
			if (event.getVehicleId() == null) log.warn("null vehicleId was found!");			
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.enrouteDrivers.contains(event.getPersonId())) {
			vehiclePositions.put(event.getVehicleId(), event.getLinkId());
			if (event.getVehicleId() == null) log.warn("null vehicleId was found!");
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
	}
	
	private static class AfterSimStepEventsCreator implements BasicEventHandler {
		
		private final List<MobsimListener> mobsimListeners;
		private double lastSimStep = 0.0; 
		
		public AfterSimStepEventsCreator(List<MobsimListener> mobsimListeners) {
			this.mobsimListeners = mobsimListeners;
		}
		
		@Override
		public void handleEvent(Event event) {
			double time = event.getTime();
			while (time > lastSimStep) {
				MobsimAfterSimStepEvent<Mobsim> e = new MobsimAfterSimStepEventImpl<Mobsim>(null, lastSimStep);
				
				for(MobsimListener mobsimListener : mobsimListeners) {
					if (mobsimListener instanceof MobsimAfterSimStepListener) {
						((MobsimAfterSimStepListener) mobsimListener).notifyMobsimAfterSimStep(e);
					}
				}
				
				lastSimStep++;
			}
		}
		
		@Override
		public void reset(int iteration) {
			// nothing to do here
		}
	}

}
