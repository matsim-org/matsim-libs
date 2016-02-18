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
package playground.jbischoff.taxi.emissions;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.*;
import org.matsim.core.events.*;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * 
 * emissions calculation for taxis. Works without config file.
 */
public class TaxiOfflineEmissionTool {
	
	private final static String dir = "C:/Users/Joschka/Documents/shared-svn/projects/sustainability-w-michal-and-dlr/data/scenarios/2014_10_basic_scenario_v4/emissions/";
	private static final String networkFile = dir + "berlin_brb_t.xml";
	
	private static final String eventsFile = dir + "events.out.xml.gz";
	private static final String emissionEventOutputFile = dir + "emission.events.offline.xml.gz";
	
	// =======================================================================================================		
	
	public static void main (String[] args) throws Exception{

		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		ecg.setUsingDetailedEmissionCalculation(false);
		ecg.setAverageColdEmissionFactorsFile(dir+"hbefa/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile(dir+"hbefa/EFA_HOT_vehcat_2005average.txt");
		ecg.setDetailedWarmEmissionFactorsFile(dir+"hbefa/EFA_HOT_SubSegm_2005detailed.txt");
		ecg.setDetailedColdEmissionFactorsFile(dir+"hbefa/EFA_ColdStart_SubSegm_2005detailed.txt");
		ecg.setEmissionRoadTypeMappingFile(dir+"roadTypeMapping.txt");
		ecg.setEmissionVehicleFile(dir+"emissionVehicles.xml");
		
		Config config = ConfigUtils.createConfig(ecg);
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		
		EmissionModule emissionModule = new EmissionModule(scenario);
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(emissionModule.getWarmEmissionHandler());
		eventsManager.addHandler(emissionModule.getColdEmissionHandler());
		
		EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsFile);
		
		emissionEventWriter.closeFile();

		emissionModule.writeEmissionInformation(emissionEventOutputFile);
//		emissionModule.
	}


}