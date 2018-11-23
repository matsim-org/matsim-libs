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
package org.matsim.contrib.emissions.example;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * 
 * Use the config file as created by the 
 * {@link CreateEmissionConfig CreateEmissionConfig} to calculate
 * emissions based on the link leave events of an events file. Resulting emission events are written into an event file.
 *
 * @author benjamin, julia
 */
public class RunDetailedEmissionToolOfflineExample{
	
	private final static String runDirectory = "./test/output/";
	private static final String configFile = "./test/input/org/matsim/contrib/emissions/config_detailed.xml";
	
	private static final String eventsFile =  "./test/input/org/matsim/contrib/emissions/5.events.xml.gz";
	// (remove dependency of one test/execution path from other. kai/ihab, nov'18)

	private static final String emissionEventOutputFile = runDirectory + "5.emission.events.offline.xml.gz";
	private Config config;

	// =======================================================================================================		
	
	public static void main (String[] args) throws Exception{
        RunDetailedEmissionToolOfflineExample emissionToolOfflineExampleV2 = new RunDetailedEmissionToolOfflineExample();
        emissionToolOfflineExampleV2.run();
	}

	public Config prepareConfig() {
		config = ConfigUtils.loadConfig(configFile, new EmissionsConfigGroup());
		return config;
	}

    public void run() {
		if ( config==null ) {
			this.prepareConfig() ;
		}
        Scenario scenario = ScenarioUtils.loadScenario(config);
        EventsManager eventsManager = EventsUtils.createEventsManager();

		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};;

		com.google.inject.Injector injector = Injector.createInjector(config, module );

        EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);

        emissionEventWriter.closeFile();

    }
}
