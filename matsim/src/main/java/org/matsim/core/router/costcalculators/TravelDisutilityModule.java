/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * TravelDisutilityModule.java
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

package org.matsim.core.router.costcalculators;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;

public class TravelDisutilityModule extends AbstractModule {

    @Override
    public void install() {
        PlansCalcRouteConfigGroup routeConfigGroup = getConfig().plansCalcRoute();
        for (String mode : routeConfigGroup.getNetworkModes()) {
            addTravelDisutilityFactoryBinding(mode).toInstance(
                    new RandomizingTimeDistanceTravelDisutilityFactory(mode, getConfig().planCalcScore()));
        }
    }

}
