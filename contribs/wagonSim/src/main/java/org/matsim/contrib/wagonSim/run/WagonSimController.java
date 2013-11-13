/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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
package org.matsim.contrib.wagonSim.run;

import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.mobsim.qsim.WagonSimQSimFactory;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimAnalysisListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.pt.router.WagonSimRouterFactoryImpl;
import org.matsim.contrib.wagonSim.pt.router.WagonSimTripRouterFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * @author balmermi
 *
 */
public class WagonSimController extends Controler {

	//////////////////////////////////////////////////////////////////////
	// vars
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public WagonSimController(Scenario scenario,ObjectAttributes vehicleLinkSpeedAttributes, Map<Id,Double> minShuntingTimes) {
		super(scenario);
		
		super.setOverwriteFiles(true);
		WagonSimVehicleLoadListener listener = new WagonSimVehicleLoadListener(scenario.getPopulation().getPersonAttributes());
		this.setMobsimFactory(new WagonSimQSimFactory(vehicleLinkSpeedAttributes, listener));
		this.addControlerListener(listener);
		TransitRouterFactory routerFactory = 
				new WagonSimRouterFactoryImpl(
						listener, 
						scenario.getTransitSchedule(), new 
						TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental()), 
						scenario.getPopulation().getPersonAttributes(), 
						vehicleLinkSpeedAttributes);
		this.setTripRouterFactory(new WagonSimTripRouterFactoryImpl(scenario, routerFactory, minShuntingTimes));
		
		this.addControlerListener(new WagonSimAnalysisListener());
	}
	
//	//////////////////////////////////////////////////////////////////////
//	// inner classes
//	//////////////////////////////////////////////////////////////////////
//
//	// almost copy/paste from QSimFactory
//	static class MyQSimFactory implements MobsimFactory {
//		
//		//////////////////////////////////////////////////////////////////////
//
//		private final LocomotiveLinkSpeedCalculator linkSpeedCalculator;
//		
//		//////////////////////////////////////////////////////////////////////
//
//		public MyQSimFactory(ObjectAttributes vehicleLinkSpeedAttributes) {
//			this.linkSpeedCalculator  = new LocomotiveLinkSpeedCalculator(vehicleLinkSpeedAttributes);
//		}
//		
//		//////////////////////////////////////////////////////////////////////
//
//		@Override
//		public Netsim createMobsim(Scenario sc, EventsManager eventsManager) {
//			QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
//			if (conf == null) {
//				throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
//			}
//
//			// Get number of parallel Threads
//			int numOfThreads = conf.getNumberOfThreads();
//			QNetsimEngineFactory netsimEngFactory;
//			if (numOfThreads > 1) {
//				/*
//				 * The SimStepParallelEventsManagerImpl can handle events from multiple threads.
//				 * The (Parallel)EventsMangerImpl cannot, therefore it has to be wrapped into a
//				 * SynchronizedEventsManagerImpl.
//				 */
//				if (!(eventsManager instanceof SimStepParallelEventsManagerImpl)) {
//					eventsManager = new SynchronizedEventsManagerImpl(eventsManager);				
//				}
//				netsimEngFactory = new ParallelQNetsimEngineFactory();
//				log.info("Using parallel QSim with " + numOfThreads + " threads.");
//			} else {
//				netsimEngFactory = new DefaultQSimEngineFactory();
//			}
//			QSim qSim = new QSim(sc, eventsManager);
//			ActivityEngine activityEngine = new ActivityEngine();
//			qSim.addMobsimEngine(activityEngine);
//			qSim.addActivityHandler(activityEngine);
//			QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
//			qSim.addMobsimEngine(netsimEngine);
//			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
//			// setting the linkSpeedCalculator
//			netsimEngine.setLinkSpeedCalculator(linkSpeedCalculator);
//			TeleportationEngine teleportationEngine = new TeleportationEngine();
//			qSim.addMobsimEngine(teleportationEngine);
//
//			AgentFactory agentFactory;
//			if (sc.getConfig().scenario().isUseTransit()) {
//				agentFactory = new TransitAgentFactory(qSim);
//				TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
//				transitEngine.setUseUmlaeufe(true);
//				transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
//				qSim.addDepartureHandler(transitEngine);
//				qSim.addAgentSource(transitEngine);
//				qSim.addMobsimEngine(transitEngine);
//			} else {
//				agentFactory = new DefaultAgentFactory(qSim);
//			}
//			if (sc.getConfig().network().isTimeVariantNetwork()) {
//				qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
//			}
//			PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
//			qSim.addAgentSource(agentSource);
//			return qSim;
//		}
//	}
//	
//	//////////////////////////////////////////////////////////////////////
//
//	static class LocomotiveLinkSpeedCalculator implements LinkSpeedCalculator {
//
//		//////////////////////////////////////////////////////////////////////
//
//		private final ObjectAttributes vehicleLinkSpeedAttributes;
//		
//		//////////////////////////////////////////////////////////////////////
//
//		LocomotiveLinkSpeedCalculator(final ObjectAttributes vehicleLinkSpeedAttributes) {
//			this.vehicleLinkSpeedAttributes = vehicleLinkSpeedAttributes;
//		}
//
//		//////////////////////////////////////////////////////////////////////
//
//		@Override
//		public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
//			Object obj = (Double)vehicleLinkSpeedAttributes.getAttribute(vehicle.getId().toString(),link.getId().toString());
//			if (obj == null) { throw new RuntimeException("time="+time+",vId="+vehicle.getId()+",lId="+link.getId()+": no speed defined in vehicleLinkSpeedAttributes. Bailing out."); }
//			return (Double)obj;
//		}
//	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {

		args = new String[] {
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/network.enriched.xml.gz",
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitSchedule.enriched.xml.gz",
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/shuntingTimes.enriched.txt",
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitVehicles.enriched.xml.gz",
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitVehicleAttributes.enriched.xml.gz",
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnrichedDemand/demand.wagons.xml.gz",
				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnrichedDemand/wagonAttributes.xml.gz",
				"D:/Users/balmermi/Documents/eclipse/output/sbb",
				"run0000_OttPerfEnr"
		};

