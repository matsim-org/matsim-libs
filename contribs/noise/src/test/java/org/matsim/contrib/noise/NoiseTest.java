/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package org.matsim.contrib.noise;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.NoiseReceiverPoint;
import org.matsim.contrib.noise.data.PersonActivityInfo;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.contrib.noise.events.NoiseEventAffected;
import org.matsim.contrib.noise.events.NoiseEventCaused;
import org.matsim.contrib.noise.handler.NoiseEquations;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */

public class NoiseTest {
	private static final Logger log = Logger.getLogger( NoiseTest.class );

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	// Tests the NoisSpatialInfo functionality separately for each function
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config1.xml";

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile, new NoiseConfigGroup()));
				
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModule("noise");
		
		noiseParameters.setReceiverPointGap(250.);	
		noiseParameters.setScaleFactor(1.);
		
		String[] consideredActivities = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities);
		
		NoiseContext noiseContext = new NoiseContext(scenario);
		
		// test the grid of receiver points
		Assert.assertEquals("wrong number of receiver points", 16, noiseContext.getReceiverPoints().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coord for receiver point Id '10'", new Coord((double) 500, (double) 100).toString(), noiseContext.getReceiverPoints().get(Id.create(10, ReceiverPoint.class)).getCoord().toString());
		
		// test the allocation of activity coordinates to the nearest receiver point
		Assert.assertEquals("wrong nearest receiver point Id for coord 300/300 (x/y)", "5", noiseContext.getGrid().getActivityCoord2receiverPointId().get(new Coord((double) 300, (double) 300)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 150/150 (x/y)", "9", noiseContext.getGrid().getActivityCoord2receiverPointId().get(new Coord((double) 150, (double) 150)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 100/100 (x/y)", "8", noiseContext.getGrid().getActivityCoord2receiverPointId().get(new Coord((double) 100, (double) 100)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 500/500 (x/y)", "2", noiseContext.getGrid().getActivityCoord2receiverPointId().get(new Coord((double) 500, (double) 500)).toString());
					
		// test the allocation of relevant links to the receiver point
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", 3, noiseContext.getReceiverPoints().get(Id.create("15", Link.class)).getLinkId2distanceCorrection().size());
		Assert.assertEquals("wrong relevant link for receiver point Id '15'", 3, noiseContext.getReceiverPoints().get(Id.create("15", Link.class)).getLinkId2angleCorrection().size());

		// test the distance correction term
		Assert.assertEquals("wrong distance between receiver point Id '8' and link Id '1'", 8.749854822140838, noiseContext.getReceiverPoints().get(Id.create("8", ReceiverPoint.class)).getLinkId2distanceCorrection().get(Id.create("link0", Link.class)), MatsimTestUtils.EPSILON);		
		
		// test the angle correction term
		Assert.assertEquals("wrong immission angle correction for receiver point 14 and link1", -0.8913405699036482, noiseContext.getReceiverPoints().get(Id.create("14", ReceiverPoint.class)).getLinkId2angleCorrection().get(Id.create("link1", Link.class)), MatsimTestUtils.EPSILON);		

		double angle0 = 180.;
		double immissionCorrection0 = 10 * Math.log10((angle0) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 12 and link5", immissionCorrection0, noiseContext.getReceiverPoints().get(Id.create("12", ReceiverPoint.class)).getLinkId2angleCorrection().get(Id.create("link5", Link.class)), MatsimTestUtils.EPSILON);		
		
		double angle = 65.39222026185993;
		double immissionCorrection = 10 * Math.log10((angle) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 9 and link5", immissionCorrection, noiseContext.getReceiverPoints().get(Id.create("9", ReceiverPoint.class)).getLinkId2angleCorrection().get(Id.create("link5", Link.class)), MatsimTestUtils.EPSILON);		

		// for a visualization of the receiver point 8 and the relevant links, see network file
		double angle2 = 0.0000000001;
		double immissionCorrection2 = 10 * Math.log10((angle2) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link5", immissionCorrection2, noiseContext.getReceiverPoints().get(Id.create("8", ReceiverPoint.class)).getLinkId2angleCorrection().get(Id.create("link5", Link.class)), MatsimTestUtils.EPSILON);
		
		double angle3 = 84.28940686250034;
		double immissionCorrection3 = 10 * Math.log10((angle3) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link1", immissionCorrection3, noiseContext.getReceiverPoints().get(Id.create("8", ReceiverPoint.class)).getLinkId2angleCorrection().get(Id.create("link1", Link.class)), MatsimTestUtils.EPSILON);
	
		double angle4 = 180;
		double immissionCorrection4 = 10 * Math.log10((angle4) / (180));
		Assert.assertEquals("wrong immission angle correction for receiver point 8 and link0", immissionCorrection4, noiseContext.getReceiverPoints().get(Id.create("8", ReceiverPoint.class)).getLinkId2angleCorrection().get(Id.create("link0", Link.class)), MatsimTestUtils.EPSILON);
	}
	
	// tests the noise emissions, immissions, considered agent units, damages (receiver points), damages (per link), damages (per vehicle) based on the generated *.csv output
	// tests the noise events applying the average cost allocation approach
	@Test
	public final void test2a(){
		// start a simple MATSim run with a single iteration
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config2.xml";
		Config config = ConfigUtils.loadConfig(configFile ) ;
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		runTest2a( config ) ;
	}
	@Test
	public final void test2aWAccessEgress(){
		// start a simple MATSim run with a single iteration
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config2.xml";
		Config config = ConfigUtils.loadConfig(configFile ) ;
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		runTest2a( config ) ;
	}
		
	private static void runTest2a( Config runConfig ) {
		Controler controler = new Controler(runConfig);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		controler.run();
		
		// run the noise analysis for the final iteration (offline)
		
		String runDirectory = controler.getConfig().controler().getOutputDirectory() + "/";
		
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(controler.getConfig().controler().getLastIteration());
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		
		noiseParameters.setReceiverPointGap(250.);	
		
		String[] consideredActivities = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities);
		
		noiseParameters.setScaleFactor(1.);
		noiseParameters.setUseActualSpeedLevel(false);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, runDirectory);
		noiseCalculation.run();	
		
		EventsManager events = EventsUtils.createEventsManager();
				
		final Map<Id<Person>, List<Event>> eventsPerPersonId = new HashMap<Id<Person>, List<Event>>();
		
		events.addHandler(new ActivityStartEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(ActivityStartEvent event) {
				
				if(!eventsPerPersonId.containsKey(event.getPersonId())){
					eventsPerPersonId.put(event.getPersonId(), new ArrayList<Event>());
				}
				eventsPerPersonId.get(event.getPersonId()).add(event);
				
			}
		});
		
		events.addHandler(new ActivityEndEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(ActivityEndEvent event) {
				
				if(!eventsPerPersonId.containsKey(event.getPersonId())){
					eventsPerPersonId.put(event.getPersonId(), new ArrayList<Event>());
				}
				eventsPerPersonId.get(event.getPersonId()).add(event);
				
			}
		});
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
		
		// ############################
		// test considered agent units
		// ############################
		
		double sevenOclock = 25200;
		double endTime = 39600;
		double ttOclock = 79200;
		
		String separator = ";";
		String line = null;
		
		double[] timeSlots = {sevenOclock, endTime, ttOclock};
		String pathToConsideredAgentUnitsFile;
		Map<Id<ReceiverPoint>, List<Double>> consideredAgentsPerReceiverPoint = new HashMap<Id<ReceiverPoint>, List<Double>>();
		Map<String, Integer> idxFromKey = new ConcurrentHashMap<String, Integer>();
		BufferedReader br;
		
		for(double currentTimeSlot : timeSlots){
			
			pathToConsideredAgentUnitsFile = runDirectory + "analysis_it.0/consideredAgentUnits/consideredAgentUnits_" + Double.toString(currentTimeSlot) + ".csv";
			
			br = IOUtils.getBufferedReader(pathToConsideredAgentUnitsFile);
			
			try {
				
				line = br.readLine();
				
				String[] keys = line.split(separator);
				for(int i = 0; i < keys.length; i++){
					idxFromKey.put(keys[i], i);
				}
				
				int idxReceiverPointId = idxFromKey.get("Receiver Point Id");
				int idxConsideredAgentUnits = idxFromKey.get("Considered Agent Units " + Time.writeTime(currentTimeSlot, Time.TIMEFORMAT_HHMMSS));
				
				while((line = br.readLine()) != null){
					
					keys = line.split(separator);
					if(!consideredAgentsPerReceiverPoint.containsKey(Id.create(keys[idxReceiverPointId], ReceiverPoint.class))){
						consideredAgentsPerReceiverPoint.put(Id.create(keys[idxReceiverPointId], ReceiverPoint.class), new ArrayList<Double>());
					}
					
					consideredAgentsPerReceiverPoint.get(Id.create(keys[idxReceiverPointId], ReceiverPoint.class)).add(Double.parseDouble(keys[idxConsideredAgentUnits]));
					
				}
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		Map<Id<ReceiverPoint>, Double> affectedPersonsPerReceiverPoint = new HashMap<Id<ReceiverPoint>, Double>();
		
		int index = 0;
		
		for(double currentTimeSlot : timeSlots){
			
			Map<Id<ReceiverPoint>, Double> affectedPersonsPerReceiverPointTest = new HashMap<Id<ReceiverPoint>, Double>();
			
			double affectedPersons = 0.;
			
			for(Id<Person> personId : scenario.getPopulation().getPersons().keySet()){
				
				double start = 0.;
				
				for(Event e : eventsPerPersonId.get(personId)){
					
					boolean activityEnded = false;
					
					PersonActivityInfo actInfo = null;
					
					if(e.getEventType().equals("actend")){
						
						ActivityEndEvent event = (ActivityEndEvent)e;
						
						if(event.getActType().equals("home")){
							
							actInfo = new PersonActivityInfo();
							actInfo.setActivityType("home");
							actInfo.setStartTime(start);
							double end= index == 2 ? 30*3600 : event.getTime();
							actInfo.setEndTime(end);
							
							activityEnded = true;
							
						}
						
						else if(event.getActType().equals("work")){
							
							actInfo = new PersonActivityInfo();
							actInfo.setActivityType("work");
							actInfo.setStartTime(start);
							actInfo.setEndTime(event.getTime());
							
							activityEnded = true;
							
						}
						
					} else if(e.getEventType().equals("actstart")){
						
						ActivityStartEvent event = (ActivityStartEvent)e;
						
						if(event.getActType().equals("home")){
							
							if(index == 0){
								
								continue;
								
							}
							
							start = event.getTime();
							
						} else if(event.getActType().equals("work")){
							
							start = event.getTime();
							
						}
						
					}
					
					if(activityEnded){
						
						// test code of getDurationInWithinInterval from actInfo
						
						double durationInThisInterval = 0.;
						double timeIntervalStart = currentTimeSlot - noiseParameters.getTimeBinSizeNoiseComputation();
						
						if (( actInfo.getStartTime() < currentTimeSlot) && ( actInfo.getEndTime() >=  timeIntervalStart )) {
							
							if ((actInfo.getStartTime() <= timeIntervalStart) && actInfo.getEndTime() >= currentTimeSlot) {
								
								durationInThisInterval = noiseParameters.getTimeBinSizeNoiseComputation();
							
							} else if (actInfo.getStartTime() <= timeIntervalStart && actInfo.getEndTime() <= currentTimeSlot) {
								
								durationInThisInterval = actInfo.getEndTime() - timeIntervalStart;
							
							} else if (actInfo.getStartTime() >= timeIntervalStart && actInfo.getEndTime() >= currentTimeSlot) {
								
								durationInThisInterval = currentTimeSlot - actInfo.getStartTime();
							
							} else if (actInfo.getStartTime() >= timeIntervalStart && actInfo.getEndTime() <= currentTimeSlot) {
								
								durationInThisInterval = actInfo.getEndTime() - actInfo.getStartTime();
								
						
							} else {
								
								throw new RuntimeException("Unknown case. Aborting...");
							}
								
						}
						
						double durationInThisIntervalMethod = actInfo.getDurationWithinInterval(currentTimeSlot, noiseParameters.getTimeBinSizeNoiseComputation()); 
							
						Assert.assertEquals("Durations of activities do not match!", durationInThisIntervalMethod, durationInThisInterval, MatsimTestUtils.EPSILON);
							
						double unitsThisPersonActivityInfo = durationInThisInterval / noiseParameters.getTimeBinSizeNoiseComputation(); 
						affectedPersons = ( unitsThisPersonActivityInfo * noiseParameters.getScaleFactor() );
						
						int outOfHomeActIdx = 2 ;
						if ( runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
							outOfHomeActIdx = 6 ;
						}
						Coord coord = actInfo.getActivityType().equals("home") ?
								((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(0)).getCoord() :
								((Activity) scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(outOfHomeActIdx)).getCoord();
							
						Id<ReceiverPoint> rpId = noiseCalculation.getNoiseContext().getGrid().getActivityCoord2receiverPointId().get(coord);
						if ( rpId==null ) {
							log.warn( "coord=" + coord );
							Gbl.assertNotNull( rpId );
						}
						
						if(!affectedPersonsPerReceiverPointTest.containsKey(rpId)){
							
							affectedPersonsPerReceiverPointTest.put(rpId, affectedPersons);
							
						} else{
							
							double n = affectedPersonsPerReceiverPointTest.get(rpId);
							affectedPersonsPerReceiverPointTest.put(rpId, n + affectedPersons);
							
						}
						
					}
					
				}
				
			}
			
			if(currentTimeSlot == endTime){
				
				affectedPersonsPerReceiverPoint = affectedPersonsPerReceiverPointTest;
				// ??? kai, feb'16
				
				if ( runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
					Assert.assertEquals("Wrong number of affected persons at receiver point 16", 1.991388888888, 
							affectedPersonsPerReceiverPointTest.get(Id.create("16", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
					Assert.assertEquals("Wrong number of affected persons at receiver point 0", 0.479722222222222, 
							affectedPersonsPerReceiverPointTest.get(Id.create("0", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("Wrong number of affected persons at receiver point 16", 2.35305555555555, 
							affectedPersonsPerReceiverPointTest.get(Id.create("16", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
					Assert.assertEquals("Wrong number of affected persons at receiver point 0", 0.479722222222222, 
							affectedPersonsPerReceiverPointTest.get(Id.create("0", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
				}
				
			}
			
			for(Id<ReceiverPoint> receiverPointId : affectedPersonsPerReceiverPointTest.keySet()){
				final Double expected = affectedPersonsPerReceiverPointTest.get(receiverPointId);
				if ( expected==null ) {
					log.warn( "receiverPointId:" + receiverPointId );
					log.warn( "affected:" + affectedPersonsPerReceiverPointTest ) ;
				}

				final List<Double> list = consideredAgentsPerReceiverPoint.get(receiverPointId);
				if ( list==null ) {
					log.warn( "receiverPointId:" + receiverPointId );
					log.warn( "affected:" + consideredAgentsPerReceiverPoint ) ;
				}
				final Double actual = list.get(index);
				if ( actual==null ) {
					log.warn( "receiverPointId:" + receiverPointId );
					log.warn( "affected:" + list ) ;
				}
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
					Assert.assertEquals("Wrong number of affected persons", expected, actual, MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("Wrong number of affected persons", expected, actual, MatsimTestUtils.EPSILON);
				}
				
			}
			
			index++;
			
		}

		// #################################
		// test emissions per link and time
		// #################################

		line = null;
		
		String pathToEmissionsFile = runDirectory + "analysis_it.0/emissions/emission_" + Double.toString(endTime) + ".csv";
		
		Map<Id<Link>, Double> emissionsPerLink = new HashMap<Id<Link>, Double>();
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToEmissionsFile);
		
		try {
			
			line = br.readLine();
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxLinkId = idxFromKey.get("Link Id");
			int idxNoiseEmission = idxFromKey.get("Noise Emission " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				emissionsPerLink.put(Id.create(keys[idxLinkId], Link.class), Double.parseDouble(keys[idxNoiseEmission]));
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Map<Id<Link>, Integer> amountOfVehiclesPerLink = new HashMap<Id<Link>, Integer>();
		
		for(NoiseEventCaused event: noiseCalculation.getTimeTracker().getNoiseEventsCaused()){
			
			if(event.getEmergenceTime() >= endTime - 3600 && event.getEmergenceTime() <= endTime){
				
				Id<Link> linkId = event.getLinkId();
				int amount = 1;
				
				if(amountOfVehiclesPerLink.containsKey(linkId)){
					
					amount = amountOfVehiclesPerLink.get(linkId) + 1;
					amountOfVehiclesPerLink.put(linkId, amount);
					
				} else{
					
					amountOfVehiclesPerLink.put(linkId, amount);
					
				}
				
			}
			
		}
		
		Map<Id<Link>,Double> noiseEmissionsPerLink = new HashMap<Id<Link>,Double>();
		
		for(Id<Link> linkId : scenario.getNetwork().getLinks().keySet()){
			noiseEmissionsPerLink.put(linkId, 0.);
		}
		
		for(Id<Link> linkId : amountOfVehiclesPerLink.keySet()){

			double vCar = (scenario.getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
			double vHdv = vCar;
			double p = 0;
			int n = amountOfVehiclesPerLink.size();
			
			double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
			double Dv = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHdv, p);
			double noiseEmission = mittelungspegel + Dv;
			
			Assert.assertEquals("Wrong amount of emission!", noiseEmission, emissionsPerLink.get(linkId), MatsimTestUtils.EPSILON);
			noiseEmissionsPerLink.put(linkId, noiseEmission);
			
		}
		
		Assert.assertEquals("Wrong amount of emission!", 56.4418948379387, noiseEmissionsPerLink.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong amount of emission!", 86.4302864851097, noiseEmissionsPerLink.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong amount of emission!", 0., noiseEmissionsPerLink.get(Id.create("link4", Link.class)), MatsimTestUtils.EPSILON);
		
		
		// ############################################
		// test immissions per receiver point and time
		// ############################################

		line = null;
		
		String pathToImmissionsFile = runDirectory + "analysis_it.0/immissions/immission_" + Double.toString(endTime) + ".csv";
		
		Map<Id<ReceiverPoint>, Double> immissionPerReceiverPointId = new HashMap<Id<ReceiverPoint>, Double>();
		
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToImmissionsFile);
		
		try {
			
			line = br.readLine();
			
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxReceiverPointId = idxFromKey.get("Receiver Point Id");
			int idxImmission = idxFromKey.get("Immission " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				immissionPerReceiverPointId.put(Id.create(keys[idxReceiverPointId], ReceiverPoint.class), Double.parseDouble(keys[idxImmission]));
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(NoiseReceiverPoint rp : noiseCalculation.getNoiseContext().getReceiverPoints().values()){
			
			Map<Id<Link>, Double> linkId2IsolatedImmission = new HashMap<Id<Link>, Double>();
			
			for(Id<Link> linkId : rp.getLinkId2distanceCorrection().keySet()){
				
				double noiseImmission = 0;
				
				if(emissionsPerLink.get(linkId) > 0){
					
					noiseImmission = emissionsPerLink.get(linkId) + rp.getLinkId2distanceCorrection().get(linkId) + rp.getLinkId2angleCorrection().get(linkId);
					
					if(noiseImmission < 0) noiseImmission = 0;
					
				}
				
				linkId2IsolatedImmission.put(linkId, noiseImmission);
				
			}
			
			Assert.assertEquals("Wrong amount of immission!", NoiseEquations.calculateResultingNoiseImmission(linkId2IsolatedImmission.values()), immissionPerReceiverPointId.get(rp.getId()), MatsimTestUtils.EPSILON);
				
		}
		
		Assert.assertEquals("Wrong amount of immission!", 77.2591534246579, immissionPerReceiverPointId.get(Id.create("15", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong amount of immission!", 67.9561670074151, immissionPerReceiverPointId.get(Id.create("31", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong amount of immission!", 0., immissionPerReceiverPointId.get(Id.create("0", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		
		// ############################################
		// test damages per receiver point and time
		// ############################################

		line = null;
		
		String pathToDamagesFile = runDirectory + "analysis_it.0/damages_receiverPoint/damages_receiverPoint_" + Double.toString(endTime) + ".csv";
		
		Map<Id<ReceiverPoint>, Double> damagesPerReceiverPointId = new HashMap<Id<ReceiverPoint>, Double>();
		
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToDamagesFile);
		
		try {
			
			line = br.readLine();
			
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxReceiverPointId = idxFromKey.get("Receiver Point Id");
			int idxDamages = idxFromKey.get("Damages " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				damagesPerReceiverPointId.put(Id.create(keys[idxReceiverPointId], ReceiverPoint.class), Double.parseDouble(keys[idxDamages]));
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for(ReceiverPoint rp : noiseCalculation.getNoiseContext().getReceiverPoints().values()){
			
			double noiseImmission = immissionPerReceiverPointId.get(rp.getId());
			double affectedAgentUnits = consideredAgentsPerReceiverPoint.get(rp.getId()).get(1);
			
			Assert.assertEquals("Wrong damage!", NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, endTime, noiseParameters.getAnnualCostRate(), noiseParameters.getTimeBinSizeNoiseComputation()), damagesPerReceiverPointId.get(rp.getId()),  MatsimTestUtils.EPSILON);
			
		}
		
		if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			Assert.assertEquals("Wrong damage!", 0.0664164095284536, 
					damagesPerReceiverPointId.get(Id.create("16", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Wrong damage!", 0.05620815014, 
					damagesPerReceiverPointId.get(Id.create("16", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		}
		Assert.assertEquals("Wrong damage!", 0., damagesPerReceiverPointId.get(Id.create("0", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		
		// ############################################
		// test average damages per link and time
		// ############################################
		
		// noise level at receiver point '16': 69.65439464
		
		// relevant link IDs
		// link2: noise contribution: 50.45464287410944 -->  share: 0.01202333 --> damage costs:  0.00079855 
		// linkA5: noise contribution: 69.60186152606298 ---> share: 0.98797667 --> damage costs: 0.06561786
		// linkB5: noise contribution: 0
		
		line = null;
		
		String pathToDamageLinkFile = runDirectory + "analysis_it.0/average_damages_link/average_damages_link_" + Double.toString(endTime) + ".csv";
		
		Map<Id<Link>, Double> damagesPerlinkId = new HashMap<Id<Link>, Double>();
		
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToDamageLinkFile);
		
		try {
			
			line = br.readLine();
			
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxLinkId = idxFromKey.get("Link Id");
			int idxDamages = idxFromKey.get("Damages " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				damagesPerlinkId.put(Id.create(keys[idxLinkId], Link.class), Double.parseDouble(keys[idxDamages]));
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			Assert.assertEquals("Wrong link's damage contribution!", 0.00079854651258, 
					damagesPerlinkId.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong link's damage contribution!", 0.06561786301587, 
					damagesPerlinkId.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Wrong link's damage contribution!", 0.00067580922544, 
					damagesPerlinkId.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong link's damage contribution!", 0.0555323409232, 
					damagesPerlinkId.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
		}
		Assert.assertEquals("Wrong link's damage contribution!", 0., damagesPerlinkId.get(Id.create("linkB5", Link.class)), MatsimTestUtils.EPSILON);
				
		// ############################################
		// test average damages per link, car and time
		// ############################################
		
		line = null;
		
		String pathToDamageLinkCar = runDirectory + "analysis_it.0/average_damages_link_car/average_damages_link_car_" + Double.toString(endTime) + ".csv";
		
		Map<Id<Link>, Double> damagesPerCar = new HashMap<Id<Link>, Double>();
		
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToDamageLinkCar);
		
		try {
			
			line = br.readLine();
			
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxlinkId = idxFromKey.get("Link Id");
			int idxDamages = idxFromKey.get("Average damages per car " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				damagesPerCar.put(Id.create(keys[idxlinkId], Link.class), Double.parseDouble(keys[idxDamages]));
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			Assert.assertEquals("Wrong damage per car per link!", 0.00079854651258 / 2.0, 
					damagesPerCar.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong damage per car per link!", 0.06561786301587 / 2.0, 
					damagesPerCar.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Wrong damage per car per link!", 0.00033790461272075167, 
					damagesPerCar.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong damage per car per link!", 0.027766170461620, 
					damagesPerCar.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
		}
		Assert.assertEquals("Wrong damage per car per link!", 0., 
				damagesPerCar.get(Id.create("linkB5", Link.class)), MatsimTestUtils.EPSILON);
				
		line = null;
		
		String pathToMarginalDamageLinkCar = runDirectory + "analysis_it.0/marginal_damages_link_car/marginal_damages_link_car_" + Double.toString(endTime) + ".csv";
		
		Map<Id<Link>, Double> marginaldamagesPerCar = new HashMap<Id<Link>, Double>();
		
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToMarginalDamageLinkCar);
		
		try {
			
			line = br.readLine();
			
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxlinkId = idxFromKey.get("Link Id");
			int idxDamages = idxFromKey.get("Marginal damages per car " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				marginaldamagesPerCar.put(Id.create(keys[idxlinkId], Link.class), Double.parseDouble(keys[idxDamages]));
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
					
		if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			Assert.assertEquals("Wrong damage per car per link!", 0.00011994155845965193, 
					marginaldamagesPerCar.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong damage per car per link!", 0.008531432493391652, 
					marginaldamagesPerCar.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong damage per car per link!", 3.440988380343235E-8, 
					marginaldamagesPerCar.get(Id.create("linkB5", Link.class)), MatsimTestUtils.EPSILON);
		} else {
			Assert.assertEquals("Wrong damage per car per link!", 0.00010150643756312, 
					marginaldamagesPerCar.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong damage per car per link!", 0.007220143967078839, 
					marginaldamagesPerCar.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong damage per car per link!", 2.9121055004910357E-8, 
					marginaldamagesPerCar.get(Id.create("linkB5", Link.class)), MatsimTestUtils.EPSILON);
		}
		
		// ############################################
		// test the noise-specific events
		// ############################################

		boolean tested = false;
		int counter = 0;
		for (NoiseEventCaused event : noiseCalculation.getTimeTracker().getNoiseEventsCaused()) {
			tested = true;

			if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test1", Vehicle.class).toString()))) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.0328089315079348, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.027766170461620, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test2", Vehicle.class).toString()))) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.0328089315079348, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.027766170461620, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("link2", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test1", Vehicle.class).toString()))) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 3.992732562920194E-4, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 3.379046127207E-4, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("link2", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test2", Vehicle.class).toString()))) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 3.992732562920194E-4, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 3.379046127207E-4, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter++;
			} else {
				Assert.assertEquals("There should either be no further events, or the amount should be zero.", 0., event.getAmount(), MatsimTestUtils.EPSILON);
			}
		}		
		Assert.assertTrue("No event found to be tested.", tested);
		Assert.assertEquals("Wrong number of total events.", 4, counter, MatsimTestUtils.EPSILON);
		
		boolean tested2 = false;
		int counter2 = 0;
		for (NoiseEventAffected event : noiseCalculation.getTimeTracker().getNoiseEventsAffected()) {
			tested2 = true;

			if (event.getEmergenceTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test1", Person.class).toString())) && event.getActType().equals("work") ) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.020745817449213576, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.0156416877593, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter2++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test2", Person.class).toString())) && event.getActType().equals("work")) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.017444990107520864, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.01234086041763, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter2++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test3", Person.class).toString())) && event.getActType().equals("home")) {
				if ( !runConfig.plansCalcRoute().isInsertingAccessEgressWalk() ) {				
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.028225601971719153, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				} else {
					Assert.assertEquals("wrong cost per car for the given link and time interval", 0.028225601971719153, 
							event.getAmount(), MatsimTestUtils.EPSILON);
				}
				counter2++;
			} else {
				Assert.assertEquals("There should either be no further events, or the amount should be zero.", 0., event.getAmount(), MatsimTestUtils.EPSILON);
			}
			
		}		
		Assert.assertTrue("No event found to be tested.", tested2);
		Assert.assertEquals("Wrong number of total events.", 3, counter2, MatsimTestUtils.EPSILON);
		
	 }
	
	// same test as before, but using the marginal cost approach
	@Test
	public final void test2b(){
		
		// start a simple MATSim run with a single iteration
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config2.xml";
		Config runConfig = ConfigUtils.loadConfig( configFile ) ;

		runConfig.plansCalcRoute().setInsertingAccessEgressWalk(false);
		// I made test2a test both versions, but I don't really want to do that work again myself. kai, feb'16 
		
		Controler controler = new Controler(runConfig);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		controler.run();
		
		// run the noise analysis for the final iteration (offline)
		
		String runDirectory = controler.getConfig().controler().getOutputDirectory() + "/";

		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(controler.getConfig().controler().getLastIteration());
						
		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		noiseParameters.setReceiverPointGap(250.);	
		
		String[] consideredActivities = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities);
		
		noiseParameters.setScaleFactor(1.);
		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);
		noiseParameters.setUseActualSpeedLevel(false);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, runDirectory);
		noiseCalculation.run();	
							
		// ############################################
		// test the noise-specific events
		// ############################################

		boolean tested = false;
		int counter = 0;
		for (NoiseEventCaused event : noiseCalculation.getTimeTracker().getNoiseEventsCaused()) {
			tested = true;

			if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test1", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.008531432493391652, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test2", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.008531432493391652, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("link2", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test1", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.00011994155845965193, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("link2", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test2", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.00011994155845965193, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else {
				Assert.assertEquals("There should either be no further events, or the amount should be zero.", 0., event.getAmount(), MatsimTestUtils.EPSILON);
			}
		}		
		Assert.assertTrue("No event found to be tested.", tested);
		Assert.assertEquals("Wrong number of total events.", 4, counter, MatsimTestUtils.EPSILON);
		
		boolean tested2 = false;
		int counter2 = 0;
		for (NoiseEventAffected event : noiseCalculation.getTimeTracker().getNoiseEventsAffected()) {
			tested2 = true;

			if (event.getEmergenceTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test1", Person.class).toString())) && event.getActType().equals("work") ) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.020745817449213576, event.getAmount(), MatsimTestUtils.EPSILON);
				counter2++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test2", Person.class).toString())) && event.getActType().equals("work")) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.017444990107520864, event.getAmount(), MatsimTestUtils.EPSILON);
				counter2++;
			} else if (event.getEmergenceTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test3", Person.class).toString())) && event.getActType().equals("home")) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.028225601971719153, event.getAmount(), MatsimTestUtils.EPSILON);
				counter2++;
			} else {
				Assert.assertEquals("There should either be no further events, or the amount should be zero.", 0., event.getAmount(), MatsimTestUtils.EPSILON);
			}
			
		}		
		Assert.assertTrue("No event found to be tested.", tested2);
		Assert.assertEquals("Wrong number of total events.", 3, counter2, MatsimTestUtils.EPSILON);
		
	 }
	
	// same test as 2a, but using the actual speed level
	@Test
	public final void test2c(){
		
		// start a simple MATSim run with a single iteration
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config2.xml";
		Controler controler = new Controler(configFile);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
		controler.run();
		
		// run the noise analysis for the final iteration (offline)
		
		String runDirectory = controler.getConfig().controler().getOutputDirectory() + "/";

		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(controler.getConfig().controler().getLastIteration());
						
		// adjust the default noise parameters
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		noiseParameters.setReceiverPointGap(250.);	
		
		String[] consideredActivities = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivities);
		
		noiseParameters.setScaleFactor(1.);
		noiseParameters.setUseActualSpeedLevel(true);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(true);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, runDirectory);
		noiseCalculation.run();		
		
		EventsManager events = EventsUtils.createEventsManager();
				
		final Map<Id<Person>, List<Event>> eventsPerPersonId = new HashMap<Id<Person>, List<Event>>();
		
		events.addHandler(new ActivityStartEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(ActivityStartEvent event) {
				
				if(!eventsPerPersonId.containsKey(event.getPersonId())){
					eventsPerPersonId.put(event.getPersonId(), new ArrayList<Event>());
				}
				eventsPerPersonId.get(event.getPersonId()).add(event);
				
			}
		});
		
		events.addHandler(new ActivityEndEventHandler() {
			
			@Override
			public void reset(int iteration) {
				
			}
			
			@Override
			public void handleEvent(ActivityEndEvent event) {
				
				if(!eventsPerPersonId.containsKey(event.getPersonId())){
					eventsPerPersonId.put(event.getPersonId(), new ArrayList<Event>());
				}
				eventsPerPersonId.get(event.getPersonId()).add(event);
				
			}
		});
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz");
				
		double endTime = 39600;
		
		String separator = ";";
		String line = null;
		
		Map<String, Integer> idxFromKey = new ConcurrentHashMap<String, Integer>();
		BufferedReader br;

		// #################################
		// test emissions per link and time
		// #################################

		line = null;
		
		String pathToEmissionsFile = runDirectory + "analysis_it.0/emissions/emission_" + Double.toString(endTime) + ".csv";
		
		Map<Id<Link>, Double> emissionsPerLink = new HashMap<Id<Link>, Double>();
		idxFromKey = new ConcurrentHashMap<String, Integer>();
		
		br = IOUtils.getBufferedReader(pathToEmissionsFile);
		
		try {
			
			line = br.readLine();
			String[] keys = line.split(separator);
			for(int i = 0; i < keys.length; i++){
				idxFromKey.put(keys[i], i);
			}
			
			int idxLinkId = idxFromKey.get("Link Id");
			int idxNoiseEmission = idxFromKey.get("Noise Emission " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS));
			
			while((line = br.readLine()) != null){
				
				keys = line.split(separator);
				emissionsPerLink.put(Id.create(keys[idxLinkId], Link.class), Double.parseDouble(keys[idxNoiseEmission]));
				
			}
		
			Assert.assertEquals("Wrong amount of emission!", 56.4418948379387, emissionsPerLink.get(Id.create("link2", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong amount of emission!", 77.3994680630406, emissionsPerLink.get(Id.create("linkA5", Link.class)), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong amount of emission!", 0., emissionsPerLink.get(Id.create("link4", Link.class)), MatsimTestUtils.EPSILON);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	 }
	
	// tests the static methods within class "noiseEquations"
	@Test
	public final void test3(){
		
		double p = 0;
		double pInPercent = 0;
		double vCar = 100;
		double vHgv = vCar;
		
		// test speed correction term
		double eCar = 27.7 + 10 * Math.log10( 1 + Math.pow((0.02 * vCar), 3) );
		double eHgv = 23.1 + 12.5 * Math.log10( vHgv );
				
		double expectedEcar = 37.2424250943932;
		double expectedEhgv = 48.1;
				
		Assert.assertEquals("Error in deviation term for speed correction (car)", expectedEcar, eCar, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in deviation term for speed correction (car)", expectedEcar, NoiseEquations.calculateLCar(vCar), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in deviation term for speed correction (hgv)", expectedEhgv, eHgv, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in deviation term for speed correction (hgv)", expectedEhgv, NoiseEquations.calculateLHdv(vHgv), MatsimTestUtils.EPSILON);
				
		Assert.assertTrue("Error in deviation term for speed correction (eCar > eHgv)", eCar < eHgv);
		
		// test mittelungspegel and speed correction
		
		for(double nHgvs = 0; nHgvs < 3; nHgvs++){
			
			for(double nCars = 0; nCars < 3; nCars++){
				
				int n = (int) (nCars + nHgvs);
				
				if(n > 0){
					p = nHgvs / n;
					pInPercent = 100 * p;
				}
				
				// test computation of mittelungspegel
				double mittelungspegel = 37.3 + 10 * Math.log10( n * ( 1 + 0.082 * pInPercent ) );
				
				double expectedMittelungspegel = Double.NEGATIVE_INFINITY;
				
				if(nHgvs == 0){
					if(nCars == 1) expectedMittelungspegel = 37.3;
					else if(nCars == 2) expectedMittelungspegel = 40.3102999566398;
				} else{
					if( nHgvs == 1){
						if( nCars == 0) expectedMittelungspegel = 46.9378782734556;
						else if( nCars == 1) expectedMittelungspegel = 47.3860017176192;
						else if(nCars == 2) expectedMittelungspegel = 47.7921802267018;
					}
					else if( nHgvs == 2){
						if( nCars == 0) expectedMittelungspegel = 49.9481782300954;
						else if( nCars == 1) expectedMittelungspegel = 50.1780172993023;
						else if( nCars == 2) expectedMittelungspegel = 50.396301674259;
					}
				}
				
				Assert.assertEquals("Error while calculating Mittelungspegel for " + nCars + " car(s) and " + nHgvs + " hgv(s)!", expectedMittelungspegel, mittelungspegel, MatsimTestUtils.EPSILON);
				Assert.assertEquals("Error while calculating Mittelungspegel for " + nCars + " car(s) and " + nHgvs + " hgv(s)!", expectedMittelungspegel, NoiseEquations.calculateMittelungspegelLm(n, p), MatsimTestUtils.EPSILON);
				
				//test speed correction
				double speedCorrection = expectedEcar - 37.3 + 10 * Math.log10( (100 + ( Math.pow(10, 0.1*(expectedEhgv - expectedEcar)) - 1 ) * pInPercent ) / (100 + 8.23*pInPercent) );
				
				double expectedSpeedCorrection = -0.0575749056067494;
				
				if(p == 1./3.) expectedSpeedCorrection = 0.956336446449128;
				else if(p == 0.5) expectedSpeedCorrection = 1.04384127904235;
				else if(p == 2./3.) expectedSpeedCorrection = 1.09354779994927;
				else if( p == 1) expectedSpeedCorrection = 1.14798298974089;
				
				Assert.assertEquals("Error while calculating speed correction term for p = " + p + "!", expectedSpeedCorrection, speedCorrection, MatsimTestUtils.EPSILON);
				Assert.assertEquals("Error while calculating speed correction term for p = " + p + "!", expectedSpeedCorrection, NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHgv, p), MatsimTestUtils.EPSILON);

			}
			
		}
		
		// test distance correction term
		for(double distance = 5.; distance <= 140; distance += 45.){
			
			double distanceCorrection = 15.8 - 10 * Math.log10( distance ) - 0.0142 * Math.pow(distance, 0.9);
			
			double expectedDistanceCorrection = 0.;
			
			if(distance == 5) expectedDistanceCorrection = 8.74985482214084;
			else if(distance == 50) expectedDistanceCorrection = -1.66983281320262;
			else if(distance == 95) expectedDistanceCorrection = -4.8327746143211;
			else if(distance == 140) expectedDistanceCorrection = -6.87412053759382;
			
			Assert.assertEquals("Error while calculating distance correction term!", expectedDistanceCorrection, distanceCorrection, MatsimTestUtils.EPSILON);
			Assert.assertEquals("Error while calculating distance correction term!", expectedDistanceCorrection, NoiseEquations.calculateDistanceCorrection(distance), MatsimTestUtils.EPSILON);
			
		}
		
		// test angle correction term
		for(double angle = 45; angle <= 360; angle += 45){
			
			double angleCorrection = 10 * Math.log10( angle / 180 );
			
			double expectedAngleCorrection = 0.;
			
			if(angle == 45) expectedAngleCorrection = -6.02059991327962;
			else if(angle == 90) expectedAngleCorrection = -3.01029995663981;
			else if(angle == 135) expectedAngleCorrection = -1.249387366083;
			else if(angle == 180) expectedAngleCorrection = 0.;
			else if(angle == 225) expectedAngleCorrection = 0.969100130080564;
			else if(angle == 270) expectedAngleCorrection = 1.76091259055681;
			else if(angle == 315) expectedAngleCorrection = 2.43038048686294;
			else if(angle == 360) expectedAngleCorrection = 3.01029995663981;
			
			Assert.assertEquals("Error while calculating angle correction term!", expectedAngleCorrection, angleCorrection, MatsimTestUtils.EPSILON);
			Assert.assertEquals("Error while calculating angle correction term!", expectedAngleCorrection, NoiseEquations.calculateAngleCorrection(angle), MatsimTestUtils.EPSILON);
			
		}
		
		//test resulting noise immission
		double distance1 = 120;
		double angle1 = 120;
		double emission1 = 49;
		double distance2 = 5;
		double angle2 = 234;
		double emission2 = 0.;
		double distance3 = 399;
		double angle3 = 10;
		double emission3 = 50;
		
		double distanceCorrection1 = 15.8 - 10 * Math.log10( distance1 ) - 0.0142 * Math.pow(distance1, 0.9);
		double angleCorrection1 = 10 * Math.log10( angle1 / 180 );
		double distanceCorrection2 = 15.8 - 10 * Math.log10( distance2 ) - 0.0142 * Math.pow(distance2, 0.9);
		double angleCorrection2 = 10 * Math.log10( angle2 / 180 );
		double distanceCorrection3 = 15.8 - 10 * Math.log10( distance3 ) - 0.0142 * Math.pow(distance3, 0.9);
		double angleCorrection3 = 10 * Math.log10( angle3 / 180 );
		
		double i1 = emission1 + distanceCorrection1 + angleCorrection1;
		double i2 = emission2 + distanceCorrection2 + angleCorrection2;
		double i3 = emission3 + distanceCorrection3 + angleCorrection3;
		
		double[] immissionsArray = {i1,i2,i3};
		
		List<Double> immissions = new ArrayList<Double>();
		immissions.add(i1);
		immissions.add(i2);
		immissions.add(i3);
		
		double tmp = 0.;
		
		for(double d : immissionsArray){
			tmp += Math.pow(10, 0.1*d);
		}
		
		double resultingNoiseImmission = 10*Math.log10(tmp);
		double expectedResultingNoiseImmission = 41.279204220881;
		
		Assert.assertEquals("Error in noise immission calculation!", expectedResultingNoiseImmission, resultingNoiseImmission, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in noise immission calculation!", expectedResultingNoiseImmission, NoiseEquations.calculateResultingNoiseImmission(immissions), MatsimTestUtils.EPSILON);
		
		//test noise damage
		double annualCostRate = (85.0/(1.95583)) * (Math.pow(1.02, (2014-1995)));
		
		double thresholdDay = 50;
		double thresholdEvening = 45;
		double thresholdNight = 40;
		
		int nPersons = 4;
		
		double costsDay = resultingNoiseImmission > thresholdDay ? annualCostRate * 3600/(365*24) * nPersons/3600 * Math.pow(2, 0.1 * (resultingNoiseImmission - thresholdDay)) : 0.;
		double costsEvening = resultingNoiseImmission > thresholdEvening ? annualCostRate * 3600/(365*24) * nPersons/3600 * Math.pow(2, 0.1 * (resultingNoiseImmission - thresholdEvening)) : 0.;
		double costsNight = resultingNoiseImmission > thresholdNight ? annualCostRate * 3600/(365*24) * nPersons/3600 * Math.pow(2, 0.1 * (resultingNoiseImmission - thresholdNight)) : 0.;
		
		double expectedCostsDay = 0.;
		double expectedCostsEvening = 0.;
		double expectedCostsNight = 0.031590380365211;
		
		Assert.assertEquals("Error in damage calculation!", expectedCostsDay, costsDay , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in damage calculation!", expectedCostsDay, NoiseEquations.calculateDamageCosts(resultingNoiseImmission, nPersons, 7.*3600, annualCostRate, 3600.) , MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in damage calculation!", expectedCostsEvening, costsEvening, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in damage calculation!", expectedCostsEvening, NoiseEquations.calculateDamageCosts(resultingNoiseImmission, nPersons, 19.*3600, annualCostRate, 3600.), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in damage calculation!", expectedCostsNight, costsNight, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Error in damage calculation!", expectedCostsNight, NoiseEquations.calculateDamageCosts(resultingNoiseImmission, nPersons, 23.*3600, annualCostRate, 3600.), MatsimTestUtils.EPSILON);	
	}	
	
	// tests the static methods within class "noiseEquations"
	@Test
	public final void test4(){
		
		double vCar = 0.0496757749985181;
		double vHGV = 0.0478758773550055;
		int nCar = 119;
		int nHGV = 4;
		
		int n = (nCar + nHGV) * 10;
		
		double p = ( (double) nHGV / (double) (nCar + nHGV));	
					
		double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
		Assert.assertEquals("Wrong mittelungspegel for n="+ n + " and p=" + p + "!", 69.22567453336540, mittelungspegel, MatsimTestUtils.EPSILON);
		
		double lCar = NoiseEquations.calculateLCar(vCar);
		Assert.assertEquals("Wrong LCar for vCar="+ vCar + "!", 27.70000000425900, lCar, MatsimTestUtils.EPSILON);
		
		double lHGV = NoiseEquations.calculateLHdv(vHGV);
		Assert.assertEquals("Wrong LHGV for vHGV="+ vHGV + "!", 6.60145932205085, lHGV, MatsimTestUtils.EPSILON);

		double dV = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHGV, p);
		Assert.assertEquals("Wrong Dv!", -10.772415234056300, dV, MatsimTestUtils.EPSILON);
		
		double emission = mittelungspegel + dV;
		Assert.assertEquals("Wrong emission!", 58.453259299309124, emission, MatsimTestUtils.EPSILON);
	
		// plus one car
		
		int nPlusOneCar = (nCar+1 + nHGV) * 10;
		double pPlusOneCar = ( (double) nHGV / (double) ((nCar + 1) + nHGV));	
		double mittelungspegelPlusOneCar = NoiseEquations.calculateMittelungspegelLm(nPlusOneCar, pPlusOneCar);			
		double dVPlusOneCar = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHGV, pPlusOneCar);
		double emissionPlusOneCar = mittelungspegelPlusOneCar + dVPlusOneCar;
		Assert.assertEquals("Wrong emission!", 58.4896140186478, emissionPlusOneCar, MatsimTestUtils.EPSILON);
		
		// plus one HGV
		
		int nPlusOneHGV = (nCar + nHGV + 1) * 10;
		double pPlusOneHGV = ( (double) (nHGV + 1) / (double) (nCar + (nHGV + 1)));	
		double mittelungspegelPlusOneHGV = NoiseEquations.calculateMittelungspegelLm(nPlusOneHGV, pPlusOneHGV);			
		double dVPlusOneHGV = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHGV, pPlusOneHGV);
		double emissionPlusOneHGV = mittelungspegelPlusOneHGV + dVPlusOneHGV;
		Assert.assertEquals("Wrong emission!", 58.4529399949061, emissionPlusOneHGV, MatsimTestUtils.EPSILON);

	}
	
	// tests the static methods within class "noiseEquations" - other speed levels
	@Test
	public final void test5(){
		
		double vCar = 30;
		double vHGV = 30;
		int nCar = 119;
		int nHGV = 4;
		
		int n = (nCar + nHGV) * 10;
		
		double p = ( (double) nHGV / (double) (nCar + nHGV));	
					
		double mittelungspegel = NoiseEquations.calculateMittelungspegelLm(n, p);
		Assert.assertEquals("Wrong mittelungspegel for n="+ n + " and p=" + p + "!", 69.22567453336540, mittelungspegel, MatsimTestUtils.EPSILON);
		
		double lCar = NoiseEquations.calculateLCar(vCar);
		Assert.assertEquals("Wrong LCar for vCar="+ vCar + "!", 28.54933574936720, lCar, MatsimTestUtils.EPSILON);
		
		double lHGV = NoiseEquations.calculateLHdv(vHGV);
		Assert.assertEquals("Wrong LHGV for vHGV="+ vHGV + "!", 41.56401568399580, lHGV, MatsimTestUtils.EPSILON);

		double dV = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHGV, p);
		Assert.assertEquals("Wrong Dv!", -7.689390421466860, dV, MatsimTestUtils.EPSILON);
		
		double emission = mittelungspegel + dV;
		Assert.assertEquals("Wrong emission!", 61.5362841118986, emission, MatsimTestUtils.EPSILON);
	
		// plus one car
		
		int nPlusOneCar = (nCar+1 + nHGV) * 10;
		double pPlusOneCar = ( (double) nHGV / (double) ((nCar + 1) + nHGV));	
		double mittelungspegelPlusOneCar = NoiseEquations.calculateMittelungspegelLm(nPlusOneCar, pPlusOneCar);			
		double dVPlusOneCar = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHGV, pPlusOneCar);
		double emissionPlusOneCar = mittelungspegelPlusOneCar + dVPlusOneCar;
		Assert.assertEquals("Wrong emission!", 61.5580658162266, emissionPlusOneCar, MatsimTestUtils.EPSILON);
		
		// plus one HGV
		
		int nPlusOneHGV = (nCar + nHGV + 1) * 10;
		double pPlusOneHGV = ( (double) (nHGV + 1) / (double) (nCar + (nHGV + 1)));	
		double mittelungspegelPlusOneHGV = NoiseEquations.calculateMittelungspegelLm(nPlusOneHGV, pPlusOneHGV);			
		double dVPlusOneHGV = NoiseEquations.calculateGeschwindigkeitskorrekturDv(vCar, vHGV, pPlusOneHGV);
		double emissionPlusOneHGV = mittelungspegelPlusOneHGV + dVPlusOneHGV;
		Assert.assertEquals("Wrong emission!", 61.9518310976080, emissionPlusOneHGV, MatsimTestUtils.EPSILON);	
	}
}
