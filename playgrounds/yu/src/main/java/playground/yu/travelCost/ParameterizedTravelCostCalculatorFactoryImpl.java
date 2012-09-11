/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTravelCostCalculatorFactoryImpl
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
package playground.yu.travelCost;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author dgrether
 *
 */
public class ParameterizedTravelCostCalculatorFactoryImpl implements TravelDisutilityFactory {
	public static class ParameterizedTravelTimeDistanceCostCalculator implements TravelDisutility {

		protected final TravelTime timeCalculator;
		private final double travelCostFactor;
		private final double marginalUtlOfDistance;

		/**
		 * @param timeCalculator
		 * @param cnScoringGroup
		 * @param A
		 *            [0,1]
		 */
		public ParameterizedTravelTimeDistanceCostCalculator(
				final TravelTime timeCalculator,
				PlanCalcScoreConfigGroup cnScoringGroup, double A) {
			this.timeCalculator = timeCalculator;
			/*
			 * Usually, the travel-utility should be negative (it's a
			 * disutility) but the cost should be positive. Thus negate the
			 * utility.
			 */
			travelCostFactor = (-cnScoringGroup.getTraveling_utils_hr() / 3600d + cnScoringGroup
					.getPerforming_utils_hr() / 3600d) * A;

			// this.marginalUtlOfDistance =
			// cnScoringGroup.getMarginalUtlOfDistanceCar();
			marginalUtlOfDistance = cnScoringGroup
					.getMonetaryDistanceCostRateCar()
					* cnScoringGroup.getMarginalUtilityOfMoney() * (1 - A);

		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			double travelTime = timeCalculator.getLinkTravelTime(link, time, person, vehicle);
			if (marginalUtlOfDistance == 0.0) {
				return travelTime * travelCostFactor;
			}
			return travelTime * travelCostFactor - marginalUtlOfDistance
					* link.getLength();
		}

		@Override
		public double getLinkMinimumTravelDisutility(final Link link) {
			if (marginalUtlOfDistance == 0.0) {
				return link.getLength() / link.getFreespeed()
						* travelCostFactor;
			}
			return link.getLength() / link.getFreespeed() * travelCostFactor
					- marginalUtlOfDistance * link.getLength();
		}
	}

	private final double A;

	/**
	 * @param A
	 *            - weight of travel time as impedance
	 */
	public ParameterizedTravelCostCalculatorFactoryImpl(double A) {
		this.A = A;
	}

	@Override
	public TravelDisutility createTravelDisutility(
			TravelTime timeCalculator,
			PlanCalcScoreConfigGroup cnScoringGroup) {
		return new ParameterizedTravelTimeDistanceCostCalculator(
				timeCalculator, cnScoringGroup, A);
	}

}
