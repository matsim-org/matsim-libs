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

package org.matsim.core.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author mrieser
 */
final class RandomizingTimeDistanceTravelDisutility implements TravelDisutility {

	private final TravelTime timeCalculator;
	private final ScoringConfigGroup scoringConfigGroup;
	private final String mode;

	private final double normalization;
	private final double sigma;

	private final Random random;
	private final Map<String, CostCoefficients> coefficientsPerSubpopulation = new HashMap<>();

	// "cache" of the random value
	private double logNormalRnd;
	private Person prevPerson;

	RandomizingTimeDistanceTravelDisutility(
			final TravelTime timeCalculator,
			final ScoringConfigGroup scoringConfigGroup,
			final String mode,
			final double normalization,
			final double sigma) {
		this.timeCalculator = timeCalculator;
		this.scoringConfigGroup = scoringConfigGroup;
		this.mode = mode;
		this.normalization = normalization;
		this.sigma = sigma;
		this.random = sigma != 0 ? MatsimRandom.getLocalInstance() : null;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		// The random term is drawn once per routing request and then reused for all links
		// of that request, so each route evaluation keeps a stable money-time tradeoff.
		if ( sigma != 0. ) {
			if ( person==null ) {
				throw new RuntimeException("you cannot use the randomzing travel disutility without person.  If you need this without a person, set"
						+ " sigma to zero. If you are loading a scenario from a config, set the routingRandomness in the plansCalcRoute config group to zero.") ;
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
			// do not use custom attributes in core??  but what would be a better solution here?? kai, mar'15
			// Is this actually used anywhere? As far as I can see, this is at least no used in this class... td, Oct'15
			person.getCustomAttributes().put("logNormalRnd", logNormalRnd ) ;
		} else {
			logNormalRnd = 1. ;
		}

		// end randomize

		final CostCoefficients coefficients = getCostCoefficients(person);
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		return coefficients.marginalCostOfTime_s * travelTime + logNormalRnd * coefficients.marginalCostOfDistance_m * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		double minimumDisutility = Double.POSITIVE_INFINITY;
		final CostCoefficients coefficients = getCostCoefficients(null);
		if (coefficients != null)
			return (link.getLength() / link.getFreespeed()) * coefficients.marginalCostOfTime_s + coefficients.marginalCostOfDistance_m * link.getLength();
		else {
			// Without a canonical default scoring set, use the minimum across all
			// configured subpopulations as a safe lower bound for the router.
			for (ScoringConfigGroup.ScoringParameterSet scoringParams : scoringConfigGroup.getScoringParametersPerSubpopulation().values()) {
				final CostCoefficients subpopCoefficients = createCostCoefficients(scoringParams);
				final double disutility =
					(link.getLength() / link.getFreespeed()) * subpopCoefficients.marginalCostOfTime_s
						+ subpopCoefficients.marginalCostOfDistance_m * link.getLength();
				minimumDisutility = Math.min(minimumDisutility, disutility);
			}
			if (minimumDisutility == Double.POSITIVE_INFINITY) {
				throw new IllegalStateException("No scoring parameters available for mode " + mode + ".");
			}
			return minimumDisutility;
		}
	}

	/**
	 * Resolves the cost coefficients for the person's subpopulation and falls back to the
	 * configured default scoring parameters if that subpopulation has no own definition.
	 */
	private CostCoefficients getCostCoefficients(Person person) {
		final String subpopulation = PopulationUtils.getSubpopulation(person);
		if (person == null|| subpopulation == null || !scoringConfigGroup.getScoringParametersPerSubpopulation().containsKey(subpopulation)) {
			final CostCoefficients fallback = getDefaultCostCoefficients();
			if (fallback != null) {
				return fallback;
			}
			return null;
		}
		return coefficientsPerSubpopulation.computeIfAbsent(subpopulation, this::createCostCoefficients);
	}

	/**
	 * Returns the default coefficients if possible. As a last resort, any configured
	 * subpopulation is used so the router still has a deterministic fallback.
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
		final ScoringConfigGroup.ScoringParameterSet scoringParams = scoringConfigGroup.getScoringParameters(subpopulation);
		if (scoringParams == null) {
			throw new IllegalStateException("No scoring parameters available for mode " + mode + " and subpopulation " + subpopulation + ".");
		}
		return createCostCoefficients(scoringParams);
	}

	/**
	 * Converts MATSim scoring parameters into the time and distance coefficients used by
	 * the randomized routing disutility calculation for one mode.
	 */
	private CostCoefficients createCostCoefficients(ScoringConfigGroup.ScoringParameterSet scoringParams) {
		final ScoringConfigGroup.ModeParams modeParams = scoringParams.getModeParams().get(mode);
		if (modeParams == null) {
			throw new NullPointerException(mode + " is not part of the valid mode parameters " + scoringParams.getModeParams().keySet());
		}

		final double marginalCostOfTime_s =
				(-modeParams.getMarginalUtilityOfTraveling() / 3600.0) + (scoringParams.getPerforming_utils_hr() / 3600.0);
		final double marginalCostOfDistance_m =
				-modeParams.getMonetaryDistanceRate() * scoringParams.getMarginalUtilityOfMoney()
						- modeParams.getMarginalUtilityOfDistance();

		return new CostCoefficients(marginalCostOfTime_s, marginalCostOfDistance_m);
	}

	private record CostCoefficients(double marginalCostOfTime_s, double marginalCostOfDistance_m) {}
}
