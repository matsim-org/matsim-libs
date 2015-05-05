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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.wagonSim.Utils;
import org.matsim.contrib.wagonSim.mobsim.qsim.WagonSimQSimFactory;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimAnalysisListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.pt.router.WagonSimRouterFactoryImpl;
import org.matsim.contrib.wagonSim.pt.router.WagonSimTripRouterFactoryImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import javax.inject.Provider;
import java.io.IOException;
import java.util.Map;

/**
 * @author balmermi
 *
 */
public final class RunWagonSim {
	private static final Logger log = Logger.getLogger( RunWagonSim.class ) ;
	
	public static final Controler init(Scenario scenario, final ObjectAttributes vehicleLinkSpeedAttributes, Map<Id<TransitStopFacility>, Double> minShuntingTimes) {
//		super(scenario);
		final Controler controler = new Controler( scenario ) ;
		Config config = controler.getConfig() ;
		
		final WagonSimVehicleLoadListener listener = new WagonSimVehicleLoadListener(scenario.getPopulation().getPersonAttributes());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new WagonSimQSimFactory(vehicleLinkSpeedAttributes, listener).createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		controler.addControlerListener(listener);
		Provider<TransitRouter> routerFactory =
				new WagonSimRouterFactoryImpl(
						listener, 
						scenario.getTransitSchedule(), new 
						TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental()), 
						scenario.getPopulation().getPersonAttributes(), 
						vehicleLinkSpeedAttributes);
		controler.setTripRouterFactory(new WagonSimTripRouterFactoryImpl(scenario, routerFactory, minShuntingTimes));
		
		controler.addControlerListener(new WagonSimAnalysisListener());
		
		return controler ;
	}
	
	//////////////////////////////////////////////////////////////////////
	// main
	//////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {

//		args = new String[] {
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/network.enriched.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitSchedule.enriched.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/shuntingTimes.enriched.txt",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitVehicles.enriched.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnriched/transitVehicleAttributes.enriched.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnrichedDemand/demand.wagons.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb/schedulePerformanceEnrichedDemand/wagonAttributes.xml.gz",
//				"D:/Users/balmermi/Documents/eclipse/output/sbb",
//				"run0000_OttPerfEnr"
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
		
		OutputDirectoryLogging.catchLogEntries();
		log.info("Main: "+RunWagonSim.class.getCanonicalName());
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
		Map<Id<TransitStopFacility>, Double> minShuntingTimes;
		try { minShuntingTimes = Utils.parseShuntingTimes(shuntingTimesFile); }
		catch (IOException e) { throw new RuntimeException(e); }
		
		Controler controler = init(scenario,vehicleLinkSpeedAttributes,minShuntingTimes);
		controler.run();
	}

}
