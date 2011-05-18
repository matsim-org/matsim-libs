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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.utils.misc.Time;
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

	private static final Logger log = Logger.getLogger(PlansCalcRoute.class);

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

	private final NetworkFactoryImpl routeFactory;

	protected final Network network;

	private final PersonalizableTravelCost costCalculator;

	private final PersonalizableTravelTime timeCalculator;

	private Map<String, LegRouter> legHandlers;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	/**Does the following (as far as I can see):<ul>
	 * <li> sets routeAlgo to the path calculator defined by <tt>factory</tt>, using <tt>costCalculator</tt> and <tt>timeCalculator</tt> as arguments </li>
	 * <li> sets routeAlgoPtFreeflow to "-1 utils/sec" (which is <it>enormous</it>--????) </li>
	 * <li> sets configGroup to <tt>group</tt> but it is not clear where this will be used.
	 * </ul>
	 * [[old javadoc: Uses the speed factors from the config group and the rerouting of the factory]]
	 */
	public PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network,
			final PersonalizableTravelCost costCalculator,
			final PersonalizableTravelTime timeCalculator, LeastCostPathCalculatorFactory factory){
		this.routeAlgo = factory.createPathCalculator(network, costCalculator, timeCalculator);
		FreespeedTravelTimeCost ptTimeCostCalc = new FreespeedTravelTimeCost(-1.0, 0.0, 0.0);
		this.routeAlgoPtFreeflow = factory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.routeFactory = (NetworkFactoryImpl) network.getFactory();
		if (group != null) {
			this.configGroup = group;
		}
		else {
			log.warn(NO_CONFIGGROUP_SET_WARNING);
		}

		initDefaultLegHandlers();
	}

	/**
	 * Creates a rerouting strategy module using dijkstra rerouting.  Does the following (as far as I can see):<ul>
	 * <li> sets routeAlgo to the path calculator defined by <tt>new DijkstraFactory()</tt>, using <tt>costCalculator</tt> and <tt>timeCalculator</tt> as arguments </li>
	 * <li> sets routeAlgoPtFreeflow to "-1 utils/sec" (which is <it>enormous</it>--????) </li>
	 * <li> sets configGroup to <tt>group</tt> but it is not clear where this will be used.
	 * </ul>
	 */
	public PlansCalcRoute(final PlansCalcRouteConfigGroup group, final Network network, final PersonalizableTravelCost costCalculator, final PersonalizableTravelTime timeCalculator) {
		this(group, network, costCalculator, timeCalculator, new DijkstraFactory());
	}

	public final LeastCostPathCalculator getLeastCostPathCalculator(){
		return this.routeAlgo;
	}

	public final LeastCostPathCalculator getPtFreeflowLeastCostPathCalculator(){
		return this.routeAlgoPtFreeflow;
	}

	private void initDefaultLegHandlers() {
		legHandlers = new HashMap<String, LegRouter>();
		this.addLegHandler(TransportMode.car, new NetworkLegRouter(this.network, this.routeAlgo, this.routeFactory));
		this.addLegHandler(TransportMode.ride, new NetworkLegRouter(this.network, this.routeAlgo, this.routeFactory));
		if (this.configGroup.getPtSpeedMode() == PlansCalcRouteConfigGroup.PtSpeedMode.freespeed) {
			this.addLegHandler(TransportMode.pt, new PseudoTransitLegRouter(this.network, this.routeAlgoPtFreeflow, this.configGroup.getPtSpeedFactor(), this.configGroup.getBeelineDistanceFactor(), this.routeFactory));
		} else if (this.configGroup.getPtSpeedMode() == PlansCalcRouteConfigGroup.PtSpeedMode.beeline) {
			this.addLegHandler(TransportMode.pt, new TeleportationLegRouter(this.routeFactory, this.configGroup.getPtSpeed(), this.configGroup.getBeelineDistanceFactor()));
		}
		this.addLegHandler(TransportMode.bike, new TeleportationLegRouter(this.routeFactory, this.configGroup.getBikeSpeed(), this.configGroup.getBeelineDistanceFactor()));
		this.addLegHandler(TransportMode.walk, new TeleportationLegRouter(this.routeFactory, this.configGroup.getWalkSpeed(), this.configGroup.getBeelineDistanceFactor()));
		this.addLegHandler("undefined", new TeleportationLegRouter(this.routeFactory, this.configGroup.getUndefinedModeSpeed(), this.configGroup.getBeelineDistanceFactor()));
	}

	public final void addLegHandler(String transportMode, LegRouter legHandler) {
		if (legHandlers.get(transportMode) != null) {
			log.warn("A LegHandler for " + transportMode + " legs is already registered - it is replaced!");
		}
		legHandlers.put(transportMode, legHandler);
	}
	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

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

	//////////////////////////////////////////////////////////////////////
	// helper methods
	//////////////////////////////////////////////////////////////////////

	protected void handlePlan(Person person, final Plan plan) {
		double now = 0;
		if (costCalculator != null) {
			costCalculator.setPerson(person);
		}
		if (timeCalculator != null) {
			timeCalculator.setPerson(person);
		}

		// loop over all <act>s
		Activity fromAct = null;
		Activity toAct = null;
		Leg leg = null;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Leg) {
				leg = (Leg) pe;
			} else if (pe instanceof Activity) {
				if (fromAct == null) {
					fromAct = (Activity) pe;
				} else {
					toAct = (ActivityImpl) pe;

					double endTime = fromAct.getEndTime();
					double startTime = fromAct.getStartTime();
					double dur = (fromAct instanceof ActivityImpl ? ((ActivityImpl) fromAct).getMaximumDuration() : Time.UNDEFINED_TIME);
					if (endTime != Time.UNDEFINED_TIME) {
						// use fromAct.endTime as time for routing
						now = endTime;
					} else if ((startTime != Time.UNDEFINED_TIME) && (dur != Time.UNDEFINED_TIME)) {
						// use fromAct.startTime + fromAct.duration as time for routing
						now = startTime + dur;
					} else if (dur != Time.UNDEFINED_TIME) {
						// use last used time + fromAct.duration as time for routing
						now += dur;
					} else {
						throw new RuntimeException("activity of plan of person " + plan.getPerson().getId().toString() + " has neither end-time nor duration." + fromAct.toString());
					}

					now += handleLeg(person, leg, fromAct, toAct, now);

					fromAct = toAct;
				}
			}
		}
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
		String legmode = leg.getMode();
		LegRouter legHandler = legHandlers.get(legmode);
		if (legHandler != null) {
			return legHandler.routeLeg(person, leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legmode + "'.");
		}
	}

	public NetworkFactoryImpl getRouteFactory() {
		return routeFactory;
	}

}