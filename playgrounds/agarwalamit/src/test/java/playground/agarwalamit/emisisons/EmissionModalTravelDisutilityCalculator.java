/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.emisisons;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculator;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;

import java.util.Set;

/**
 * Created by amit on 19/09/16.
 */


public class EmissionModalTravelDisutilityCalculator {

//    EmissionTravelDisutilityCalculator delegate ;
//
//    public EmissionTravelDisutilityCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, EmissionModule emissionModule, EmissionCostModule emissionCostModule, Set<Id<Link>> hotspotLinks) {
//        this.timeCalculator = timeCalculator;
//        this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
//        this.distanceCostRateCar = cnScoringGroup.getModes().get(TransportMode.car).getMonetaryDistanceRate();
//        this.marginalUtlOfTravelTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
//        this.emissionModule = emissionModule;
//        this.emissionCostModule = emissionCostModule;
//        this.hotspotLinks = hotspotLinks;
//    }
//
//    @Override
//    public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle v) {
//        return delegate.getLinkTravelDisutility(link, time, person, v);
//    }
//
//    @Override
//    public double getLinkMinimumTravelDisutility(Link link) {
//        return delegate.getLinkMinimumTravelDisutility(link);
//    }

}
