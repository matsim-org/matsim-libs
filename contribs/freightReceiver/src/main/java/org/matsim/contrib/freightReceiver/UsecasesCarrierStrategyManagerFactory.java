package org.matsim.contrib.freightReceiver;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.controler.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.controler.ReRouteVehicles;
import org.matsim.contrib.freight.controler.TimeAllocationMutator;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;

import static org.matsim.contrib.freight.usecases.chessboard.TravelDisutilities.createBaseDisutility;

public class UsecasesCarrierStrategyManagerFactory implements CarrierPlanStrategyManagerFactory{
	// I don't think that this has much of an effect.  I would, in fact, re-run jsprit after every iteration, in which case this here would have no effect at all.  kai, feb'19

	/*
	 * Adapted from RunChessboard.java by sschroeder and gliedtke.
	 */
	private final Network network;
	private final MatsimServices controler;
	private final CarrierVehicleTypes types;

	public UsecasesCarrierStrategyManagerFactory( final CarrierVehicleTypes types, final Network network, final MatsimServices controler ) {
		this.types = types;
		this.network = network;
		this.controler= controler;
	}

	@Override
	public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
		return getCarrierPlanCarrierGenericStrategyManager( types, controler, network );
	}

	static GenericStrategyManager<CarrierPlan, Carrier> getCarrierPlanCarrierGenericStrategyManager( CarrierVehicleTypes types, MatsimServices controler,
																	 Network network ){
		TravelDisutility travelDis = createBaseDisutility( types, controler.getLinkTravelTimes() );
		final LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator( network,	travelDis, controler.getLinkTravelTimes() );

		final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<>();
		strategyManager.setMaxPlansPerAgent(5);
		{
			GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new ExpBetaPlanChanger<>( 1. ));
			strategyManager.addStrategy(strategy, null, 1.0);
		}

		{
			GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>( new KeepSelected<>());
			strategy.addStrategyModule(new TimeAllocationMutator() );
			ReRouteVehicles reRouteModule = new ReRouteVehicles(router, network, controler.getLinkTravelTimes(), 1.);
			strategy.addStrategyModule(reRouteModule);
			strategyManager.addStrategy(strategy, null, 0.5);
		}
		return strategyManager;
	}
}
