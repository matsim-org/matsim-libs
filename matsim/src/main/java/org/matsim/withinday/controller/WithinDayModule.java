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

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.mobsim.WithinDayQSimFactory;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

public class WithinDayModule extends AbstractModule {
    @Override
    public void install() {
        binder().bind(TravelTimeCollector.class);
        bindNetworkTravelTime().to(TravelTimeCollector.class);
        binder().bind(WithinDayEngine.class);
        bind(Mobsim.class).toProvider(WithinDayQSimFactory.class);
    }
}
