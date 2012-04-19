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
package playground.southafrica.gauteng.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingScheme.Cost;
import org.matsim.roadpricing.RoadPricingSchemeI;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author kn after bkick after dgrether
 *
 */
public class GautengTravelDisutilityInclTollFactory implements TravelDisutilityFactory {

	final private RoadPricingSchemeI scheme;
	private final UtilityOfMoneyI utlOfMon ;

	public GautengTravelDisutilityInclTollFactory( RoadPricingSchemeI scheme, UtilityOfMoneyI utlOfMon ) {
		this.scheme = scheme ;
		this.utlOfMon = utlOfMon ;
	}

	@Override
	public TravelDisutility createTravelDisutility(PersonalizableTravelTime timeCalculator, 
			PlanCalcScoreConfigGroup cnScoringGroup) {
		final TravelDisutility delegate = new TravelTimeAndDistanceBasedTravelDisutility(timeCalculator, cnScoringGroup);
		final RoadPricingSchemeI localScheme = this.scheme ;
		final UtilityOfMoneyI localUtlOfMon = this.utlOfMon ;
		
		return new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				double linkTravelDisutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
				double toll_usually_positive = 0. ;
				Cost cost = localScheme.getLinkCostInfo(link.getId(), time, person.getId() ) ;
				if ( localScheme.getType().equals(RoadPricingScheme.TOLL_TYPE_DISTANCE) ) {
					toll_usually_positive = link.getLength() * cost.amount ;
				} else if ( localScheme.getType().equals(RoadPricingScheme.TOLL_TYPE_LINK ) ) {
						toll_usually_positive = cost.amount ;
				} else {
					throw new RuntimeException("not set up for toll type: " + localScheme.getType() + ". aborting ...") ;
				}

				double utilityOfMoney_normally_positive = localUtlOfMon.getUtilityOfMoney_normally_positive(person.getId() ) ; 
				
				linkTravelDisutility += utilityOfMoney_normally_positive * toll_usually_positive ;
				// positive * positive = positive, i.e. correct (since it is a positive disutility contribution)
				
				return linkTravelDisutility;
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}
		};
	}

}
