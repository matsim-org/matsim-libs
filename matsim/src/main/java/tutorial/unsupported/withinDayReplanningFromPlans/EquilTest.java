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

package tutorial.unsupported.example50WithinDayReplanningFromPlans;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterProviderImpl;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollectorFactory;

import javax.inject.Provider;
import java.util.HashSet;
import java.util.Set;

public class EquilTest {

	public static void main(String[] args){		
		final Controler controler = new Controler("examples/tutorial/programming/example50VeryExperimentalWithindayReplanning/withinday-config.xml");
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				Controler controler = event.getControler() ;
				
				Set<String> analyzedModes = new HashSet<String>();
				analyzedModes.add(TransportMode.car);
				TravelTime travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(controler.getScenario(), analyzedModes);
				controler.getEvents().addHandler((TravelTimeCollector) travelTime);
				controler.getMobsimListeners().add((TravelTimeCollector) travelTime);
				
				controler.setMobsimFactory(new MyMobsimFactory(new WithinDayTripRouterFactory(event.getControler(), travelTime)));
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
