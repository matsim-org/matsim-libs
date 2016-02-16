/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package tutorial.programming.withinDayReplanningFromPlans;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

public class RunWithinDayReplanningFromPlansExample {

	public static void main(String[] args){
		final Config config = ConfigUtils.loadConfig( "examples/tutorial/programming/example50VeryExperimentalWithinDayReplanning/withinday-config.xml" );
		final Scenario scenario = ScenarioUtils.createScenario( config );
		final Controler controler = new Controler( scenario );

		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		// (need to make sure that test fails if it does not get the output directory right! kai, nov'15)

		// define the travel time collector (/predictor) that you want to use for routing:
		Set<String> analyzedModes = new HashSet<>();
		analyzedModes.add(TransportMode.car);
		final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance( travelTime );
				this.bind(TravelTime.class).toInstance(travelTime);
				this.addMobsimListenerBinding().to(MyWithinDayMobsimListener.class);
				this.addMobsimListenerBinding().toInstance(travelTime);
			}
		});
		controler.run();
	}
	
}
