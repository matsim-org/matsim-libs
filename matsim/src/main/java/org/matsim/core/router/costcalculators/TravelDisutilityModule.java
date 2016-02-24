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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Inject;

public class TravelDisutilityModule extends AbstractModule {

	@Inject PlanCalcScoreConfigGroup cnScoringGroup;
	
    @Override
    public void install() {
        addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(new RandomizingTimeDistanceTravelDisutility.Builder(TransportMode.car, cnScoringGroup));
    }

}
