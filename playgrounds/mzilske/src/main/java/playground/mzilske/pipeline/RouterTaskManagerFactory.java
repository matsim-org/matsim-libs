package playground.mzilske.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;

public class RouterTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		TaskManager leastCostPathCalculatorFactory;
		if (config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.Dijkstra)) {
			leastCostPathCalculatorFactory = new LeastCostPathCalculatorTaskManager(new DijkstraTask(new DijkstraFactory()));
		} else if (config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.AStarLandmarks)) {
			leastCostPathCalculatorFactory = new LeastCostPathCalculatorTaskManager(new AStarLandmarksTask(new AStarLandmarksFactory(), new FreespeedTravelTimeCost(config.charyparNagelScoring())));
		} else {
			throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
		}
		return leastCostPathCalculatorFactory;
	}

}
