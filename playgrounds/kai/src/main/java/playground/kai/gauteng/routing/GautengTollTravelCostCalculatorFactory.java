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
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.roadpricing.RoadPricingScheme;

/**
 * @author kn after bkick after dgrether
 *
 */
public class GautengTollTravelCostCalculatorFactory implements TravelCostCalculatorFactory {

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
	
	public PersonalizableTravelCost createTravelCostCalculator(PersonalizableTravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		final PersonalizableTravelCost delegate = new TravelTimeDistanceCostCalculator(timeCalculator, cnScoringGroup);

		if ( !isUsingRoadpricing ) {
			
			return delegate ;
			
		} else {
			final GautengLinkTollCalculator tollCostCalculator = new GautengLinkTollCalculator(scheme);

			return new PersonalizableTravelCost() {
				@Override
				public void setPerson(Person person) {
					delegate.setPerson(person);
					tollCostCalculator.setPerson(person);
				}
				@Override
				public double getLinkGeneralizedTravelCost(Link link, double time) {
					double linkTravelDisutility = delegate.getLinkGeneralizedTravelCost(link, time);
					double utilityOfMoney = 1. ;
					linkTravelDisutility += utilityOfMoney * tollCostCalculator.getLinkTollForCars(link, time);
					return linkTravelDisutility ;
				}
			};

		}
	}

}
