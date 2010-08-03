package playground.christoph.withinday.replanning.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.multimodal.router.MultiModalPlansCalcRoute;

public class ReplanningModule extends AbstractMultithreadedModule {

	protected Config config;
	protected Network network;
	protected PersonalizableTravelCost costCalculator;
	protected TravelTime timeCalculator;
	protected LeastCostPathCalculatorFactory factory;
	
	public ReplanningModule(Config config, Network network, 
			PersonalizableTravelCost costCalculator, TravelTime timeCalculator, 
			LeastCostPathCalculatorFactory factory) {
		super(config.global());
		
		this.config = config;
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.factory = factory;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new MultiModalPlansCalcRoute(config.plansCalcRoute(), network, costCalculator, timeCalculator, factory);
	}

}
