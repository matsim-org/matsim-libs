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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import java.util.Map;
import java.util.HashMap;

/**
 * <p>
 * CostCalculator and TravelTimeCalculator for Links based on freespeed on links
 * and distance costs if set. It sets the <em> function </em> that is to be used
 * with calls <tt>getLinkTravelTime( link, time)</tt> and
 * <tt>getLinkTravelCost( link, time )</tt>.
 * </p>
 * <p>
 * The unit of "cost" is defined by the input: if the marginal utilities are
 * given in "utils per second", then cost is in "utils"; if the marginal
 * utilities are given in "euros per second", then cost is in "euros". When the
 * CharyparNagelScoringFunction is used, the values come from the config file,
 * where one is also free to interpret the units.
 * </p>
 * 
 * @author mrieser
 * @author dgrether
 */
public class FreespeedTravelTimeAndDisutility implements TravelDisutility, TravelTime, LinkToLinkTravelTime {

	private static final Logger log = Logger.getLogger(FreespeedTravelTimeAndDisutility.class);

	private static class Parameters {
		private final double travelCostFactor;
		private final double marginalUtlOfDistance;

	    Parameters(double scaledMarginalUtilityOfTraveling,
				double scaledMarginalUtilityOfPerforming, double scaledMarginalUtilityOfDistance) {
			this(scaledMarginalUtilityOfTraveling + scaledMarginalUtilityOfPerforming, scaledMarginalUtilityOfDistance);
		}

		Parameters(final double travelCostFactor, final double marginalUtlOfDistance) {
			this.travelCostFactor = travelCostFactor;
			this.marginalUtlOfDistance = marginalUtlOfDistance;
		}
	}

	private final Map<String, Parameters> parametersPerSubpopulation = new HashMap<>();
	private double minTravelCostFactor = Double.POSITIVE_INFINITY;
	private double maxUtilOfDistance = Double.NEGATIVE_INFINITY;

	private Person currentPerson = null;
	private Parameters currentParameters = null;

	private static int wrnCnt = 0;

	/**
	 *
	 * @param scaledMarginalUtilityOfTraveling  Must be scaled, i.e. per second.
	 *                                          Usually negative.
	 * @param scaledMarginalUtilityOfPerforming Must be scaled, i.e. per second.
	 *                                          Usually positive.
	 * @param scaledMarginalUtilityOfDistance   Must be scaled, i.e. per meter.
	 *                                          Usually negative.
	 */
	public FreespeedTravelTimeAndDisutility(double scaledMarginalUtilityOfTraveling,
			double scaledMarginalUtilityOfPerforming, double scaledMarginalUtilityOfDistance) {
		// store those "population wide" parameters with null key. Not optimal...

		// usually, the travel-utility should be negative (it's a disutility)
		// but for the cost, the cost should be positive.
		this.parametersPerSubpopulation.put(null,
			new Parameters(
				scaledMarginalUtilityOfTraveling,
				scaledMarginalUtilityOfPerforming,
				scaledMarginalUtilityOfDistance));

		updateMinCosts();

		if (wrnCnt < 1) {
			double costFactor = parametersPerSubpopulation.get(null).travelCostFactor;
			if (costFactor <= 0) {
				wrnCnt++;
				log.warn("The travel cost in " + this.getClass().getName()
						+ " under normal circumstances should be > 0. " + "Currently, it is " + costFactor + "."
						+ "That is the sum of the costs for traveling and the opportunity costs."
						+ " Please adjust the parameters"
						+ "'traveling' and 'performing' in the module 'planCalcScore' in your config file to be"
						+ " lower or equal than 0 when added.");
				log.warn(Gbl.ONLYONCE);
			}
		}
	}

	private void updateMinCosts() {
		for (Parameters parameters : parametersPerSubpopulation.values()) {
			if (parameters.travelCostFactor < minTravelCostFactor) minTravelCostFactor = parameters.travelCostFactor;
			if (parameters.marginalUtlOfDistance > maxUtilOfDistance) maxUtilOfDistance = parameters.marginalUtlOfDistance;
		}
	}

	public FreespeedTravelTimeAndDisutility(PlanCalcScoreConfigGroup cnScoringGroup) {
		for (ScoringParameterSet scoringParameters : cnScoringGroup.getScoringParametersPerSubpopulation().values()) {
			this.parametersPerSubpopulation.put(
				scoringParameters.getSubpopulation(),
				new Parameters(
					scoringParameters.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0,
					scoringParameters.getPerforming_utils_hr() / 3600.0,
					scoringParameters.getModes().get(TransportMode.car).getMonetaryDistanceRate() *scoringParameters.getMarginalUtilityOfMoney()));
		}

		updateMinCosts();
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		updateCurrentParameters(person);
		if (currentParameters.marginalUtlOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed(time)) * currentParameters.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed(time)) * currentParameters.travelCostFactor - currentParameters.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		// not sure why that check was included... td Feb 20
		if (this.maxUtilOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed()) * this.minTravelCostFactor;
		}
		return (link.getLength() / link.getFreespeed()) * this.minTravelCostFactor
		- this.maxUtilOfDistance * link.getLength();
	}

	private final void updateCurrentParameters(Person person) {
		if (person == currentPerson) return;
		currentPerson = person;
		currentParameters = parametersPerSubpopulation.get(PopulationUtils.getSubpopulation(person));
		if (currentParameters == null) {
			currentParameters = parametersPerSubpopulation.get(null);
		}
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
	
}
