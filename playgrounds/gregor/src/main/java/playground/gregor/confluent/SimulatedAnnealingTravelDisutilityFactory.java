package playground.gregor.confluent;/* *********************************************************************** *
 * project: org.matsim.*
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class SimulatedAnnealingTravelDisutilityFactory implements org.matsim.core.router.costcalculators.TravelDisutilityFactory {
    @Inject
    Injector injector;

    @Inject
    SimulatedAnnealingTravelDisutility msaTD;

    @Override
    public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
        return msaTD;
    }
}
