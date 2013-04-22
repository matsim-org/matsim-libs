package org.matsim.roadpricing;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class AreaTollControler extends Controler {

	public AreaTollControler(Config config) {
		super(config);
		this.addControlerListener(new RoadPricing());
		throw new RuntimeException("This will very most likely not work since overwriting createRoutingAlgorithm() " +
				"affects only the initialization (prepareForSim) but not the routing _during_ the iterations.  Aborting since " +
				"this is most probably not what anybody wants.  kai, apr'13") ;
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm() {
		TravelDisutility travelCosts = this.createTravelCostCalculator();
		TravelTime travelTimes = this.getLinkTravelTimes();
		ModeRouteFactory routeFactory = ((PopulationFactoryImpl) (this.population.getFactory())).getModeRouteFactory();

		return new PlansCalcAreaTollRoute(this.config.plansCalcRoute(), this.network, travelCosts,
				travelTimes, this.getLeastCostPathCalculatorFactory(), routeFactory, (RoadPricingSchemeImpl) this.scenarioData.getScenarioElement(RoadPricingScheme.class), config);
	}
	
}
