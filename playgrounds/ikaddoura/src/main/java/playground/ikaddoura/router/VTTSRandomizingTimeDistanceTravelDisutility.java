/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.ikaddoura.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.analysis.vtts.VTTSHandler;

import java.util.Random;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs.
 * 
 * Time is converted into costs taking into account the person-specific VTTS. 
 *
 * @author mrieser, ikaddoura
 */
public final class VTTSRandomizingTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger( VTTSRandomizingTimeDistanceTravelDisutility.class ) ;

	private static int normalisationWrnCnt = 0; 

	private final TravelTime timeCalculator;
	private final PlanCalcScoreConfigGroup cnScoringGroup;
	private final VTTSHandler vttsHandler;

	private final double normalization ;
	private double sigma ;

	private Random random;

	private double logNormalRnd;

	private Person prevPerson;
	
	private static int wrnCnt = 0 ;

	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory{
		private double sigma = 0. ;
		private VTTSHandler vttsHandler;
		public Builder() {
		}
		@Override
		public VTTSRandomizingTimeDistanceTravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
                return new VTTSRandomizingTimeDistanceTravelDisutility(timeCalculator, cnScoringGroup, this.sigma, vttsHandler )  ;
		}
		public void setSigma( double val ) {
			this.sigma = val ;
		}
	}  
	// === end Builder ===

	VTTSRandomizingTimeDistanceTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, double sigma, VTTSHandler vttsHandler) {
		this.timeCalculator = timeCalculator;
		this.vttsHandler = vttsHandler;
		this.cnScoringGroup = cnScoringGroup;
		
		
		ModeParams params = cnScoringGroup.getModes().get( TransportMode.car ) ;
		if ( params.getMarginalUtilityOfDistance() !=  0.0 ) {
			throw new RuntimeException( "marginal utility of distance not honored for travel disutility; aborting ... (should be easy to implement)") ;
		}
				
		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if ( cnScoringGroup.getModes().get( TransportMode.car ).getMonetaryDistanceRate() > 0. ) {
				Logger.getLogger(this.getClass()).warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal " +
				"behavior; just found positive.  Continuing anyway.  This behavior may be changed in the future.") ;
			}
		}
		
		this.sigma = sigma ;
		if ( sigma != 0. ) {
			this.random = MatsimRandom.getLocalInstance() ;
			this.normalization = 1./Math.exp( this.sigma*this.sigma/2 );
			if ( normalisationWrnCnt < 10 ) {
				normalisationWrnCnt++ ;
				log.info(" sigma: " + this.sigma + "; resulting normalization: " + normalization ) ;
			}
		} else {
			this.normalization = 1. ;
		}

	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		// randomize if applicable:
		if ( sigma != 0. ) {
			if ( person==null ) {
				throw new RuntimeException("you cannot use the randomzing travel disutility without person.  If you need this without a person, set"
						+ "sigma to zero.") ;
			}
			if ( person != prevPerson ) {
				prevPerson = person ;

				logNormalRnd = Math.exp( sigma * random.nextGaussian() ) ;
				logNormalRnd *= normalization ;
				// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
				// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

				/* The argument is something like this:<ul> 
				 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
				 * <li> The mean of this is exp( mu + sigma^2/2 ) .  
				 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
				 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
				 * </ul>
				 * Should be tested. kai, jan'14 */
			}
			person.getCustomAttributes().put("logNormalRnd", logNormalRnd ) ; // do not use custom attributes in core??  but what would be a better solution here?? kai, mar'15
		} else {
			logNormalRnd = 1. ;
		}
		
		// end randomize
		
		double travelTime_sec = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		
		double marginalCostOfDistance = - cnScoringGroup.getModes().get( TransportMode.car ).getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney() ;

		double vtts_hour = this.vttsHandler.getVTTS(person.getId(), time);
		
		double linkTravelDisutility = vtts_hour * cnScoringGroup.getMarginalUtilityOfMoney() * travelTime_sec / 3600. + logNormalRnd * marginalCostOfDistance * link.getLength();
		System.out.println(linkTravelDisutility);
		return linkTravelDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		throw new UnsupportedOperationException("I have not yet figured out why this method is needed. Aborting...");
	}

}
