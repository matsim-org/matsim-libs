/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TravelTimeCalculatorModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.trafficmonitoring;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import javax.inject.Inject;
import javax.inject.Provider;


/**
 * The first module.
 * Mind that it is retrofitted and I didn't touch the classes themselves.
 * For new code, we wouldn't need the extra layer of Providers.
 *
 * @author michaz
 */
public class TravelTimeCalculatorModule extends AbstractModule {

	@Override
	public void install() {
		if (getConfig().travelTimeCalculator().getSeparateModes()) {
			if (getConfig().travelTimeCalculator().isCalculateLinkToLinkTravelTimes()) {
				throw new RuntimeException("separate modes together with link2link routing currently not implemented. doesn't look difficult, "
						+ "but I cannot say if it would be picked up correctly by downstream modules.  kai, nov'16") ;
			}			
			// go through all modes:
			for (final String mode : CollectionUtils.stringToSet(getConfig().travelTimeCalculator().getAnalyzedModes())) {
				
				// generate and bind the observer:
				bind(TravelTimeCalculator.class).annotatedWith(Names.named(mode)).toProvider(new SingleModeTravelTimeCalculatorProvider(mode)).in(Singleton.class);

				// bind the observer to travel time provider (for router):
				addTravelTimeBinding(mode).toProvider(new Provider<TravelTime>() {
					@Inject Injector injector;
					@Override public TravelTime get() {
						return injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named(mode))).getLinkTravelTimes();
					}
				});
			}
		} else {
			bind(TravelTimeCalculator.class).in(Singleton.class);
			if (getConfig().travelTimeCalculator().isCalculateLinkTravelTimes()) {
				for (String mode : CollectionUtils.stringToSet(getConfig().travelTimeCalculator().getAnalyzedModes())) {
					addTravelTimeBinding(mode).toProvider(ObservedLinkTravelTimes.class);
				}
			}
			if (getConfig().travelTimeCalculator().isCalculateLinkToLinkTravelTimes()) {
				bind(LinkToLinkTravelTime.class).toProvider(ObservedLinkToLinkTravelTimes.class);
			}
		}
	}

	private static class SingleModeTravelTimeCalculatorProvider implements Provider<TravelTimeCalculator> {

		@Inject TravelTimeCalculatorConfigGroup config;
		@Inject EventsManager eventsManager;
		@Inject Network network;

		private String mode;

		SingleModeTravelTimeCalculatorProvider(String mode) {
			this.mode = mode;
		}

		@Override
		public TravelTimeCalculator get() {
			TravelTimeCalculator calculator = new TravelTimeCalculator(network, config.getTraveltimeBinSize(), config.getMaxTime(), 
					config.isCalculateLinkTravelTimes(), config.isCalculateLinkToLinkTravelTimes(), true, CollectionUtils.stringToSet(mode));
			eventsManager.addHandler(calculator);
			return TravelTimeCalculator.configure(calculator, config, network);
		}
	}

}
