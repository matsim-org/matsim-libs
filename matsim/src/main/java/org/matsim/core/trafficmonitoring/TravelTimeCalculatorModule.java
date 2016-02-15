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
            for (final String mode : CollectionUtils.stringToSet(getConfig().travelTimeCalculator().getAnalyzedModes())) {
                bind(TravelTimeCalculator.class).annotatedWith(Names.named(mode)).toProvider(new SingleModeTravelTimeCalculatorProvider(mode)).in(Singleton.class);
                addTravelTimeBinding(mode).toProvider(new Provider<TravelTime>() {
                    @Inject Injector injector;
                    @Override
                    public TravelTime get() {
                        return injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named(mode))).getLinkTravelTimes();
                    }
                });
            }
        } else {
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

        @Inject TravelTimeCalculatorConfigGroup config;
        @Inject EventsManager eventsManager;
        @Inject Network network;

        @Override
        public TravelTimeCalculator get() {
            TravelTimeCalculator calculator = new TravelTimeCalculator(network, config.getTraveltimeBinSize(), 30*3600, config.isCalculateLinkTravelTimes(), config.isCalculateLinkToLinkTravelTimes(), config.isFilterModes(), CollectionUtils.stringToSet(config.getAnalyzedModes()));
            eventsManager.addHandler(calculator);
            return TravelTimeCalculator.configure(calculator, config, network);
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
            TravelTimeCalculator calculator = new TravelTimeCalculator(network, config.getTraveltimeBinSize(), 30*3600, config.isCalculateLinkTravelTimes(), config.isCalculateLinkToLinkTravelTimes(), true, CollectionUtils.stringToSet(mode));
            eventsManager.addHandler(calculator);
            return TravelTimeCalculator.configure(calculator, config, network);
        }
    }

}