//		args = new String[] {
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/network.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitSchedule.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitVehicles.ott.performance.xml",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitVehicleLinkSpeedAttributes.performance.ott.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/demand/demand.wagons.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/demand/wagonAttributes.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/runs",
//				"run0000_OttPerf"
//		};

//		args = new String[] {
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/network.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitSchedule.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitVehicles.ott.performance.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformance/transitVehicleLinkSpeedAttributes.performance.ott.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/input/population.empty.xml",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/demand/wagonAttributes.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/runs",
//				"runOttPerfEmptyPlans"
//		};

//		args = new String[] {
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\network.ott.performance.xml.gz",
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\transitSchedule.ott.performance.xml.gz",
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\transitVehicles.ott.performance.xml.gz",
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\transitVehicleLinkSpeedAttributes.performance.ott.xml.gz",
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\demand.wagons.xml.gz",
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\wagonAttributes.xml.gz",
//				"C:\\Users\\Daniel\\Desktop\\wagonSim\\runs\\",
//				"runOttPerfWithPlans"
//		};

		if (args.length != 9) {
			log.error(OttToMATSimScheduleConverterMain.class.getCanonicalName()+" networkFile scheduleFile shuntingTimesFile vehicleFile vehicleAttributeFile plansFile wagonAttributesFile outputBase runId");
			System.exit(-1);
		}

		String networkFile = args[0];
		String scheduleFile = args[1];
		String shuntingTimesFile = args[2];
		String vehicleFile = args[3];
		String vehicleAttributeFile = args[4];
		String plansFile = args[5];
		String wagonAttributeFile = args[6];
		String outputBase = args[7];
		String runId = args[8];
		// creates a new runId, depending on the number of runs performed before
//		File f = new File(outputBase);
//		runId = runId + (f.listFiles().length + 1);
		
		
		// catch log-entries, otherwise all messages written before the controler is set up are lost...
		OutputDirectoryLogging.catchLogEntries();
//		log.warn("########################### check config-settings ###########################");
		log.info("Main: "+WagonSimController.class.getCanonicalName());
		log.info("networkFile: "+networkFile);
		log.info("scheduleFile: "+scheduleFile);
		log.info("shuntingTimesFile: "+shuntingTimesFile);
		log.info("vehicleFile: "+vehicleFile);
		log.info("vehicleAttributeFile: "+vehicleAttributeFile);
		log.info("plansFile: "+plansFile);
		log.info("wagonAttributeFile: "+wagonAttributeFile);
		log.info("outputBase: "+outputBase);
		log.info("runId: "+runId);

		Config config = Utils.getDefaultWagonSimConfig();
		config.network().setInputFile(networkFile);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehicleFile);
		config.plans().setInputFile(plansFile);
		config.plans().setInputPersonAttributeFile(wagonAttributeFile);
		config.controler().setOutputDirectory(outputBase+"/"+runId);
		config.controler().setRunId(runId);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		ObjectAttributes vehicleLinkSpeedAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(vehicleLinkSpeedAttributes).parse(vehicleAttributeFile);
		Map<Id, Double> minShuntingTimes;
		try { minShuntingTimes = Utils.parseShuntingTimes(shuntingTimesFile); }
		catch (IOException e) { throw new RuntimeException(e); }
		
		WagonSimController controller = new WagonSimController(scenario,vehicleLinkSpeedAttributes,minShuntingTimes);
		controller.run();
	}

}
