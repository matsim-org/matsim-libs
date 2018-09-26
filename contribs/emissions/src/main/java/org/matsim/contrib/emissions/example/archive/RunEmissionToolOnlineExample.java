/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
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
package org.matsim.contrib.emissions.example.archive;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.roadTypeMapping.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.roadTypeMapping.VisumHbefaRoadTypeMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;

/**
 * 
 * After creating a config file with 
 * {@link org.matsim.contrib.emissions.example.CreateEmissionConfig CreateEmissionConfig}
 * this class runs a simulation and calculates emissions online. 
 * Results are written into distinct xml-files including emission event files for some iterations (as specified by the config). 
 * <p></p>
 * See <a href="{@docRoot}/src-html/org/matsim/contrib/emissions/example/RunEmissionToolOnlineExample.html#line.39">here</a> for the listing.
 *
 * Archived: Nov'16
 *
 * @author benjamin, julia
 */

public class RunEmissionToolOnlineExample {

	private static final String configFile = "./test/input/org/matsim/contrib/emissions/config_v2.xml";
	
	public static Config prepareConfig( String[] args ) {

		// following is only for backward compatibility in which vehicle description is null;
		// for the new scenarios, setting vehicle description should be preferred.; Amit, sep 2016.
		EmissionsConfigGroup emissionsConfigGroup = new EmissionsConfigGroup();
		emissionsConfigGroup.setUsingVehicleTypeIdAsVehicleDescription(true);
		
		Config config ;

		if ( args==null || args.length==0 ) {
			config = ConfigUtils.loadConfig(configFile, emissionsConfigGroup);
		} else {
			config = ConfigUtils.loadConfig( args[0], emissionsConfigGroup);
		}
		
		return config ;
	}
	
	public static Scenario prepareScenario( Config config ) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		URL context = scenario.getConfig().getContext();

		//load emissions config
		EmissionsConfigGroup emissionsConfigGroup =  (EmissionsConfigGroup) config.getModules().get(EmissionsConfigGroup.GROUP_NAME);
		String mappingFile = emissionsConfigGroup.getEmissionRoadTypeMappingFileURL(context).getFile();

		//add Hbefa mappings to the network
		HbefaRoadTypeMapping vhtm = VisumHbefaRoadTypeMapping.createVisumRoadTypeMapping(mappingFile);
		vhtm.addHbefaMappings(scenario.getNetwork());
		return scenario ;
	}
	
	public static final void run( Scenario scenario, AbstractModule... overrides ) {
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(EmissionModule.class).asEagerSingleton();
			}
		});
		for ( AbstractModule module : overrides ) {
			controler.addOverridingModule(module);
		}
		controler.run();
	}
	public static void main(String[] args) {
		Config config = prepareConfig( args ) ;
		Scenario scenario = prepareScenario( config ) ;
		run(scenario) ;
	}

}
