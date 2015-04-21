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

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelTime;

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
        // I declare that there is something of type TravelTimeCalculator which
        //  - will exist only once (live as long as the Controler).
        //  - will be available to other bound classes via injection
        //  - will come out of the Provider below.
        // If I was in a script, I could also pass an instance directly which I created myself, but
        // here, the Scenario is not available yet, so I defer construction.
        bind(TravelTimeCalculator.class).toProvider(TravelTimeCalculatorProvider.class).in(Singleton.class);
        // I declare that my single TravelTimeCalculator is an EventHandler.
        // The Controler will wire it into the EventsManager later.
        // (Again, there is a second method to add an instance directly.)
        addEventHandlerBinding().to(TravelTimeCalculator.class);
        // I export interfaces TravelTime and LinkToLinkTravelTime, and I say
        // what is behind them, see below. The Providers
        // are just getters which access the TravelTimeCalculator.
        // I don't say "asSingleton" here, because I don't need to care -
        // The TravelTimeCalculator is the singleton, and the TravelTime reference can be
        // fetched from it as often as the framework wants.
        bind(TravelTime.class).toProvider(TravelTimeProvider.class);
        bind(LinkToLinkTravelTime.class).toProvider(LinkToLinkTravelTimeProvider.class);
    }

    static class TravelTimeProvider implements Provider<TravelTime> {

        @Inject
        TravelTimeCalculator travelTimeCalculator;

        @Override
        public TravelTime get() {
            return travelTimeCalculator.getLinkTravelTimes();
        }

    }

    static class LinkToLinkTravelTimeProvider implements Provider<LinkToLinkTravelTime> {

        @Inject
        TravelTimeCalculator travelTimeCalculator;

        @Override
        public LinkToLinkTravelTime get() {
            return travelTimeCalculator.getLinkToLinkTravelTimes();
        }

    }

    static class TravelTimeCalculatorProvider implements Provider<TravelTimeCalculator> {

        @Inject
        Scenario scenario;

        @Override
        public TravelTimeCalculator get() {
            return TravelTimeCalculator.create(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
        }

    }

}
