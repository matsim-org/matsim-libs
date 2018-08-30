/* *********************************************************************** *
 * project: org.matsim.*
 * PtControler.java
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

package org.matsim.codeexamples.extensions.otfvis;

import java.net.MalformedURLException;
import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunTransitWithOtfvisExample {

	public static void main(final String[] args) {
		
		Config config = null ;
		if ( args != null && args.length >= 1 ) {
			config = ConfigUtils.loadConfig(args[0], new OTFVisConfigGroup() ) ;
		} else {
			final String filename = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/cottbus/cottbus-tutorial-2016/config01.xml";
			try {
				URL url = new URL(filename) ;
				config = ConfigUtils.loadConfig(url, new OTFVisConfigGroup() ) ;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;

		config.qsim().setSnapshotStyle( SnapshotStyle.queue ) ;
		config.qsim().setVehicleBehavior( QSimConfigGroup.VehicleBehavior.teleport ) ;

		config.transit().setUseTransit(true);
			
		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class ) ;
		visConfig.setDrawTime(true);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setAgentSize(125);
		visConfig.setLinkWidth(10);
		visConfig.setDrawTransitFacilityIds(false);
		visConfig.setDrawTransitFacilities(false);
		
		if ( args.length > 1 && args[1]!=null ) {
			ConfigUtils.loadConfig(config, args[1]);
			// (this loads a second config file, if you want to insist on overriding the settings so far but don't want to touch the code.  kai, aug'16)
		}
			
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler controler = new Controler(scenario) ;

		controler.addOverridingModule( new OTFVisLiveModule() );

		controler.run();
	}

}
