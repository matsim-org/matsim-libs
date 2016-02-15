/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mrieser;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import javax.inject.Provider;

public class DemoPeter {

	public static void main(final String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		Scenario scenario = ScenarioUtils.createScenario(config); // check: createScenario or loadScenario

		Controler controler = new Controler(scenario);

		TransitRouterConfig routerConfig = new TransitRouterConfig(config.planCalcScore(), config.plansCalcRoute(), config.transitRouter(), config.vspExperimental());
		routerConfig.setSearchRadius(2000);
		final MyTransitRouterFactory transitRouterFactory = new MyTransitRouterFactory(((MutableScenario) scenario).getTransitSchedule(), routerConfig);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(transitRouterFactory);
			}
		});

		controler.run();
	}

	public static class MyTransitRouterFactory implements Provider<TransitRouter> {

		private final TransitSchedule schedule;
		private final TransitRouterConfig config;
		private final TransitRouterNetwork routerNetwork;

		public MyTransitRouterFactory(final TransitSchedule schedule, final TransitRouterConfig config) {
			this.schedule = schedule;
			this.config = config;
			this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.getBeelineWalkConnectionDistance());
		}

		@Override
		public TransitRouter get() {
			MyTransitRouterNetworkTravelTimeCost ttcalc = new MyTransitRouterNetworkTravelTimeCost(this.config);
			return new TransitRouterImpl(this.config, new PreparedTransitSchedule(schedule), this.routerNetwork, ttcalc, ttcalc);
		}
	}

	public static class MyTransitRouterNetworkTravelTimeCost extends TransitRouterNetworkTravelTimeAndDisutility {

		public MyTransitRouterNetworkTravelTimeCost(final TransitRouterConfig config) {
			super(config);
		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
			return super.getLinkTravelDisutility(link, time, person, vehicle, dataManager);
		}

	}

//	public static class Fares {
//		public double adultFare;
//		public double studentFare;
//	}
//
//	Map<Tuple<String, String>, Fares> fares = new HashMap<Tuple<String, String>, Double>();
//	fares.put(new Tuple("FromZoneId", "ToZoneId"), null);
//	fares.get(new Tuple("FromZoneId", "ToZoneId")).adultFare;
}

