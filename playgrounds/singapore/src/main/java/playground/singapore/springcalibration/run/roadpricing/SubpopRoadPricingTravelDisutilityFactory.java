/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Builder.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.singapore.springcalibration.run.roadpricing;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.roadpricing.RoadPricingScheme;

public class SubpopRoadPricingTravelDisutilityFactory implements TravelDisutilityFactory {
	private final RoadPricingScheme scheme;
	private CharyparNagelScoringParametersForPerson parameters;
	private TravelDisutilityFactory previousTravelDisutilityFactory;
	private double sigma ;


	public SubpopRoadPricingTravelDisutilityFactory(TravelDisutilityFactory previousTravelDisutilityFactory, 
			RoadPricingScheme scheme, CharyparNagelScoringParametersForPerson parameters) {
		this.scheme = scheme ;
		this.parameters = parameters ;
		this.previousTravelDisutilityFactory = previousTravelDisutilityFactory ;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
//		if ( this.sigma != 0. ) {
//			if ( previousTravelDisutilityFactory instanceof Builder) {
//				((Builder) previousTravelDisutilityFactory).setSigma( this.sigma );
//			} 
////			else {
////				throw new RuntimeException("cannot use sigma!=null together with provided travel disutility factory");
////			}
//		}
		return new SubpopTravelDisutilityIncludingToll(
				previousTravelDisutilityFactory.createTravelDisutility(timeCalculator),
				this.scheme,
				this.parameters,
				this.sigma
		);
	}

	public void setSigma( double val ) {
		this.sigma = val ;
	}

}
