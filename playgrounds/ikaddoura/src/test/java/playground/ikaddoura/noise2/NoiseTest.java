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
package playground.ikaddoura.noise2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 *
 */

public class NoiseTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	// Tests the NoisSpatialInfo functionality separately for each function
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config1.xml";

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setReceiverPointGap(250.);
		noiseParameters.setScaleFactor(1.);
		String[] consideredActivities = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamages(consideredActivities);
		
		NoiseContext noiseContext = new NoiseContext(scenario, noiseParameters);
		
		noiseContext.initialize();
		
		// test the grid of receiver points
		Assert.assertEquals("wrong number of receiver points", 16, noiseContext.getReceiverPoints().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong coord for receiver point Id '10'", new CoordImpl(500, 100).toString(), noiseContext.getReceiverPoints().get(Id.create(10, ReceiverPoint.class)).getCoord().toString());
		
//		// test the allocation of receiver point to grid cell
//		Assert.assertEquals("wrong number of grid cells for which receiver points are stored", 9, noiseContext.getZoneTuple2listOfReceiverPointIds().size(), MatsimTestUtils.EPSILON);
				
		// test the allocation of activity coordinates to the nearest receiver point
		Assert.assertEquals("wrong nearest receiver point Id for coord 300/300 (x/y)", "5", noiseContext.getActivityCoord2receiverPointId().get(new CoordImpl(300, 300)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 150/150 (x/y)", "9", noiseContext.getActivityCoord2receiverPointId().get(new CoordImpl(150, 150)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 100/100 (x/y)", "8", noiseContext.getActivityCoord2receiverPointId().get(new CoordImpl(100, 100)).toString());
		Assert.assertEquals("wrong nearest receiver point Id for coord 500/500 (x/y)", "2", noiseContext.getActivityCoord2receiverPointId().get(new CoordImpl(500, 500)).toString());
					
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
	
	// tests the noise emissions, immissions and exposures
	@Test
	public final void test2(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseTest/config2.xml";

		Controler controler = new Controler(configFile);
		
		controler.setOverwriteFiles(true);
		controler.run();
		
		// +++++++++++++++++++++++++++++++++++++++++++++++++++
		
		String runDirectory = controler.getConfig().controler().getOutputDirectory() + "/";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(runDirectory + "output_network.xml.gz");
		config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setLastIteration(controler.getConfig().controler().getLastIteration());
		
		NoiseParameters noiseParameters = new NoiseParameters();
		noiseParameters.setReceiverPointGap(250.);
		noiseParameters.setScaleFactor(1.);
		
		String[] consideredActivities = {"home", "work"};
		noiseParameters.setConsideredActivitiesForDamages(consideredActivities);
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
		
		String outputFilePath = runDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		File file = new File(outputFilePath);
		file.mkdirs();
		
		EventsManager events = EventsUtils.createEventsManager();
		
		EventWriterXML eventWriter = new EventWriterXML(outputFilePath + config.controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
		events.addHandler(eventWriter);
			
		NoiseContext noiseContext = new NoiseContext(scenario, noiseParameters);
		noiseContext.initialize();
		NoiseWriter.writeReceiverPoints(noiseContext, outputFilePath + "/receiverPoints/");
		
		PersonActivityTracker actTracker = new PersonActivityTracker(noiseContext);
		events.addHandler(actTracker);
		
		NoiseTimeTracker timeTracker = new NoiseTimeTracker(noiseContext, events, outputFilePath);
		events.addHandler(timeTracker);
				
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
		
		timeTracker.computeFinalTimeIntervals();

		eventWriter.closeFile();
		
		// +++++++++++++++++++++++++++++++++++++++++++++++++++
		
		// test considered agent units
		
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
			
			pathToConsideredAgentUnitsFile = runDirectory + "analysis_it.0/consideredAgentUnits/0.consideredAgentUnits_" + Double.toString(currentTimeSlot) + ".csv";
			
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
			
			for(Id<Person> personId : noiseContext.getScenario().getPopulation().getPersons().keySet()){
				
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
						
						//test code of getDurationInWithinInterval from actInfo
						
						double durationInThisInterval = 0.;
						double timeIntervalStart = currentTimeSlot - noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
						
						if (( actInfo.getStartTime() < currentTimeSlot) && ( actInfo.getEndTime() >=  timeIntervalStart )) {
							
							if ((actInfo.getStartTime() <= timeIntervalStart) && actInfo.getEndTime() >= currentTimeSlot) {
								
								durationInThisInterval = noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation();
							
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
						
						double durationInThisIntervalMethod = actInfo.getDurationWithinInterval(currentTimeSlot, noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation()); 
							
						Assert.assertEquals("Durations of activities do not match!", durationInThisIntervalMethod, durationInThisInterval, MatsimTestUtils.EPSILON);
							
						double unitsThisPersonActivityInfo = durationInThisInterval / noiseContext.getNoiseParams().getTimeBinSizeNoiseComputation(); 
						affectedPersons = ( unitsThisPersonActivityInfo * noiseContext.getNoiseParams().getScaleFactor() );
						
						Coord coord = actInfo.getActivityType().equals("home") ?
								((Activity)noiseContext.getScenario().getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(0)).getCoord() :
								((Activity)noiseContext.getScenario().getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements().get(2)).getCoord();
							
						Id<ReceiverPoint> rpId = noiseContext.getActivityCoord2receiverPointId().get(coord);
						
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
				
				Assert.assertEquals("Wrong number of affected persons at receiver point 16", 2.35305555555555, affectedPersonsPerReceiverPointTest.get(Id.create("16", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
				Assert.assertEquals("Wrong number of affected persons at receiver point 0", 0.479722222222222, affectedPersonsPerReceiverPointTest.get(Id.create("0", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
				
			}
			
			for(Id<ReceiverPoint> receiverPointId : affectedPersonsPerReceiverPointTest.keySet()){
				
				Assert.assertEquals("Wrong number of affected persons", affectedPersonsPerReceiverPointTest.get(receiverPointId), consideredAgentsPerReceiverPoint.get(receiverPointId).get(index), MatsimTestUtils.EPSILON);
				
			}
			
			index++;
			
		}
		
		// +++++++++++++++++++++++++++++++++++++++++++++++++++
		// test emissions per link and time
		
		line = null;
		
		String pathToEmissionsFile = runDirectory + "analysis_it.0/emissions/0.emission_" + Double.toString(endTime) + ".csv";
		
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
		
		for(NoiseEventCaused event: timeTracker.getNoiseEventsCaused()){
			
			if(event.getTime() >= endTime - 3600 && event.getTime() <= endTime){
				
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
		
		for(Id<Link> linkId : noiseContext.getScenario().getNetwork().getLinks().keySet()){
			noiseEmissionsPerLink.put(linkId, 0.);
		}
		
		for(Id<Link> linkId : amountOfVehiclesPerLink.keySet()){

			double vCar = (noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed()) * 3.6;
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
		
		
		// +++++++++++++++++++++++++++++++++++++++++++++++++++
		// test immissions and damages per receiver point and time
		
		line = null;
		
		String pathToImmissionsFile = runDirectory + "analysis_it.0/immissions/0.immission_" + Double.toString(endTime) + ".csv";
		
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
		
		for(ReceiverPoint rp : noiseContext.getReceiverPoints().values()){
			
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
		
		line = null;
		
		String pathToDamagesFile = runDirectory + "analysis_it.0/damages/0.damages_" + Double.toString(endTime) + ".csv";
		
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
		
		for(ReceiverPoint rp : noiseContext.getReceiverPoints().values()){
			
			double noiseImmission = immissionPerReceiverPointId.get(rp.getId());
			double affectedAgentUnits = consideredAgentsPerReceiverPoint.get(rp.getId()).get(1);
			
			Assert.assertEquals("Wrong damage!", NoiseEquations.calculateDamageCosts(noiseImmission, affectedAgentUnits, endTime, noiseParameters.getAnnualCostRate(), noiseParameters.getTimeBinSizeNoiseComputation()), damagesPerReceiverPointId.get(rp.getId()),  MatsimTestUtils.EPSILON);
			
		}
		
		Assert.assertEquals("Wrong damage!", 0.0664164095284536, damagesPerReceiverPointId.get(Id.create("16", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong damage!", 0., damagesPerReceiverPointId.get(Id.create("0", ReceiverPoint.class)), MatsimTestUtils.EPSILON);
		
		// +++++++++++++++++++++++++++++++++++++++++++++++++++

		boolean tested = false;
		int counter = 0;
		for (NoiseEventCaused event : timeTracker.getNoiseEventsCaused()) {
			tested = true;

			if (event.getTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test1", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.0328089315079348, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else if (event.getTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("linkA5", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test2", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.0328089315079348, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else if (event.getTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("link2", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test1", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 3.992732562920194E-4, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else if (event.getTime() == 11 * 3600. && event.getLinkId().toString().equals(Id.create("link2", Link.class).toString()) && event.getCausingVehicleId().toString().equals((Id.create("person_car_test2", Vehicle.class).toString()))) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 3.992732562920194E-4, event.getAmount(), MatsimTestUtils.EPSILON);
				counter++;
			} else {
				Assert.assertEquals("There should either be no further events, or the amount should be zero.", 0., event.getAmount(), MatsimTestUtils.EPSILON);
			}
		}		
		Assert.assertTrue("No event found to be tested.", tested);
		Assert.assertEquals("Wrong number of total events.", 4, counter, MatsimTestUtils.EPSILON);
		
		boolean tested2 = false;
		int counter2 = 0;
		for (NoiseEventAffected event : timeTracker.getNoiseEventsAffected()) {
			tested2 = true;

			if (event.getTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test1", Person.class).toString())) && event.getActType().equals("work") ) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.020745817449213576, event.getAmount(), MatsimTestUtils.EPSILON);
				counter2++;
			} else if (event.getTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test2", Person.class).toString())) && event.getActType().equals("work")) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.017444990107520864, event.getAmount(), MatsimTestUtils.EPSILON);
				counter2++;
			} else if (event.getTime() == 11 * 3600. && event.getrReceiverPointId().toString().equals(Id.create("16", ReceiverPoint.class).toString()) && event.getAffectedAgentId().toString().equals((Id.create("person_car_test3", Person.class).toString())) && event.getActType().equals("home")) {
				Assert.assertEquals("wrong cost per car for the given link and time interval", 0.028225601971719153, event.getAmount(), MatsimTestUtils.EPSILON);
				counter2++;
			} else {
				Assert.assertEquals("There should either be no further events, or the amount should be zero.", 0., event.getAmount(), MatsimTestUtils.EPSILON);
			}
			
		}		
		Assert.assertTrue("No event found to be tested.", tested2);
		Assert.assertEquals("Wrong number of total events.", 3, counter2, MatsimTestUtils.EPSILON);
		
	 }
	
}
