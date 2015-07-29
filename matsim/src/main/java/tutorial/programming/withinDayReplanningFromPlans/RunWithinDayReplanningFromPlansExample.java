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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterProviderImpl;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

public class RunWithinDayReplanningFromPlansExample {

	public static void main(String[] args){		
		final Controler controler = new Controler("examples/tutorial/programming/example50VeryExperimentalWithinDayReplanning/withinday-config.xml");
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		// define the travel time collector (/predictor) that you want to use for routing:
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
		controler.getEvents().addHandler( travelTime );
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// construct necessary trip router:
				final TripRouter router = new TripRouterProviderImpl(
						controler.getScenario(), 
						controler.getTravelDisutilityFactory(),
						travelTime, 
						controler.getLeastCostPathCalculatorFactory(), 
						controler.getTransitRouterFactory()).get();

//				this.bindMobsim().toProvider(new Provider<Mobsim>() {
//					@Override
//					public Mobsim get() {
//						
//						// construct qsim and insert listeners:
//						QSim qSim = QSimUtils.createDefaultQSim( controler.getScenario(), controler.getEvents() );
//						qSim.addQueueSimulationListeners(new MyWithinDayMobsimListener(router)) ;
//						qSim.addQueueSimulationListeners( travelTime );
//						return qSim;
//					}
//				});
				
				// I think that the two lines below now replace the commented lines above.  Pls check and let us know.  kai, jul'15
				
				this.addMobsimListenerBinding().toInstance(new MyWithinDayMobsimListener(router));
				this.addMobsimListenerBinding().toInstance( travelTime );
			}
		});
		
		// I just made this a lot shorter.  There is, however, no test for functionality, so keep your eyes open. kai, may'15

		controler.run();
	}
	
}
