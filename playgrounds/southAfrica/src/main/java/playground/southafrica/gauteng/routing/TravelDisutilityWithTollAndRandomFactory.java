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

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

/**
 * @author kn after bkick after dgrether
 *
 */
public class TravelDisutilityWithTollAndRandomFactory implements TravelDisutilityFactory {
	private static final Logger log = Logger.getLogger(TravelDisutilityWithTollAndRandomFactory.class ) ;

//	private static int wrnCnt = 0;

	private RoadPricingScheme scheme = null ;
	private double sigma = 0. ;

	public TravelDisutilityWithTollAndRandomFactory( Scenario scenario ) {
	}

	private static int infoCnt = 0 ;
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, final PlanCalcScoreConfigGroup cnScoringGroup) {
		infoCnt++ ;
		if ( infoCnt<10 ) {
			log.warn( "calling createTravelDisutility" ) ;
		}
		
		final Random random = MatsimRandom.getLocalInstance() ;

		final TravelDisutility delegate = new TravelTimeAndDistanceBasedTravelDisutility(timeCalculator, cnScoringGroup) ;

		final double normalization = 1./Math.exp( this.sigma*this.sigma/2 );
		if ( infoCnt<10 ) {
			log.warn(" sigma: " + this.sigma + "; resulting normalization: " + normalization ) ;
		}
		
		// anonymous class:
		final RoadPricingScheme localScheme = this.scheme ; // (generating final variable for anonymous class)
		return new TravelDisutility() {
			private Person prevPerson = null ;
			private double utilityOfMoney_normally_positive = 0. ;
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				if ( person != prevPerson ) {
					prevPerson = person ;
					this.utilityOfMoney_normally_positive = cnScoringGroup.getMarginalUtilityOfMoney() ;

					// randomize if applicable:
					if ( sigma != 0. ) {
						double logNormal = Math.exp( sigma * random.nextGaussian() ) ;
						logNormal *= normalization ;
						// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
						// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

						/* The argument is something like this:<ul> 
						 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
						 * <li> The mean of this is exp( mu + sigma^2/2 ) .  
						 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
						 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
						 * </ul>
						 * kai, jan'14
						 */
						
						this.utilityOfMoney_normally_positive *= logNormal ;
						// yy the expectation value of this should be tested ...
					}
					// end randomize

				}
				double linkTravelDisutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);

				// apply toll if applicable:
				if ( localScheme != null ) {
					double toll_usually_positive = 0. ;
//					Id vehicleId = null ;
//					if ( vehicle != null ) {
//						vehicleId  = vehicle.getId() ;
//					} else{
//						vehicleId = person.getId() ;
//						if ( wrnCnt<1 ) {
//							wrnCnt++ ;
//							Logger.getLogger(this.getClass()).warn( "still taking vehicle id from driver id (presumably during routing)") ;
//							Logger.getLogger(this.getClass()).warn( Gbl.ONLYONCE ) ;
//						}
//					}
//					Cost cost = localScheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicleId ) ;
					// yyyy I (kn) think we should re-run the abmtrans paper with getLinkCostInfo( link.getId(), time, null, null ) .  kai, jul'14
					Cost cost = localScheme.getTypicalLinkCostInfo(link.getId(), time) ;
					
					if ( cost != null ) {
						/* This needed to be introduced after the GautengRoadPricingScheme started to return null instead of
						 * Cost objects with amount=0.  kai, apr'12
						 */
						if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_DISTANCE) ) {
							toll_usually_positive = link.getLength() * cost.amount ;
						} else if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_LINK ) ) {
							toll_usually_positive = cost.amount ;
						} else {
							throw new RuntimeException("not set up for toll type: " + localScheme.getType() + ". aborting ...") ;
						}

						linkTravelDisutility += utilityOfMoney_normally_positive * toll_usually_positive ;
						// positive * positive = positive, i.e. correct (since it is a positive disutility contribution)
					}
					// end toll

				}

				return linkTravelDisutility;
			}

			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return delegate.getLinkMinimumTravelDisutility(link) ;
			}
		};
	}
	
	/**
	 * This sets the width parameter of the randomness; 0 (default) means no randomness.  This varies the utility of money with 
	 * respect to toll (not even with respect to distance).
	 * 
	 * @param val
	 */
	public void setRandomness( double val ) {
		this.sigma = val ;
	}

	/**
	 * Obviously, this is for setting a road pricing scheme.  If a road pricing scheme is set, it will be considered.  Otherwise,
	 * tolls are assumed to be zero everywhere.
	 * 
	 * @param scheme
	 */
	public void setRoadPricingScheme( RoadPricingScheme scheme ) {
		this.scheme = scheme ;
	}



}
