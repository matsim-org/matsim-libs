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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.AccessibilityCalculator;
import org.matsim.contrib.accessibility.GridBasedAccessibilityShutdownListenerV3;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.GridUtils;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.swing.event.CellEditorListener;

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
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		// the run method is extracted so that a test can operate on it.
		run( scenario);
	}

	
	public static void run(final Scenario scenario) {
		
		final List<String> activityTypes = new ArrayList<String>() ;
		final ActivityFacilities homes = FacilitiesUtils.createActivityFacilities("homes") ;
		for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
			for ( ActivityOption option : fac.getActivityOptions().values() ) {
				// figure out all activity types
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
				// figure out where the homes are
				if ( option.getType().equals("h") ) {
					homes.addActivityFacility(fac);
				}
			}
		}
		
		log.warn( "found the following activity types: " + activityTypes ); 
		
		// yyyy there is some problem with activity types: in some algorithms, only the first letter is interpreted, in some other algorithms,
		// the whole string.  BEWARE!  This is not good software design and should be changed.  kai, feb'14
		
		final Controler controler = new Controler(scenario) ;
		controler.getConfig().controler().setOverwriteFileSetting(
				OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				for ( final String actType : activityTypes ) {

//			if ( !actType.equals("w") ) {
//				log.error("skipping everything except work for debugging purposes; remove in production code. kai, feb'14") ;
//				continue ;
//			}

					final ActivityFacilities opportunities = FacilitiesUtils.createActivityFacilities() ;
					for ( ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()  ) {
						for ( ActivityOption option : fac.getActivityOptions().values() ) {
							if ( option.getType().equals(actType) ) {
								opportunities.addActivityFacility(fac);
							}
						}
					}
					addControlerListenerBinding().toProvider(new Provider<ControlerListener>() {

						@Inject Map<String, TravelTime> travelTimes;
						@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;

						@Override
						public ControlerListener get() {
							Double cellSizeForCellBasedAccessibility = Double.parseDouble(scenario.getConfig().getModule("accessibility").getValue("cellSizeForCellBasedAccessibility"));
							Config config = scenario.getConfig();
							if (cellSizeForCellBasedAccessibility <= 0) {
								throw new RuntimeException("Cell Size needs to be assigned a value greater than zero.");
							}
							BoundingBox bb = BoundingBox.createBoundingBox(scenario.getNetwork());
							AccessibilityCalculator accessibilityCalculator = new AccessibilityCalculator(travelTimes, travelDisutilityFactories, scenario);
							accessibilityCalculator.setMeasuringPoints(GridUtils.createGridLayerByGridSizeByBoundingBoxV2(bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSizeForCellBasedAccessibility));

							GridBasedAccessibilityShutdownListenerV3 listener = new GridBasedAccessibilityShutdownListenerV3(accessibilityCalculator, opportunities, null, config, scenario, travelTimes, travelDisutilityFactories,bb.getXMin(), bb.getYMin(), bb.getXMax(), bb.getYMax(), cellSizeForCellBasedAccessibility);

							// define the modes that will be considered
							// here, the accessibility computation is only done for freespeed
							accessibilityCalculator.setComputingAccessibilityForMode(Modes4Accessibility.freeSpeed, true);

							// add additional facility data to an additional column in the output
							// here, an additional population density column is used
							listener.addAdditionalFacilityData(homes) ;
							listener.writeToSubdirectoryWithName(actType);
							return listener;
						}
					});
				}
			}
		});


		controler.run();
	}

}
