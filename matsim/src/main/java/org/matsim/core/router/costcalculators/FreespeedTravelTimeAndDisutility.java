/* *********************************************************************** *
 * project: org.matsim.*
 * FreespeedTravelTimeCost.java
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

package org.matsim.core.router.costcalculators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**<p>
 * CostCalculator and TravelTimeCalculator for Links based on freespeed on links and
 * distance costs if set.  It sets the <em> function </em> that is to be used with calls
 * <tt>getLinkTravelTime( link, time)</tt> and <tt>getLinkTravelCost( link, time )</tt>.
 * </p><p>
 * The unit of "cost" is defined by the input: if the marginal utilities are given in "utils per second", then
 * cost is in "utils"; if the marginal utilities are given in "euros per second", then cost is in "euros".
 * When the CharyparNagelScoringFunction is used, the values come from the config file, where one is also free to
 * interpret the units.
 * </p>
 * @author mrieser
 * @author dgrether
 */
public class FreespeedTravelTimeAndDisutility implements TravelDisutility, TravelTime, LinkToLinkTravelTime {

	private static final Logger log = LogManager.getLogger(FreespeedTravelTimeAndDisutility.class);

	private final Double fixedTravelCostFactor;
	private final Double fixedMarginalUtlOfDistance;
	private final ScoringConfigGroup scoringConfigGroup;
	private final String mode;
	private final Map<String, CostCoefficients> coefficientsPerSubpopulation = new HashMap<>();
	private static int wrnCnt = 0 ;
	/**
	 *
	 * @param scaledMarginalUtilityOfTraveling Must be scaled, i.e. per second.  Usually negative.
	 * @param scaledMarginalUtilityOfPerforming Must be scaled, i.e. per second.  Usually positive.
	 * @param scaledMarginalUtilityOfDistance Must be scaled, i.e. per meter.  Usually negative.
	 */
	public FreespeedTravelTimeAndDisutility(double scaledMarginalUtilityOfTraveling, double scaledMarginalUtilityOfPerforming,
			double scaledMarginalUtilityOfDistance){
		// usually, the travel-utility should be negative (it's a disutility)
		// but for the cost, the cost should be positive.
		this.fixedTravelCostFactor = -scaledMarginalUtilityOfTraveling + scaledMarginalUtilityOfPerforming;
		this.fixedMarginalUtlOfDistance = scaledMarginalUtilityOfDistance;
		this.scoringConfigGroup = null;
		this.mode = null;

		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if (this.fixedTravelCostFactor <= 0) {
				log.warn("The travel cost in " + this.getClass().getName() + " under normal circumstances should be > 0. " +
						"Currently, it is " + this.fixedTravelCostFactor + "." +
						"That is the sum of the costs for traveling and the opportunity costs." +
						" Please adjust the parameters" +
						"'traveling' and 'performing' in the module 'planCalcScore' in your config file to be" +
				" lower or equal than 0 when added.");
				log.warn(Gbl.ONLYONCE) ;
			}
		}
	}

	public FreespeedTravelTimeAndDisutility(ScoringConfigGroup cnScoringGroup){
		this(cnScoringGroup, TransportMode.car);
	}

	/**
	 * Creates a freespeed disutility that resolves scoring parameters per person subpopulation.
	 * If no exact subpopulation parameters exist, it falls back to the default or root scoring set.
	 */
	public FreespeedTravelTimeAndDisutility(ScoringConfigGroup scoringConfigGroup, String mode) {
		this.fixedTravelCostFactor = null;
		this.fixedMarginalUtlOfDistance = null;
		this.scoringConfigGroup = scoringConfigGroup;
		this.mode = mode;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		final CostCoefficients coefficients = getCostCoefficients(person);
		if (coefficients.scaledMarginalUtilityOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed(time)) * coefficients.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed(time)) * coefficients.travelCostFactor - coefficients.scaledMarginalUtilityOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		double minimumDisutility = Double.POSITIVE_INFINITY;
		if (fixedTravelCostFactor != null) {
			if (fixedMarginalUtlOfDistance == 0.0) {
				return (link.getLength() / link.getFreespeed()) * fixedTravelCostFactor;
			}
			return (link.getLength() / link.getFreespeed()) * fixedTravelCostFactor - fixedMarginalUtlOfDistance * link.getLength();
		}

		final CostCoefficients coefficients = getDefaultCostCoefficients();
		if (coefficients != null) {
			if (coefficients.scaledMarginalUtilityOfDistance == 0.0) {
				return (link.getLength() / link.getFreespeed()) * coefficients.travelCostFactor;
			}
			return (link.getLength() / link.getFreespeed()) * coefficients.travelCostFactor - coefficients.scaledMarginalUtilityOfDistance * link.getLength();
		}

		// If there is no canonical default scoring set, stay conservative and use the
		// smallest disutility over all configured subpopulations.
		for (ScoringConfigGroup.ScoringParameterSet scoringParams : scoringConfigGroup.getScoringParametersPerSubpopulation().values()) {
			final CostCoefficients subpopCoefficients = createCostCoefficients(scoringParams);
			final double disutility;
			if (subpopCoefficients.scaledMarginalUtilityOfDistance == 0.0) {
				disutility = (link.getLength() / link.getFreespeed()) * subpopCoefficients.travelCostFactor;
			} else {
				disutility = (link.getLength() / link.getFreespeed()) * subpopCoefficients.travelCostFactor
					- subpopCoefficients.scaledMarginalUtilityOfDistance * link.getLength();
			}
			minimumDisutility = Math.min(minimumDisutility, disutility);
		}

		if (minimumDisutility == Double.POSITIVE_INFINITY) {
			throw new IllegalStateException("No scoring parameters available for mode " + mode + ".");
		}
		return minimumDisutility;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return link.getLength() / link.getFreespeed(time);
	}

	/**
	 * If travelling freespeed the turning move travel time is not relevant
	 */
	@Override
	public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time, Person person, Vehicle vehicle) {
		return this.getLinkTravelTime(fromLink, time, null, null);
	}

	/**
	 * Resolves the coefficients for the person's subpopulation and applies the same fallback
	 * behavior that is used in the routing disutility factory code paths.
	 */
	private CostCoefficients getCostCoefficients(Person person) {
		if (fixedTravelCostFactor != null) {
			return new CostCoefficients(fixedTravelCostFactor, fixedMarginalUtlOfDistance);
		}

		final String subpopulation = person == null ? null : PopulationUtils.getSubpopulation(person);
		if (subpopulation != null && scoringConfigGroup.getScoringParametersPerSubpopulation().containsKey(subpopulation)) {
			return coefficientsPerSubpopulation.computeIfAbsent(subpopulation, this::createCostCoefficients);

		}
		final CostCoefficients fallback = getDefaultCostCoefficients();
		if (fallback != null) {
			return fallback;
		}
		throw new IllegalStateException("No scoring parameters available for mode " + mode + ".");
	}

	/**
	 * Returns the default scoring coefficients if possible. As a last resort, any configured
	 * subpopulation is used so routing still has a well-defined lower bound.
	 */
	private CostCoefficients getDefaultCostCoefficients() {
		if (scoringConfigGroup.getScoringParametersPerSubpopulation().containsKey(ScoringConfigGroup.DEFAULT_SUBPOPULATION)) {
			return coefficientsPerSubpopulation.computeIfAbsent(ScoringConfigGroup.DEFAULT_SUBPOPULATION, this::createCostCoefficients);
		}
		if (scoringConfigGroup.getScoringParametersPerSubpopulation().containsKey(null)) {
			return coefficientsPerSubpopulation.computeIfAbsent(null, this::createCostCoefficients);
		}
		if (!scoringConfigGroup.getScoringParametersPerSubpopulation().isEmpty()) {
			final String anySubpopulation = scoringConfigGroup.getScoringParametersPerSubpopulation().keySet().iterator().next();
			return coefficientsPerSubpopulation.computeIfAbsent(anySubpopulation, this::createCostCoefficients);
		}
		return null;
	}

	/**
	 * Looks up the scoring parameter set directly by subpopulation key. Fallback resolution
	 * is handled before this method is called.
	 */
	private CostCoefficients createCostCoefficients(String subpopulation) {
		final ScoringConfigGroup.ScoringParameterSet scoringParams = scoringConfigGroup.getScoringParametersPerSubpopulation().get(subpopulation);
		if (scoringParams == null) {
			throw new IllegalStateException("No scoring parameters available for mode " + mode + " and subpopulation " + subpopulation + ".");
		}
		return createCostCoefficients(scoringParams);
	}

	/**
	 * Converts MATSim scoring parameters into the coefficients used by the freespeed
	 * router disutility calculation for one mode.
	 */
	private CostCoefficients createCostCoefficients(ScoringConfigGroup.ScoringParameterSet scoringParams) {
		final ScoringConfigGroup.ModeParams modeParams = scoringParams.getModeParams().get(mode);
		if (modeParams == null) {
			throw new NullPointerException(mode + " is not part of the valid mode parameters " + scoringParams.getModeParams().keySet());
		}

		final double travelCostFactor =
			(-modeParams.getMarginalUtilityOfTraveling() / 3600.0) + (scoringParams.getPerforming_utils_hr() / 3600.0);
		final double scaledMarginalUtilityOfDistance =
			modeParams.getMonetaryDistanceRate() * scoringParams.getMarginalUtilityOfMoney();

		return new CostCoefficients(travelCostFactor, scaledMarginalUtilityOfDistance);
	}

	private record CostCoefficients(double travelCostFactor, double scaledMarginalUtilityOfDistance) {}
}
