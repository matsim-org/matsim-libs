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
package playground.southafrica.kai.gauteng;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.gauteng.utilityofmoney.UtilityOfMoneyI;

/**
 * @author kn after bkick after dgrether
 *
 */
public class ConfigurableTravelDisutilityFactory implements TravelDisutilityFactory {
	private static final Logger log = Logger.getLogger(ConfigurableTravelDisutilityFactory.class ) ;

	private final Scenario scenario;
	
	private UtilityOfMoneyI uom = null ;
	private UtilityOfDistanceI uod = null ;
	private UtilityOfTtimeI uott = null ;

	private ScoringFunctionFactory scoringFunctionFactory = null ;
	private RoadPricingScheme scheme = null ;
	private double sigma = 0. ;

	private UtilityOfTtimeI externalUott;

	private UtilityOfDistanceI externalUod;

	private UtilityOfMoneyI externalUom;

	public void setRandomness( double val ) {
		this.sigma = val ;
	}

	/**
	 * Obviously, this is for setting a scoring function factory.  If set, and the effective marginal utilities container is <i>not</i> set,
	 * then this is used to auto-sense the marginal utilities.
	 * 
	 * @param scoringFunctionFactory
	 */
	public void setScoringFunctionFactory( ScoringFunctionFactory scoringFunctionFactory ) {
		this.scoringFunctionFactory = scoringFunctionFactory ;
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

	public ConfigurableTravelDisutilityFactory( Scenario scenario ) {
		this.scenario = scenario ;
	}
	
	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, final PlanCalcScoreConfigGroup cnScoringGroup) {
		log.warn( "calling createTravelDisutility" ) ;
		if ( (this.externalUom==null || this.externalUod==null || this.externalUott==null ) && this.scoringFunctionFactory != null ) { 
			EffectiveMarginalUtilitiesContainer muc = RouterUtils.createMarginalUtilitiesContainer(scenario, scoringFunctionFactory);
			if ( this.externalUom==null ) {
				this.uom = muc ; // works because muc fulfills _all_ the interfaces.  Maybe not so nice.
				log.warn( "using autosensing marginal utility of money") ;
			} else {
				this.uom = this.externalUom ;
				log.warn( " using external marginal utility of money" ) ;
			}
			if ( this.externalUom==null ) {
				this.uod=muc ;
				log.warn( "using autosensing marginal utility of distance") ;
			} else {
				this.uod = this.externalUod ;
				log.warn( " using external marginal utility of distance" ) ;
			}
			if ( this.externalUom==null ) {
				this.uott=muc ;
				log.warn( "using autosensing marginal utility of ttime") ;
			} else {
				this.uott = this.externalUott ;
				log.warn( " using external marginal utility of ttime" ) ;
			}
			// yyyy the above is all not well tested. kai, dec'13
		}
		final UtilityOfMoneyI localUom = this.uom ; // (generating final variable for anonymous class)
		
		TravelDisutility tmp ;
		if ( this.uott==null && this.uod==null ) {
			tmp = new TravelTimeAndDistanceBasedTravelDisutility(timeCalculator, cnScoringGroup) ;
			log.warn("using normal (non-person-based) base travel disutility (i.e. no time pressure) (UoM included later)") ;
		} else {
			tmp = new PersonIndividualTimeDistanceDisutility(timeCalculator, this.uott, this.uod ) ;
			log.warn("using person individual travel disutility (i.e. including time pressure) (UoM included later)") ;
		}
		final TravelDisutility delegate = tmp ; // (generating final variable for anonymous class)
		
		
		final double normalization = 1./Math.exp( this.sigma*this.sigma/2 );
		log.warn(" sigma: " + this.sigma + "; resulting normalization: " + normalization ) ;

		// generating final variables for anonymous class:
		final RoadPricingScheme localScheme = this.scheme ;
		
		int cnt = 0 ;
		
		if ( localUom!=null ) {
			log.warn("will use person-specific UoM");
		}
		
		// anonymous class:
		return new TravelDisutility() {
			private Person prevPerson = null ;
			private double utilityOfMoney_normally_positive = 0. ;
			@Override
			public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				if ( person != prevPerson ) {
					prevPerson = person ;
					if ( localUom!=null ) {
						this.utilityOfMoney_normally_positive = localUom.getMarginalUtilityOfMoney( person.getId() ) ;
					} else {
						this.utilityOfMoney_normally_positive = cnScoringGroup.getMarginalUtilityOfMoney() ;
					}
					
					// randomize if applicable:
					if ( sigma != 0. ) {
						double logNormal = Math.exp( sigma * MatsimRandom.getRandom().nextGaussian() ) ;
						logNormal *= normalization ;
						// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
						// parameter mu, I rather just normalize (which should be the same). kai, nov'13
						
						this.utilityOfMoney_normally_positive *= logNormal ;
						// yy the expectation value of this should be tested ...
					}
					// end randomize
					
				}
				double linkTravelDisutility = delegate.getLinkTravelDisutility(link, time, person, vehicle);
				
				// apply toll if applicable:
				if ( localScheme != null ) {
					double toll_usually_positive = 0. ;
					Id vehicleId = null ;
					if ( vehicle != null ) {
						vehicleId  = vehicle.getId() ;
					}
					Cost cost = localScheme.getLinkCostInfo(link.getId(), time, person.getId(), vehicleId ) ;
					if ( cost != null ) {
						/* This needed to be introduced after the GautengRoadPricingScheme started to return null instead of
						 * Cost objects with amount=0.  kai, apr'12
						 */
						if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_DISTANCE) ) {
							toll_usually_positive = link.getLength() * cost.amount ;
						} else if ( localScheme.getType().equalsIgnoreCase(RoadPricingScheme.TOLL_TYPE_LINK ) ) {
							toll_usually_positive = cost.amount ;
						} else {
							/* I guess we can/should take out this exception since `cordon' should now be working? - JWJ Apr '12 */
							/* This still does not work for cordon, and I currently think it never will.  Marcel's cordon toll
							 * is different from other software packages, and so I don't want to mirror the
							 * computation here, especially since we do not need it.  kai, apr'12 */
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

	public void setUom(UtilityOfMoneyI uom) {
		this.externalUom = uom;
	}

	public void setUod(UtilityOfDistanceI uod) {
		this.externalUod = uod;
	}

	public void setUott(UtilityOfTtimeI uott) {
		this.externalUott = uott;
	}

}
