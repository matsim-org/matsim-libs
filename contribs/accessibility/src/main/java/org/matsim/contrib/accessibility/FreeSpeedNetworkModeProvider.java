/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author dziemke
 */
public class FreeSpeedNetworkModeProvider implements Provider<AccessibilityContributionCalculator>{

    private String mode;
    @Inject Scenario scenario;
    @Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
    
    public FreeSpeedNetworkModeProvider(String mode) {
        this.mode = mode;
    }
    
    @Override
    public AccessibilityContributionCalculator get() {
        return new NetworkModeAccessibilityExpContributionCalculator( new FreeSpeedTravelTime(), travelDisutilityFactories.get(mode), scenario);
    }
}