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

package playground.vsp.airPollution.flatEmissions;

import java.util.Set;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * benjamin, ihab, amit
 */

public class EmissionTravelDisutilityCalculatorFactory implements TravelDisutilityFactory {

    private double sigma = 0. ;
    private final RandomizingTimeDistanceTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory;
    @Inject  private EmissionModule emissionModule;
    private final EmissionCostModule emissionCostModule;
    private final PlanCalcScoreConfigGroup cnScoringGroup;
    private Set<Id<Link>> hotspotLinks = null;

    public EmissionTravelDisutilityCalculatorFactory(RandomizingTimeDistanceTravelDisutilityFactory randomizedTimeDistanceTravelDisutilityFactory,
                                                     EmissionModule emissionModule,
                                                     EmissionCostModule emissionCostModule,
                                                     PlanCalcScoreConfigGroup cnScoringGroup) {
        this.randomizedTimeDistanceTravelDisutilityFactory = randomizedTimeDistanceTravelDisutilityFactory;
//        this.emissionModule = emissionModule;
        this.emissionCostModule = emissionCostModule;
        this.cnScoringGroup = cnScoringGroup;
    }

    @Override
    public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
        randomizedTimeDistanceTravelDisutilityFactory.setSigma(sigma);

        return new EmissionTollTimeDistanceTravelDisutility(this.randomizedTimeDistanceTravelDisutilityFactory.createTravelDisutility(timeCalculator),
                timeCalculator,
                this.cnScoringGroup.getMarginalUtilityOfMoney(),
                this.emissionModule,
                this.emissionCostModule,
                this.sigma,
                this.hotspotLinks);
    }

    public void setSigma ( double val ) {
        this.sigma = val;
    }

    public void setHotspotLinks(Set<Id<Link>> hotspotLinks) {
        this.hotspotLinks = hotspotLinks;
    }
}
