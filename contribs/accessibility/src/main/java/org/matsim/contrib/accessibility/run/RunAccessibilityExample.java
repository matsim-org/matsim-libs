/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility.run;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author nagel
 *
 */
final public class RunAccessibilityExample {
	// do not change name of class; matsim book refers to it.  kai, dec'14

	private static final Logger log = Logger.getLogger(RunAccessibilityExample.class);

	
	public static void main(String[] args) {

		if ( args.length==0 || args.length>1 ) {
			throw new RuntimeException("usage: ... config.xml") ;
		}
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		AccessibilityConfigGroup accConfig = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class ) ;
		accConfig.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// ---
		
		run( scenario);
		// (the run method is extracted so that a test can operate on it)
	}

	
	public static void run(final Scenario scenario) {
		
		List<String> activityTypes = AccessibilityUtils.collectAllFacilityOptionTypes(scenario) ;
		log.warn( "found the following activity types: " + activityTypes );
		
		Controler controler = new Controler(scenario);

		for (final String actType : activityTypes) { // add an overriding module per activity type:
			final AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(actType);
			controler.addOverridingModule(module);
		}

		controler.run();
		
	}
}
