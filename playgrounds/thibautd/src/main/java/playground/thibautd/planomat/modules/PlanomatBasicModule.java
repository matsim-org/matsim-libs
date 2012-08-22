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

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.router.PlansCalcTransitRoute;

import playground.thibautd.planomat.Planomat;
import playground.thibautd.planomat.api.ActivityWhiteList;
import playground.thibautd.planomat.api.PlanomatFitnessFunctionFactory;
import playground.thibautd.planomat.basic.PermissiveWhiteList;
import playground.thibautd.planomat.basic.PlanomatBasicPtFitnessFunctionFactory;
import playground.thibautd.planomat.basic.PlanomatConfigurationFactoryImpl;
import playground.thibautd.planomat.basic.PlanomatFitnessFunctionFactoryImpl;
import playground.thibautd.planomat.config.Planomat2ConfigGroup;

/**
 * Module using planomat v2, in a setting which makes it similar
 * to v1.
 *
 * @author thibautd
 */
public class PlanomatBasicModule extends AbstractMultithreadedModule {
	private final TravelDisutility travelCost;
	private final TravelTime travelTime;
	private final Controler controler;
	private final LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory;
	private final Planomat2ConfigGroup configGroup;
	private final Config config;
	
	public PlanomatBasicModule(
			Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler;

		this.travelCost = controler.createTravelCostCalculator();
		this.travelTime = controler.getTravelTimeCalculator();

		this.config = controler.getConfig();
		this.configGroup = (Planomat2ConfigGroup)
			this.config.getModule( Planomat2ConfigGroup.GROUP_NAME );

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

		PlanomatFitnessFunctionFactory fitnessFunctionFactory = null;

		if (this.config.scenario().isUseTransit()) {
			fitnessFunctionFactory =
				new PlanomatBasicPtFitnessFunctionFactory(
						controler.getScoringFunctionFactory(),
						configGroup,
						(PlansCalcTransitRoute) routingAlgorithm,
						controler.getNetwork(),
						legTravelTimeEstimatorFactory);
		}
		else {
			fitnessFunctionFactory =
				new PlanomatFitnessFunctionFactoryImpl(
						controler.getScoringFunctionFactory(),
						configGroup,
						routingAlgorithm,
						controler.getNetwork(),
						legTravelTimeEstimatorFactory);
		}
		
		return new Planomat(
				fitnessFunctionFactory,
				new PlanomatConfigurationFactoryImpl( configGroup ),
				whiteList,
				MatsimRandom.getLocalInstance());
	}
}

