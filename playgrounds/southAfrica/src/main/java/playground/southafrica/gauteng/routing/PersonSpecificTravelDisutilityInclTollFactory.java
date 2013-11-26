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
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author kn after bkick after dgrether
 *
 */
public class PersonSpecificTravelDisutilityInclTollFactory implements TravelDisutilityFactory {

	final private RoadPricingScheme scheme;
	private final UtilityOfMoneyI utlOfMon ;

	public PersonSpecificTravelDisutilityInclTollFactory( RoadPricingScheme scheme, UtilityOfMoneyI utlOfMon ) {
		this.scheme = scheme ;
		this.utlOfMon = utlOfMon ;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, 
			PlanCalcScoreConfigGroup cnScoringGroup) {
		final TravelDisutility delegate = new TravelTimeAndDistanceBasedTravelDisutility(timeCalculator, cnScoringGroup);
		final RoadPricingScheme localScheme = this.scheme ;
		final UtilityOfMoneyI localUtlOfMon = this.utlOfMon ;
		
		if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_DISTANCE) ) {
			return new DistanceTollTravelDisutility(delegate, localScheme, localUtlOfMon);
		} else if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_LINK ) ) {
			return new LinkTollTravelDisutility(delegate, localScheme, localUtlOfMon);
		} else {
			/* I guess we can/should take out this exception since `cordon' should now be working? - JWJ Apr '12 */
			/* This still does not work for cordon, and I currently think it never will.  Marcel's cordon toll
			 * is different from other software packages, and so I don't want to mirror the
			 * computation here, especially since we do not need it.  kai, apr'12 */
			throw new RuntimeException("not set up for toll type: " + localScheme.getType() + ". aborting ...") ;
		}
	}
	
	private static class LinkTollTravelDisutility extends DistanceTollTravelDisutility {
		LinkTollTravelDisutility(TravelDisutility delegate, RoadPricingScheme localScheme, UtilityOfMoneyI localUtlOfMon) {
			super(delegate, localScheme, localUtlOfMon);
		}
		@Override
		double calculateToll(final Link link, Cost cost) {
			double toll_usually_positive;
			toll_usually_positive = cost.amount ;
			return toll_usually_positive;
		}
	}
	
	private static class DistanceTollTravelDisutility implements TravelDisutility {
		private final TravelDisutility delegate;
		private final RoadPricingScheme localScheme;
		private final UtilityOfMoneyI localUtlOfMon;

		DistanceTollTravelDisutility(TravelDisutility delegate, RoadPricingScheme localScheme, UtilityOfMoneyI localUtlOfMon) {
			this.delegate = delegate;
			this.localScheme = localScheme;
			this.localUtlOfMon = localUtlOfMon;

		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			double linkTravelDisutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
			double toll_usually_positive = 0. ;
			Cost cost = localScheme.getLinkCostInfo(link.getId(), time, person.getId() ) ;
			if ( cost != null ) {
				/* This needed to be introduced after the GautengRoadPricingScheme started to return null instead of
				 * Cost objects with amount=0.  kai, apr'12
				 */
					toll_usually_positive = calculateToll(link, cost);
			}

			linkTravelDisutility += localUtlOfMon.getMarginalUtilityOfMoney(person.getId() ) * toll_usually_positive ;
			// positive * positive = positive, i.e. correct (since it is a positive disutility contribution)
			
			return linkTravelDisutility;
		}

		double calculateToll(final Link link, Cost cost) {
			double toll_usually_positive;
			toll_usually_positive = link.getLength() * cost.amount ;
			return toll_usually_positive;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException();
		}
	}


}
