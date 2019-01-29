///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2015 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package org.matsim.contrib.minibus.ana;
//
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map.Entry;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.contrib.minibus.PConfigGroup;
//import org.matsim.contrib.minibus.fare.StageContainerCreator;
//import org.matsim.contrib.minibus.fare.TicketMachine;
//import org.matsim.contrib.minibus.operator.TimeProvider;
//import org.matsim.contrib.minibus.routeProvider.TimeAwareComplexCircleScheduleProviderHandler;
//import org.matsim.contrib.minibus.scoring.OperatorCostCollectorHandler;
//import org.matsim.contrib.minibus.scoring.ScorePlansHandler;
//import org.matsim.contrib.minibus.scoring.StageContainer2AgentMoneyEvent;
//import org.matsim.contrib.minibus.stats.CountPOperatorHandler;
//import org.matsim.contrib.minibus.stats.CountPPaxHandler;
//import org.matsim.contrib.minibus.stats.CountPVehHandler;
//import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.*;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.events.EventsUtils;
//import org.matsim.core.events.MatsimEventsReader;
//import org.matsim.core.events.handler.EventHandler;
//import org.matsim.core.gbl.Gbl;
//import org.matsim.core.network.MatsimNetworkReader;
//import org.matsim.core.scenario.ScenarioUtils;
//import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
//import org.matsim.vehicles.VehicleReaderV1;
//
//public class EventsHandlerSpeedTest {
//	
//	private static final Logger log = Logger.getLogger(EventsHandlerSpeedTest.class);
//	
//	public static void run(String eventsFile, List<EventHandler> eventHandlers){
//		
//		HashMap<EventHandler, Double> handler2time = new HashMap<EventHandler, Double>();
//		double currentCPUTimeNeeded = 0.0;
//		
//		double lastCPUTime = 0.0;
//		
//		for (EventHandler eventHandler : eventHandlers) {
//			eventHandler.reset(0);
//			Gbl.startMeasurement();
//			EventsManager eventsManager = EventsUtils.createEventsManager();
//			eventsManager.addHandler(eventHandler);
//			new MatsimEventsReader(eventsManager).readFile(eventsFile);
//			currentCPUTimeNeeded = Gbl.getThreadCpuTime(Thread.currentThread()) - lastCPUTime;
//			log.info(eventHandler.getClass().getSimpleName() + " needed " + currentCPUTimeNeeded + "s");
//			handler2time.put(eventHandler, currentCPUTimeNeeded);
//			lastCPUTime = Gbl.getThreadCpuTime(Thread.currentThread());
//		}
//		
//		for (Entry<EventHandler, Double> handler2timeEntry : handler2time.entrySet()) {
//			log.info(handler2timeEntry.getKey().getClass().getSimpleName() + " needed " + handler2timeEntry.getValue() + "s");
//		}
//	}
//	
//	public static void main(String[] args) {
//		String eventsFile = "e:/eventsTest/bvg6_b_0.1000.events.xml";
//		String configFile = "e:/eventsTest/config_bvg6_b_0.xml";
//		String outputFolder = "e:/eventsTest/output";
//		String networkFile = "e:/eventsTest/network.final.xml.gz";
//		String transitScheduleFile = "e:/eventsTest/bvg6_b_0.1000.transitSchedule.xml.gz";
//		String vehicleFile = "e:/eventsTest/bvg6_b_0.1000.vehicles.xml.gz";
//		
//		Config config = new Config();
//		config.addModule(new PConfigGroup());
//		ConfigUtils.loadConfig(config, configFile);
//		PConfigGroup pConfig = ConfigUtils.addOrGetModule(config, PConfigGroup.GROUP_NAME, PConfigGroup.class);
//		
//		Scenario scenario =  ScenarioUtils.createScenario(config);
//		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
//		networkReader.readFile(networkFile);
//		
//		VehicleReaderV1 vehicleReader = new VehicleReaderV1(scenario.getVehicles());
//		vehicleReader.readFile(vehicleFile);
//		TransitScheduleReaderV1 scheduleReader = new TransitScheduleReaderV1(scenario);
//		scheduleReader.readFile(transitScheduleFile);
//		
//				
//		
//		
//		List<EventHandler> eventHandlers = new LinkedList<EventHandler>();
//		
//		eventHandlers.add(new CountTripsPerMode());
//		eventHandlers.add(new CountVehPerMode());
//		eventHandlers.add(new CountVehicleMeterPerMode(scenario.getNetwork()));
//		eventHandlers.add(new AverageTripDistanceMeterPerMode(scenario.getNetwork()));
//		eventHandlers.add(new AverageInVehicleTripTravelTimeSecondsPerMode());
//		eventHandlers.add(new AverageWaitingTimeSecondsPerMode());
//		eventHandlers.add(new AverageNumberOfStopsPerMode());
//		eventHandlers.add(new CountTransfersPerModeModeCombination());
//		eventHandlers.add(new CountTripsPerPtModeCombination());
//		eventHandlers.add(new AverageLoadPerDeparturePerMode());
//		eventHandlers.add(new CountDeparturesWithNoCapacityLeftPerMode());
//		eventHandlers.add(new CountDeparturesPerMode());
//		
//		CountPassengerMeterPerMode countPassengerMeterPerMode = new CountPassengerMeterPerMode(scenario.getNetwork());
//		eventHandlers.add(countPassengerMeterPerMode);
//		CountCapacityMeterPerMode countCapacityMeterPerMode = new CountCapacityMeterPerMode(scenario.getNetwork());
//		eventHandlers.add(countCapacityMeterPerMode);
//		eventHandlers.add(new AverageLoadPerDistancePerMode(countPassengerMeterPerMode, countCapacityMeterPerMode));
//		
//		BVGLines2PtModes bvg = new BVGLines2PtModes();
//		bvg.setPtModesForEachLine(scenario.getTransitSchedule(), pConfig.getPIdentifier());
//		
//		for (EventHandler eventHandler : eventHandlers) {
//			PAnalysisModule module = (PAnalysisModule) eventHandler;
//			module.setLineId2ptModeMap(bvg.getLineId2ptModeMap());
//			module.updateVehicles(scenario.getVehicles());
//		}
//		
//		
//		
//		TimeProvider timeProvider = new TimeProvider(pConfig, outputFolder);
//		eventHandlers.add(timeProvider);
//		
//		StageContainerCreator sCCreator = new StageContainerCreator(pConfig.getPIdentifier());
//		sCCreator.init(scenario.getNetwork());
//		TicketMachine ticketMachine = new TicketMachine(pConfig.getEarningsPerBoardingPassenger(), pConfig.getEarningsPerKilometerAndPassenger() / 1000.0);
//		ScorePlansHandler scorePlansHandler = new ScorePlansHandler(ticketMachine);
//		sCCreator.addStageContainerHandler(scorePlansHandler);
//		eventHandlers.add(sCCreator);
//		
//		OperatorCostCollectorHandler oCCCollector = new OperatorCostCollectorHandler(pConfig.getPIdentifier(), pConfig.getCostPerVehicleAndDay(), pConfig.getCostPerKilometer(), pConfig.getCostPerHour());
//		oCCCollector.init(scenario.getNetwork());
//		oCCCollector.addOperatorCostContainerHandler(scorePlansHandler);
//		eventHandlers.add(oCCCollector);
//		
//		TimeAwareComplexCircleScheduleProviderHandler scheduleProvider = new TimeAwareComplexCircleScheduleProviderHandler(pConfig.getPIdentifier());
//		eventHandlers.add(scheduleProvider);
//		
//		
//        eventHandlers.add(new CountPPaxHandler(pConfig.getPIdentifier())); 
//        eventHandlers.add(new CountPVehHandler(pConfig.getPIdentifier()));
//        eventHandlers.add(new CountPOperatorHandler(pConfig.getPIdentifier()));
//		
//		EventsHandlerSpeedTest.run(eventsFile, eventHandlers);
//		
//		
//	}
//
//}
