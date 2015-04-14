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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterProviderImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

import javax.inject.Provider;
import java.util.HashSet;
import java.util.Set;

public class RunWithinDayReplanningFromPlansExample {

	public static void main(String[] args){		
		final Controler controler = new Controler("examples/tutorial/programming/example50VeryExperimentalWithinDayReplanning/withinday-config.xml");
		// define the travel time collector (/predictor) that you want to use for routing:
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		final TravelTime travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(controler.getScenario(), analyzedModes);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindToProvider(Mobsim.class, new Provider<Mobsim>() {

					private MyMobsimFactory myMobsimFactory = new MyMobsimFactory(new WithinDayTripRouterFactory(controler, travelTime));

					@Override
					public Mobsim get() {
						return myMobsimFactory.createMobsim(controler.getScenario(), controler.getEvents());
					}
				});
			}
		});
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Controler controler = event.getControler() ;
				controler.getEvents().addHandler((TravelTimeCollector) travelTime);
				controler.getMobsimListeners().add((TravelTimeCollector) travelTime);
				// set the mobsim factory (which insertes the within-day replanning agents):
			}
		}) ;
		controler.run();
	}
	
	private static class WithinDayTripRouterFactory implements Provider<TripRouter> {

		private final Controler controler;
		private final TravelTime travelTime;
		
		public WithinDayTripRouterFactory(Controler controler, TravelTime travelTime) {
			this.controler = controler;
			this.travelTime = travelTime;
		}
		
		@Override
		public TripRouter get() {
			return new TripRouterProviderImpl(
					controler.getScenario(), 
					controler.getTravelDisutilityFactory(),
					travelTime, 
					controler.getLeastCostPathCalculatorFactory(), 
					controler.getTransitRouterFactory()).get();
		}
		
		
	}
}
