/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.benjamin.scenarios.manteuffel;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;


/**
 *
 * @author benjamin
 */
public class ManteuffelEmissionToolOffline{
	private static final Logger logger = Logger.getLogger(ManteuffelEmissionToolOffline.class);
	
	private static final String inputPath = "../../runs-svn/manteuffelstrasse/";
	private static final String emissionVehicleFile = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionVehicles.xml.gz";
	private static final String roadTypeMappingFile = inputPath + "hbefaForMatsim/roadTypeMapping.txt";
	private static final String averageFleetWarmEmissionFactorsFile = inputPath + "hbefaForMatsim/EFA_HOT_vehcat_2005average.txt";
	private static final String averageFleetColdEmissionFactorsFile = inputPath + "hbefaForMatsim/EFA_ColdStart_vehcat_2005average.txt";
	
	private static final String eventsFile = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.events.xml.gz";
	private static final String emissionEventsOutputFile = inputPath + "bau/ITERS/it.30/bvg.run190.25pct.dilution001.network20150727.v2.static.30.emissionEvents.xml.gz";
	private static final String netFile = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionNetwork.xml.gz";
	
//	private static final String eventsFile = inputPath + "p1/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.events.xml.gz";
//	private static final String emissionEventsOutputFile = inputPath + "p1/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.I.30.emissionEvents.xml.gz";
//	private static final String netFile = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionNetwork.xml.gz";
	
//	private static final String eventsFile = inputPath + "p2/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.events.xml.gz";
//	private static final String emissionEventsOutputFile = inputPath + "p2/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.II.30.emissionEvents.xml.gz";
//	private static final String netFile = inputPath + "bau/bvg.run190.25pct.dilution001.network20150727.v2.static.emissionNetwork.xml.gz";
	
//	private static final String eventsFile = inputPath + "p3/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.events.xml.gz";
//	private static final String emissionEventsOutputFile = inputPath + "p3/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.III.30.emissionEvents.xml.gz";
//	private static final String netFile = inputPath + "p3/bvg.run190.25pct.dilution001.network.20150731.LP2.III.emissionNetwork.xml.gz";
	
//	private static final String eventsFile = inputPath + "p4/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.events.xml.gz";
//	private static final String emissionEventsOutputFile = inputPath + "p4/ITERS/it.30/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.30.emissionEvents.xml.gz";
//	private static final String netFile = inputPath + "p4/bvg.run190.25pct.dilution001.network.20150731.LP2.IV.emissionNetwork.xml.gz";
	
	// =======================================================================================================		
	
	public static void main (String[] args) throws Exception{
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
		config.addModule(ecg);
		ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		ecg.setEmissionVehicleFile(emissionVehicleFile);
		ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
        
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		
		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventsOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		emissionEventWriter.closeFile();

		emissionModule.writeEmissionInformation(emissionEventsOutputFile);
	}
}