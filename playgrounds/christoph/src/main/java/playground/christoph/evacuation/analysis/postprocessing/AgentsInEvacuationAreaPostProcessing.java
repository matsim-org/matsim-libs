/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsInEvacuationAreaPostProcessing.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.analysis.postprocessing;

import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.analysis.christoph.TravelTimesWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaActivityCounter;
import playground.christoph.evacuation.analysis.AgentsInEvacuationAreaCounter;
import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.config.EvacuationConfigReader;
import playground.christoph.evacuation.controler.PrepareEvacuationScenario;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataGrabber;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PanicModel;
import playground.christoph.evacuation.mobsim.decisionmodel.PickupModel;
import playground.christoph.evacuation.withinday.replanning.utils.SHPFileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class to produce AgentInEvacuationArea data using output data from a 
 * previous simulation run.
 * Expect three input arguments:
 * <ul>
 * 	<li>config file</li>
 * 	<li>evacuation config file</li>
 * </ul>
 * 
 * Note: due to changes in PersonDriverAgentImpl this code does not run at the moment!
 * 
 * @author cdobler
 */
public class AgentsInEvacuationAreaPostProcessing {

	public static void main(String[] args) throws IOException {
		if (args.length == 2) {
			new AgentsInEvacuationAreaPostProcessing(args[0], args[1]);
		}
	}
	
	public AgentsInEvacuationAreaPostProcessing(String configFile, String evacuationConfigFile) throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile);	
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager(); 
		
		new EvacuationConfigReader().readFile(evacuationConfigFile);
		EvacuationConfig.printConfig();
		
		/*
		 * Prepare the scenario:
		 * 	- connect facilities to network
		 * 	- add exit links to network
		 * 	- add pickup facilities
		 *  - add z Coordinates to network
		 */
		new PrepareEvacuationScenario().prepareScenario(scenario);
		
		/*
		 * Create two DummyControllers which provide OutputDirectoryHierarchy that point
		 * to the analyzed run's output directory.
		 * Since we do not want to overwrite existing results we add an additional prefix
		 * to the re-created outputs.
		 */
		DummyController dummyInputController = new DummyController(scenario);
		
		// add another string to the runId to not overwrite old files
		String runId = scenario.getConfig().controler().getRunId();
		scenario.getConfig().controler().setRunId(runId + ".postprocessed");
		DummyController dummyOutputController = new DummyController(scenario);
		
		List<ControlerListener> controlerListeners = new ArrayList<ControlerListener>();
		List<MobsimListener> mobsimListeners = new ArrayList<MobsimListener>();
		
		Set<SimpleFeature> features = new HashSet<SimpleFeature>();
		SHPFileUtil util = new SHPFileUtil();
		for (String file : EvacuationConfig.evacuationArea) {
			features.addAll(ShapeFileReader.getAllFeatures(file));		
		}
		Geometry affectedArea = util.mergeGeometries(features);
		
		CoordAnalyzer coordAnalyzer = new CoordAnalyzer(affectedArea);

		HouseholdsTracker householdsTracker = new HouseholdsTracker(scenario);
		householdsTracker.notifyBeforeMobsim(null);
		DecisionDataProvider decisionDataProvider = new DecisionDataProvider();
		
		/*
		 * Create a DecisionDataGrabber and run notifyMobsimInitialized(...)
		 * which inserts decision data into the DecisionDataProvider.
		 */
		DecisionDataGrabber decisionDataGrabber = new DecisionDataGrabber(scenario, coordAnalyzer, 
				householdsTracker, ((ScenarioImpl) scenario).getHouseholds().getHouseholdAttributes());	
		
		
		// read people in panic from file
		String panicFile = dummyInputController.getControlerIO().getIterationFilename(0, PanicModel.panicModelFile);
		PanicModel panicModel = new PanicModel(decisionDataProvider, EvacuationConfig.panicShare);
		panicModel.readDecisionsFromFile(panicFile);
		panicModel.printStatistics();
		
		// read pickup behavior from file
		String pickupFile = dummyInputController.getControlerIO().getIterationFilename(0, PickupModel.pickupModelFile);
		PickupModel pickupModel = new PickupModel(decisionDataProvider);
		pickupModel.readDecisionsFromFile(pickupFile);
		pickupModel.printStatistics();
		
		// read evacuation decisions from file
		String evacuationDecisionFile = dummyInputController.getControlerIO().getIterationFilename(0, EvacuationDecisionModel.evacuationDecisionModelFile);
		EvacuationDecisionModel evacuationDecisionModel = new EvacuationDecisionModel(scenario, MatsimRandom.getLocalInstance(), decisionDataProvider);
		evacuationDecisionModel.readDecisionsFromFile(evacuationDecisionFile);
		evacuationDecisionModel.printStatistics();
		
		// Create the set of analyzed modes.
		Set<String> transportModes = new HashSet<String>();
		transportModes.add(TransportMode.bike);
		transportModes.add(TransportMode.car);
		transportModes.add(TransportMode.pt);
		transportModes.add(TransportMode.ride);
		transportModes.add(TransportMode.walk);
		transportModes.add(PassengerQNetsimEngine.PASSENGER_TRANSPORT_MODE);
		
		/*
		 * Class to create dummy MobsimAfterSimStepEvents.
		 * Has to be added as first Listener to the EventsManager!
		 */
		AfterSimStepEventsCreator afterSimStepEventsCreator = new AfterSimStepEventsCreator(mobsimListeners);
		eventsManager.addHandler(afterSimStepEventsCreator);
		
		// Initialize AgentsInEvacuationAreaCounter
		double scaleFactor = 1 / config.qsim().getFlowCapFactor();
		AgentsInEvacuationAreaCounter agentsInEvacuationAreaCounter = new AgentsInEvacuationAreaCounter(scenario, transportModes, 
				coordAnalyzer.createInstance(), decisionDataProvider, scaleFactor);
		controlerListeners.add(agentsInEvacuationAreaCounter);
		mobsimListeners.add(agentsInEvacuationAreaCounter);
		eventsManager.addHandler(agentsInEvacuationAreaCounter);
		
		// Initialize AgentsInEvacuationAreaActivityCounter
		AgentsInEvacuationAreaActivityCounter agentsInEvacuationAreaActivityCounter = new AgentsInEvacuationAreaActivityCounter(scenario, 
				coordAnalyzer.createInstance(), decisionDataProvider, scaleFactor);
		controlerListeners.add(agentsInEvacuationAreaActivityCounter);
		mobsimListeners.add(agentsInEvacuationAreaActivityCounter);
		eventsManager.addHandler(agentsInEvacuationAreaActivityCounter);
		
		// MobsimInitializedListener
		MobsimInitializedEvent<Mobsim> mobsimInitializedEvent = new MobsimInitializedEvent<Mobsim>(null);
		for(MobsimListener mobsimListener : mobsimListeners) {
			if (mobsimListener instanceof MobsimInitializedListener) {
				((MobsimInitializedListener) mobsimListener).notifyMobsimInitialized(mobsimInitializedEvent);
			}
		}
		
		// TravelTimeCalculator
		config.travelTimeCalculator().setFilterModes(true);
		config.travelTimeCalculator().setAnalyzedModes(TransportMode.car);
		TravelTimeCalculator travelTime = TravelTimeCalculator.create(scenario.getNetwork(), config.travelTimeCalculator());
		eventsManager.addHandler(travelTime);
		
		String eventsFile = dummyInputController.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML);
		new EventsReaderXMLv1(eventsManager).parse(eventsFile);
		
		//IterationEndsListener
		IterationEndsEvent iterationEndsEvent = new IterationEndsEvent(dummyOutputController, 0);
		for(ControlerListener controlerListener : controlerListeners) {
			if (controlerListener instanceof IterationEndsListener) {
				((IterationEndsListener) controlerListener).notifyIterationEnds(iterationEndsEvent);
			}
		}
		
		/*
		 * Write car travel times
		 */
		TravelTimesWriter travelTimesWriter = new TravelTimesWriter();
