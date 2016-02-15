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

package org.matsim.roadpricing;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;

public class RoadPricingTravelDisutilityFactory implements TravelDisutilityFactory {
	private final RoadPricingScheme scheme;
	private final double marginalUtilityOfMoney ;
	private TravelDisutilityFactory previousTravelDisutilityFactory;
	private double sigma = 3. ;

	@Inject
	RoadPricingTravelDisutilityFactory(Scenario scenario, RoadPricingScheme roadPricingScheme) {
		this(ControlerDefaults.createDefaultTravelDisutilityFactory(scenario), roadPricingScheme, scenario.getConfig());
	}

	public RoadPricingTravelDisutilityFactory(TravelDisutilityFactory previousTravelDisutilityFactory, RoadPricingScheme scheme, double marginalUtilityOfMoney) {
		this.scheme = scheme ;
		this.marginalUtilityOfMoney = marginalUtilityOfMoney ;
		this.previousTravelDisutilityFactory = previousTravelDisutilityFactory ;
	}

	public RoadPricingTravelDisutilityFactory(TravelDisutilityFactory previousTravelDisutilityFactory, RoadPricingScheme scheme, Config config) {
		this( previousTravelDisutilityFactory, scheme, config.planCalcScore().getMarginalUtilityOfMoney() ) ;
	}

	@Override
	public TravelDisutility createTravelDisutility(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		if ( this.sigma != 0. ) {
			if ( previousTravelDisutilityFactory instanceof Builder) {
				((Builder) previousTravelDisutilityFactory).setSigma( this.sigma );
			} else {
				throw new RuntimeException("cannot use sigma!=null together with provided travel disutility factory");
			}
		}
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(this.scheme.getType())
				|| RoadPricingScheme.TOLL_TYPE_CORDON.equals(this.scheme.getType())
				|| RoadPricingScheme.TOLL_TYPE_LINK.equals(this.scheme.getType()) ) {
		// yy this is historically without area toll but it might be better to do it also with area toll
		// when the randomizing router is used.  I do think, however, that the current specification
		// of the area toll disutility will not work in that way.  kai, sep'14
			return new TravelDisutilityIncludingToll(
					previousTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup),
					this.scheme,
					this.marginalUtilityOfMoney,
					this.sigma
			);
		} else {
			return previousTravelDisutilityFactory.createTravelDisutility(timeCalculator, cnScoringGroup);
		}
	}

	public void setSigma( double val ) {
		this.sigma = val ;
	}

}
