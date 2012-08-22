/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRoute.java
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

package org.matsim.core.router;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**<p>
 * This is, I think, a configurable wrapper/adapter class that essentially uses
 * <tt>(new DijkstraFactory()).createPathCalculator( costCalculator, timeCalculator )</tt>
 * to bundle costCalculator and timeCalculator with the Dijkstra s.p. algorithm into a MATSim Person/PlanAlgo.
 * </p><p>
 * Dijkstra can be replaced by something else by using the <tt>factory</tt> parameter.
 * </p>
   The PlanStrategy
 * is then plugged into a PlanStrategyModule (where, essentially, the planStrategyModule.handlePlan(plan) is connected to the
 * planStrategy.run(plan) method).
 * <p/>
 * Design thoughts:
 * <ul>
 * <li>Do we really need separate methods planStrategy.run(plan) and planStrategyModule.handlePlan(plan). Or could
 * PlanStrategyModule inherit the method name from PlanStrategy?
 * <li>Do we really need yet another plansCalcRoute.handlePlan( person, plan ) method? Presumably a leftover from the days where
 * plan did not have a back pointer to person.
 * </ul>
 * */
public class PlansCalcRoute extends AbstractPersonAlgorithm implements PlanAlgorithm {
	
	static final Logger log = Logger.getLogger(PlansCalcRoute.class);

	private static final String NO_CONFIGGROUP_SET_WARNING = "No PlansCalcRouteConfigGroup"
		+ " is set in PlansCalcRoute, using the default values. Make sure that those values" +
		"fit your needs, otherwise set it expclicitly.";

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	/**
	 * The routing algorithm to be used for finding routes on the network with actual travel times.
	 */
	private final LeastCostPathCalculator routeAlgo;
	/**
	 * The routing algorithm to be used for finding pt routes, based on the empty network, with freeflow travel times.
	 */
	private final LeastCostPathCalculator routeAlgoPtFreeflow;

	/**
	 * if not set via constructor use the default values
	 */
	protected PlansCalcRouteConfigGroup configGroup = new PlansCalcRouteConfigGroup();

	private final ModeRouteFactory routeFactory;

	protected final Network network;

	private final PersonalizableTravelTime timeCalculator;

	private PlansCalcRouteData data = new PlansCalcRouteData();

	/**Does the following (as far as I can see):<ul>
	 * <li> sets routeAlgo to the path calculator defined by <tt>factory</tt>, using <tt>costCalculator</tt> and <tt>timeCalculator</tt> as arguments </li>
	 * <li> sets routeAlgoPtFreeflow to "-1 utils/sec" (which is <it>enormous</it>--????) </li>
	 * <li> sets configGroup to <tt>group</tt> but it is not clear where this will be used.
	 * </ul>
	 * [[old javadoc: Uses the speed factors from the config group and the rerouting of the factory]]
	 */
	public PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network,
			final TravelDisutility costCalculator,
			final PersonalizableTravelTime timeCalculator, LeastCostPathCalculatorFactory factory, ModeRouteFactory routeFactory) {
		this.routeAlgo = factory.createPathCalculator(network, costCalculator, timeCalculator);
		FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		this.routeAlgoPtFreeflow = factory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);
		this.network = network;
		this.timeCalculator = timeCalculator;
		this.routeFactory = routeFactory;
		if (group != null) {
			this.configGroup = group;
		}
		else {
			log.warn(NO_CONFIGGROUP_SET_WARNING);
		}

		Collection<String> networkModes = this.configGroup.getNetworkModes();
		for (String mode : networkModes) {
			this.addLegHandler(mode, new NetworkLegRouter(this.network, this.routeAlgo, this.routeFactory));
		}
		Map<String, Double> teleportedModeSpeeds = this.configGroup.getTeleportedModeSpeeds();
		for (Entry<String, Double> entry : teleportedModeSpeeds.entrySet()) {
			this.addLegHandler(entry.getKey(), new TeleportationLegRouter(this.routeFactory, entry.getValue(), this.configGroup.getBeelineDistanceFactor()));
		}
		Map<String, Double> teleportedModeFreespeedFactors = this.configGroup.getTeleportedModeFreespeedFactors();
		for (Entry<String, Double> entry : teleportedModeFreespeedFactors.entrySet()) {
			this.addLegHandler(entry.getKey(), new PseudoTransitLegRouter(this.network, this.routeAlgoPtFreeflow, entry.getValue(), this.configGroup.getBeelineDistanceFactor(), this.routeFactory));
		}
	}

	/**
	 * Creates a rerouting strategy module using dijkstra rerouting.  Does the following (as far as I can see):<ul>
	 * <li> sets routeAlgo to the path calculator defined by <tt>new DijkstraFactory()</tt>, using <tt>costCalculator</tt> and <tt>timeCalculator</tt> as arguments </li>
	 * <li> sets routeAlgoPtFreeflow to "-1 utils/sec" (which is <it>enormous</it>--????) </li>
	 * <li> sets configGroup to <tt>group</tt> but it is not clear where this will be used.
	 * </ul>
	 */
	public PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final TravelDisutility costCalculator, final PersonalizableTravelTime timeCalculator, ModeRouteFactory routeFactory) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory(), routeFactory);
	}

	public final LeastCostPathCalculator getLeastCostPathCalculator(){
		return this.routeAlgo;
	}

	public final LeastCostPathCalculator getPtFreeflowLeastCostPathCalculator(){
		return this.routeAlgoPtFreeflow;
	}

	public final void addLegHandler(String transportMode, LegRouter legHandler) {
		data.addLegHandler(transportMode, legHandler);
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			handlePlan(person, plan);
		}
	}

	@Override
	public void run(final Plan plan) {
		handlePlan(plan.getPerson(), plan);
	}

	protected void handlePlan(Person person, final Plan plan) {
		if (timeCalculator != null) {
			timeCalculator.setPerson(person);
		}
		data.routePlan(person, plan);
	}

	/**
	 * @param person
	 * @param leg the leg to calculate the route for.
	 * @param fromAct the Act the leg starts
	 * @param toAct the Act the leg ends
	 * @param depTime the time (seconds from midnight) the leg starts
	 * @return the estimated travel time for this leg
	 */
	public double handleLeg(Person person, final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		return data.handleLeg(person, leg, fromAct, toAct, depTime);
	}

	public ModeRouteFactory getRouteFactory() {
		return routeFactory;
	}

}
