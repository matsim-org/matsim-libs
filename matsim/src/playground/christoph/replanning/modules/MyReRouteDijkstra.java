package playground.christoph.replanning.modules;

import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.KnowledgePlansCalcRoute;
import playground.christoph.router.util.MyDijkstraFactory;

/*
 * Basically we could also extend AbstractMultithreadedModule -
 * currently we don't use methods from ReRouteDijkstra but maybe
 * somewhere is checked if our Class is instanceof ReRouteDijkstra...
 */
public class MyReRouteDijkstra extends ReRouteDijkstra {

	TravelCost costCalculator = null;
	TravelTime timeCalculator = null;
	NetworkLayer network = null;
	
	private PlansCalcRouteConfigGroup configGroup = null;
	
	public MyReRouteDijkstra(PlansCalcRouteConfigGroup configGroup, final NetworkLayer network, final TravelCost costCalculator, final TravelTime timeCalculator) {
		super(configGroup, network, costCalculator, timeCalculator);
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.configGroup = configGroup;
	}
	
	/*
	 * If possible, we should probably clone the Cost- and TimeCalculator.
	 * Maybe MATSim runs MultiThreaded and will use more Threads to do the
	 * replanning.
	 */
	@Override
	public PlanAlgorithm getPlanAlgoInstance()
	{
		return new KnowledgePlansCalcRoute(this.configGroup, this.network, this.costCalculator, this.timeCalculator, new MyDijkstraFactory());
		//return new KnowledgePlansCalcRoute(this.configGroup, this.network, this.costCalculator, this.timeCalculator, new DijkstraFactory());
	}
}
