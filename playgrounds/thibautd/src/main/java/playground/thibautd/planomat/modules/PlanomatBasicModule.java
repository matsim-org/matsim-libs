/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatBasicStrategy.java
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
package playground.thibautd.planomat.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory;
import playground.thibautd.planomat.basic.PermissiveWhiteList;
import playground.thibautd.planomat.basic.PlanomatFitnessFunctionFactoryImpl;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;
import playground.thibautd.planomat.Planomat;

/**
 * Module using planomat v2, in a setting which makes it similar
 * to v1.
 *
 * @author thibautd
 */
public class PlanomatBasicModule extends AbstractMultithreadedModule {
	private final PersonalizableTravelCost travelCost;
	private final PersonalizableTravelTime travelTime;
	private final Controler controler;
	private final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final Planomat2ConfigGroup configGroup;
	
	public PlanomatBasicModule(
			Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler;

		this.travelCost = controler.createTravelCostCalculator();
		this.travelTime = controler.getTravelTimeCalculator();

		this.configGroup = (Planomat2ConfigGroup)
			controler.getConfig().getModule( Planomat2ConfigGroup.GROUP_NAME );

		DepartureDelayAverageCalculator tDepDelayCalc =
			new DepartureDelayAverageCalculator(
				controler.getNetwork(),
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());

		controler.getEvents().addHandler( tDepDelayCalc );

		this.legTravelTimeEstimatorFactory =
			new LegTravelTimeEstimatorFactory(
					travelTime,
					tDepDelayCalc);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlansCalcRoute routingAlgorithm = (PlansCalcRoute)
			controler.createRoutingAlgorithm(
					travelCost,
					travelTime);

		ActivityWhiteList whiteList = new PermissiveWhiteList();

		PlanomatFitnessFunctionFactory fitnessFunctionFactory =
			new PlanomatFitnessFunctionFactoryImpl(
					controler.getScoringFunctionFactory(),
					configGroup,
					routingAlgorithm,
					controler.getNetwork(),
					legTravelTimeEstimatorFactory);
		
		return new Planomat( fitnessFunctionFactory , whiteList , configGroup );
	}
}

