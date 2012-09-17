/* *********************************************************************** *
 * project: org.matsim.*
 * PlanRouterAdapter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.router.old;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.ActivityWrapperFacility;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Adapts a PlanRouter to the PlansCalcRoute interface.
 * Note that it is difficult to guarantee full backward compatibility,
 * as the legs are not modified "in place" anymore.
 * @author thibautd
 * @deprecated this is just a way to keep old code running,
 * and should by no means be used in new code.
 */
@Deprecated
public class PlanRouterAdapter implements PlanAlgorithm, PersonAlgorithm {
	private final LeastCostPathCalculator routeAlgo;
	private final LeastCostPathCalculator routeAlgoPtFreeFlow;
	private final Network network;
	private final ModeRouteFactory routeFactory;
	private final PopulationFactory populationFactory;
	private final PlanRouter planRouter;

	public PlanRouterAdapter(
			final PlanRouter planRouter,
			final LeastCostPathCalculator routeAlgo,
			final LeastCostPathCalculator routeAlgoPtFreeFlow,
			final Network network,
			final ModeRouteFactory routeFactory,
			final PopulationFactory populationFactory) {
		this.routeAlgo = routeAlgo;
		this.routeAlgoPtFreeFlow = routeAlgoPtFreeFlow;
		this.network = network;
		this.routeFactory = routeFactory;
		this.populationFactory = populationFactory;
		this.planRouter = planRouter;
	}

	public PlanRouterAdapter(
			final Controler controler) {
		this( new PlanRouter(
					controler.getTripRouterFactory().createTripRouter(),
					controler.getScenario().getActivityFacilities() ),
				controler);
	}

	public PlanRouterAdapter(
			final PlanRouter planRouter,
			final Controler controler) {
		this.planRouter = planRouter;

		TravelTime time = controler.getTravelTimeCalculator();
		TravelDisutility disutility =
			controler.getTravelDisutilityFactory().createTravelDisutility(
					time,
					controler.getConfig().planCalcScore());
		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
			new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);

		LeastCostPathCalculatorFactory factory = controler.getLeastCostPathCalculatorFactory();
		this.network = controler.getNetwork();
		this.routeAlgo = factory.createPathCalculator(network, disutility, time);
		this.routeAlgoPtFreeFlow = factory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);
		this.populationFactory = controler.getPopulation().getFactory();
		this.routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
	}

	public PlanRouterAdapter(
			final PlanRouter planRouter,
			final Network network,
			final PopulationFactory populationFactory,
			final TravelTime time,
			final TravelDisutility disutility,
			final LeastCostPathCalculatorFactory factory) {
		this.planRouter = planRouter;

		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
			new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);

		this.network = network;
		this.routeAlgo = factory.createPathCalculator(network, disutility, time);
		this.routeAlgoPtFreeFlow = factory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);
		this.populationFactory = populationFactory;
		this.routeFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();
	}


	public final LeastCostPathCalculator getLeastCostPathCalculator(){
		return this.routeAlgo;
	}

	public final LeastCostPathCalculator getPtFreeflowLeastCostPathCalculator(){
		return this.routeAlgoPtFreeFlow;
	}

	public final void addLegHandler(
			final String transportMode,
			final LegRouter legHandler) {
		planRouter.getTripRouter().setRoutingModule(
				transportMode,
				new LegRouterWrapper(
					transportMode,
					populationFactory,
					legHandler));
	}

	@Override
	public void run(final Person person) {
		for (Plan plan : person.getPlans()) {
			run( plan);
		}
	}

	@Override
	public void run(final Plan plan) {
		planRouter.run( plan );
	}

	/**
	 * @param person
	 * @param leg the leg to calculate the route for.
	 * @param fromAct the Act the leg starts
	 * @param toAct the Act the leg ends
	 * @param depTime the time (seconds from midnight) the leg starts
	 * @return the estimated travel time for this leg
	 */
	public double handleLeg(
			final Person person,
			final Leg leg,
			final Activity fromAct,
			final Activity toAct,
			final double depTime) {
		List<? extends PlanElement> trip =
			planRouter.getTripRouter().calcRoute(
					leg.getMode(),
					new ActivityWrapperFacility( fromAct ),
					new ActivityWrapperFacility( toAct ),
					depTime,
					person);

		if ( trip.size() != 1 ) {
			throw new IllegalStateException( getClass()+".handleLeg() can only be used with "+
					"routing modules returning single legs. Got the following trip "+
					"for mode "+leg.getMode()+": "+trip );
		}

		Leg tripLeg = (Leg) trip.get( 0 );
		leg.setRoute( tripLeg.getRoute() );
		leg.setTravelTime( tripLeg.getTravelTime() );
		leg.setDepartureTime( tripLeg.getDepartureTime() );
		
		return tripLeg.getRoute() != null &&
			tripLeg.getRoute().getTravelTime() != Time.UNDEFINED_TIME ?
				tripLeg.getRoute().getTravelTime() :
				tripLeg.getTravelTime();
	}

	public ModeRouteFactory getRouteFactory() {
		return routeFactory;
	}

}

