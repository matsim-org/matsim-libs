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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityContributionCalculator;
import org.matsim.contrib.accessibility.GridBasedAccessibilityModule;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

import com.google.inject.multibindings.MapBinder;

/**
 * @author nagel
 *
 */
final public class RunAccessibilityExample {
	// do not change name of class; matsim book refers to it.  kai, dec'14

	private static final Logger log = Logger.getLogger(RunAccessibilityExample.class);

	
	public static void main(String[] args) {

		if ( args.length==0 || args.length>1 ) {
			throw new RuntimeException("useage: ... config.xml") ;
		}
		Config config = ConfigUtils.loadConfig( args[0] ) ;
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// the run method is extracted so that a test can operate on it.
		run( scenario);
	}

	
	public static void run(final Scenario scenario) {
		
		final List<String> activityTypes = new ArrayList<>() ;
		final ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;

		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) { // go through all facilities ...
			for ( ActivityOption option : fac.getActivityOptions().values() ) { // go through all activity options at each facility ...
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
				// figure out where the homes are
				if ( option.getType().equals("h") ) { // yyyyyy hardcoded home activity option; replace!!!!
					homes.addActivityFacility(fac);
				}
			}
		}
		
		log.warn( "found the following activity types: " + activityTypes );
		AbstractModule[] overridingModule = {}; 
		
		scenario.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
				
				Controler controler = new Controler(scenario);
		
				for ( int ii=0 ; ii< overridingModule.length ; ii++ ) {
					controler.addOverridingModule( overridingModule[ii] );
				}
			
				final GridBasedAccessibilityModule mm = new GridBasedAccessibilityModule();
		//		mm.addSpatialGridDataExchangeInterface( null ) ;
				mm.addAdditionalFacilityData(homes) ;
				controler.addOverridingModule(mm);
			
				// Add calculators
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						MapBinder<String,AccessibilityContributionCalculator> accBinder = MapBinder.newMapBinder(this.binder(), String.class, AccessibilityContributionCalculator.class);
						AccessibilityUtils.addFreeSpeedNetworkMode(this.binder(), accBinder, TransportMode.car);
						AccessibilityUtils.addNetworkMode(this.binder(), accBinder, TransportMode.car);
						AccessibilityUtils.addConstantSpeedMode(this.binder(), accBinder, TransportMode.bike);
					}
			
				});
				controler.run();
		
//		
//		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some other algorithms,
//		// the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
//		
//		final Controler controler = new Controler(scenario) ;
//		controler.getConfig().controler().setOverwriteFileSetting(
//				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );
//
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				for (final String actType : activityTypes) {
//
//					final ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities() ;
//					for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
//						for ( ActivityOption option : fac.getActivityOptions().values() ) {
//							if ( option.getType().equals(actType) ) {
//								opportunities.addActivityFacility(fac);
//							}
//						}
//					}
//					
//					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {
//						@Override public ControlerListener get() {
//							Double cellSizeForCellBasedAccessibility = Double.parseDouble(scenario.getConfig().getModule("accessibility").getValue("cellSizeForCellBasedAccessibility"));
//							Config config = scenario.getConfig();
//							if (cellSizeForCellBasedAccessibility <= 0) {
//								throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
//							}
//							BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
//							ActivityFacilitiesImpl measuringPoints = GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSizeForCellBasedAccessibility) ;
//							AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(scenario, measuringPoints);
//
//							GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, opportunities, null, scenario, bb.getXMin(), bb.getYMin(), bb.getXMax(),bb.getYMax(), cellSizeForCellBasedAccessibility);
//
//							if ( true ) {
//								throw new RuntimeException("The following needs to be replaced with the newer, more modern syntax.  kai, nov'16" ) ;
//							}
//							// define the modes that will be considered
//							// here, the accessibility computation is only done for freespeed
////							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
//
//							// add additional facility data to an additional column in the output
//							// here, an additional population density column is used
//							listener.addAdditionalFacilityData(homes) ;
//							listener.writeToSubdirectoryWithName(actType);
//							return listener;
//						}
//					});
//				}
//			}
//		});
//
//
//		controler.run();
	}
}