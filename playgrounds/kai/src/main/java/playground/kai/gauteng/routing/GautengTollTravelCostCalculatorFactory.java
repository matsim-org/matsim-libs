/* *********************************************************************** *
 * project: org.matsim.*
 * Income1TravelCostCalculatorFactory
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.kai.gauteng.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.corelisteners.RoadPricing;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author kn after bkick after dgrether
 *
 */
public class GautengTollTravelCostCalculatorFactory implements TravelDisutilityFactory {

	final boolean isUsingRoadpricing ;
	final private RoadPricingScheme scheme;

	public GautengTollTravelCostCalculatorFactory(boolean isUsingRoadPricing, RoadPricing roadPricing) {
		this.isUsingRoadpricing = isUsingRoadPricing ;
		if ( isUsingRoadPricing ) {
			this.scheme = roadPricing.getRoadPricingScheme() ;
		} else {
			this.scheme = null ;
		}
	}
	
	public PersonalizableTravelDisutility createTravelDisutility(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		final PersonalizableTravelDisutility delegate = new TravelTimeAndDistanceBasedTravelDisutility(timeCalculator, cnScoringGroup);

		if ( !isUsingRoadpricing ) {
			
			return delegate ;
			
		} else {
			final GautengLinkTollCalculator tollCostCalculator = new GautengLinkTollCalculator(scheme);

			return new PersonalizableTravelDisutility() {
				@Override
				public void setPerson(Person person) {
					delegate.setPerson(person);
					tollCostCalculator.setPerson(person);
				}
				@Override
				public double getLinkTravelDisutility(Link link, double time) {
					double linkTravelDisutility = delegate.getLinkTravelDisutility(link, time);
					double utilityOfMoney = 1. ;
					linkTravelDisutility += utilityOfMoney * tollCostCalculator.getLinkTollForCars(link, time);
					return linkTravelDisutility ;
				}
			};

		}
	}

}