//		travelTimesWriter.collectTravelTimes();
		
		String absoluteTravelTimesFile = dummyOutputController.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesAbsoluteFile);
		String relativeTravelTimesFile = dummyOutputController.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesRelativeFile);
		travelTimesWriter.writeAbsoluteTravelTimes(absoluteTravelTimesFile);
		travelTimesWriter.writeRelativeTravelTimes(relativeTravelTimesFile);

		String absoluteSHPTravelTimesFile = dummyOutputController.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesAbsoluteSHPFile);
		String relativeSHPTravelTimesFile = dummyOutputController.getControlerIO().getIterationFilename(0, TravelTimesWriter.travelTimesRelativeSHPFile);
		travelTimesWriter.writeAbsoluteSHPTravelTimes(absoluteSHPTravelTimesFile, MGC.getCRS(config.global().getCoordinateSystem()), true);
		travelTimesWriter.writeRelativeSHPTravelTimes(relativeSHPTravelTimesFile, MGC.getCRS(config.global().getCoordinateSystem()), true);

//		dummyInputController.shutdown(false);
//		dummyOutputController.shutdown(false);
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
				MobsimAfterSimStepEvent<Mobsim> e = new MobsimAfterSimStepEvent<Mobsim>(null, lastSimStep);
				
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
	
	/**
	 * Create a dummy controler that provides a ControlerIO object but that
	 * does not run.
	 * 
	 * @author cdobler
	 */
	private static class DummyController extends Controler {
		
		public DummyController(Scenario scenario) {
			super(scenario);
			
			String outputPath = this.getScenario().getConfig().controler().getOutputDirectory();
			if (outputPath.endsWith("/")) {
				outputPath = outputPath.substring(0, outputPath.length() - 1);
			}
			if (this.getScenario().getConfig().controler().getRunId() != null) {
//				this.controlerIO = new OutputDirectoryHierarchy(outputPath, this.scenarioData.getConfig().controler().getRunId(), true);
				this.setupOutputDirectory(outputPath, this.getScenario().getConfig().controler().getRunId(), true) ;
			} else {
//				this.controlerIO = new OutputDirectoryHierarchy(outputPath, true);
				this.setupOutputDirectory(outputPath, null, true) ;
			}
			// yy I don't think that this if confidition is necessary. kai, apr'13
			
		}
	}
}
