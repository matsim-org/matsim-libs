/* *********************************************************************** *
 * project: org.matsim.*
 * MSATollTravelDisutilityCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accessibility.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author dziemke
 */
public final class Coord2CoordTimeDistanceTravelDisutility implements TravelDisutility {

	private static final Logger log = Logger.getLogger(Coord2CoordTimeDistanceTravelDisutility.class);

//	private final RandomizingTimeDistanceTravelDisutility delegate;
	private final TravelDisutility delegate;
	private final double marginalCostOfTime_s;
	private final double marginalCostOfDistance_m;

	// needed for beeline-based travel time calculation
	private final double walkSpeed_m_s;


	// === start Builder ===
	public static class Builder implements TravelDisutilityFactory{
		private final String mode;
		private double walkSpeed_m_s = 0.; // TODO meaningful default
		private final PlanCalcScoreConfigGroup cnScoringGroup;

		public Builder( final String mode, final PlanCalcScoreConfigGroup cnScoringGroup ) {
			this.mode = mode;
			this.cnScoringGroup = cnScoringGroup;
			
		}

		@Override
		public Coord2CoordTimeDistanceTravelDisutility createTravelDisutility(final TravelTime timeCalculator) {
			if (mode != TransportMode.walk) {
				throw new NullPointerException("This disutility only works properly for the \"walk\" mode.");
			}
			
			final ModeParams params = cnScoringGroup.getModes().get(mode) ;
			if (params == null) {
				throw new NullPointerException(mode + " is not part of the valid mode parameters " + cnScoringGroup.getModes().keySet());
			}
			
			// analogous to "RandomizingTimeDistanceTravelDisutility"
			final double marginalCostOfTime_s = (-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
			final double marginalCostOfDistance_m = -params.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney();
			//

			return new Coord2CoordTimeDistanceTravelDisutility(
					timeCalculator,
					cnScoringGroup,
					marginalCostOfTime_s,
					marginalCostOfDistance_m,
					walkSpeed_m_s);
		}

		
		public Builder setWalkSpeed(double walkSpeed_m_s) {
			this.walkSpeed_m_s = walkSpeed_m_s;
			return this;
		}
	}  
	// === end Builder ===

	
	private Coord2CoordTimeDistanceTravelDisutility(
			final TravelTime timeCalculator,
			final PlanCalcScoreConfigGroup cnScoringGroup,
			final double marginalCostOfTime_s,
			final double marginalCostOfDistance_m,
			final double walkSpeed_m_s) {
		this.marginalCostOfTime_s = marginalCostOfTime_s;
		this.marginalCostOfDistance_m = marginalCostOfDistance_m;
		this.walkSpeed_m_s = walkSpeed_m_s;
		
		if (walkSpeed_m_s == 0.) {
			log.warn("The walk speed is currently set to " + walkSpeed_m_s + "m/s. It should be set to a reasonable value"
					+ " in order to obtain meaningful results.");
		}
		
//		final RandomizingTimeDistanceTravelDisutility.Builder builder = new RandomizingTimeDistanceTravelDisutility.Builder(TransportMode.walk, cnScoringGroup);
		RandomizingTimeDistanceTravelDisutilityFactory builder = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.walk, cnScoringGroup);
		this.delegate = builder.createTravelDisutility(timeCalculator);
	}


	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		return this.delegate.getLinkTravelDisutility(link, time, person, vehicle);
	}
	
	
	/**
	 * Calculates a travel disutility for the walk (!) from one coordinate to another
	 * As this is a beeline walk, this is time-*in*dependent
	 */
	public double getCoord2CoordTravelDisutility(final Coord fromCoord, final Coord toCoord) {
		double distance_m 	= NetworkUtils.getEuclideanDistance(fromCoord, toCoord);
		double walkTravelTime_s = distance_m / this.walkSpeed_m_s;

		return this.marginalCostOfTime_s * walkTravelTime_s + this.marginalCostOfDistance_m * distance_m;
	}


	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return this.delegate.getLinkMinimumTravelDisutility(link);
	}
}