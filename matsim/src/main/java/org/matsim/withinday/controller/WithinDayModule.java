/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WithinDayModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.withinday.controller;

import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Named;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;
import org.matsim.withinday.trafficmonitoring.WithinDayTravelTimeModule;

import com.google.inject.Provides;

public class WithinDayModule extends AbstractModule {
    @Override
    public void install() {
        install(new WithinDayTravelTimeModule());
        bind(WithinDayEngine.class);
        bind(Mobsim.class).toProvider(WithinDayQSimFactory.class);
        bind(FixedOrderSimulationListener.class).asEagerSingleton();
        bind(WithinDayControlerListener.class).asEagerSingleton();
        addControlerListenerBinding().to(WithinDayControlerListener.class);
        bind(MobsimDataProvider.class).asEagerSingleton();
        bind(ActivityReplanningMap.class).asEagerSingleton();
        bind(LinkReplanningMap.class).asEagerSingleton();
        bind(EarliestLinkExitTimeProvider.class).asEagerSingleton();
    }

    @SuppressWarnings("static-method")
    @Provides @Named("lowerBound") Map<String, TravelTime> provideEarliestLinkExitTravelTimes(Map<String, TravelTime> travelTimes) {
        Map<String, TravelTime> earliestLinkExitTravelTimes = new HashMap<>();
        earliestLinkExitTravelTimes.putAll(travelTimes);
        earliestLinkExitTravelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
        return earliestLinkExitTravelTimes;
    }
}
