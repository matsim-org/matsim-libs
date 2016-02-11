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

    final boolean ONE_FOR_EACH_MODE = true;

    @Override
    public void install() {
        if (ONE_FOR_EACH_MODE) {
            for (final String mode : CollectionUtils.stringToSet(getConfig().travelTimeCalculator().getAnalyzedModes())) {
                bind(TravelTimeCalculator.class).annotatedWith(Names.named(mode)).toProvider(TravelTimeCalculatorProvider.class).in(Singleton.class);
                addTravelTimeBinding(mode).toProvider(new Provider<TravelTime>() {
                    @Inject Injector injector;
                    @Override
                    public TravelTime get() {
                        return injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named(mode))).getLinkTravelTimes();
                    }
                });
            }
        } else {
            // I declare that there is something of type TravelTimeCalculator which
            //  - will exist only once (live as long as the Controler).
            //  - will be available to other bound classes via injection
            //  - will come out of the Provider below.
            // If I was in a script, I could also pass an instance directly which I created myself, but
            // here, the Scenario is not available yet, so I defer construction.
            bind(TravelTimeCalculator.class).toProvider(TravelTimeCalculatorProvider.class).in(Singleton.class);
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

    private static class ObservedLinkTravelTimes implements Provider<TravelTime> {

        @Inject
        TravelTimeCalculator travelTimeCalculator;

        @Override
        public TravelTime get() {
            return travelTimeCalculator.getLinkTravelTimes();
        }

    }

    private static class ObservedLinkToLinkTravelTimes implements Provider<LinkToLinkTravelTime> {

        @Inject
        TravelTimeCalculator travelTimeCalculator;

        @Override
        public LinkToLinkTravelTime get() {
            return travelTimeCalculator.getLinkToLinkTravelTimes();
        }

    }

    private static class TravelTimeCalculatorProvider implements Provider<TravelTimeCalculator> {

        @Inject
        TravelTimeCalculatorConfigGroup config;

        @Inject
        EventsManager eventsManager;

        @Inject
        Network network;

        @Override
        public TravelTimeCalculator get() {
            TravelTimeCalculator calculator = new TravelTimeCalculator(network, config.getTraveltimeBinSize(), 30*3600, config.isCalculateLinkTravelTimes(), config.isCalculateLinkToLinkTravelTimes(), config.isFilterModes(), CollectionUtils.stringToSet(config.getAnalyzedModes()));

            switch ( config.getTravelTimeCalculatorType() ) {
				case TravelTimeCalculatorArray:
					calculator.setTravelTimeDataFactory(new TravelTimeDataArrayFactory(network, calculator.numSlots));
					break;
				case TravelTimeCalculatorHashMap:
					calculator.setTravelTimeDataFactory(new TravelTimeDataHashMapFactory(network));
					break;
				default:
					throw new RuntimeException(config.getTravelTimeCalculatorType() + " is unknown!");
			}

            AbstractTravelTimeAggregator travelTimeAggregator;
            switch( config.getTravelTimeAggregatorType() ) {
			case "optimistic":
				travelTimeAggregator = new OptimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice);
				break ;
			case "experimental_LastMile":
				travelTimeAggregator = new PessimisticTravelTimeAggregator(calculator.numSlots, calculator.timeSlice);
				break ;
			default:
				throw new RuntimeException(config.getTravelTimeAggregatorType() + " is unknown!");
			}
            calculator.setTravelTimeAggregator(travelTimeAggregator);

            TravelTimeGetter travelTimeGetter;
            switch( config.getTravelTimeGetterType() ) {
			case "average":
				travelTimeGetter = new AveragingTravelTimeGetter();
				break ;
			case "linearinterpolation":
				travelTimeGetter = new LinearInterpolatingTravelTimeGetter(calculator.numSlots, calculator.timeSlice);
				break ;
			default:
				throw new RuntimeException(config.getTravelTimeGetterType() + " is unknown!");
			}
            travelTimeAggregator.connectTravelTimeGetter(travelTimeGetter);

            eventsManager.addHandler(calculator);
            return calculator;
        }

    }

}
