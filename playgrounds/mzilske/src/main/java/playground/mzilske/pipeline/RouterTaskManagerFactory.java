package playground.mzilske.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.router.util.DijkstraFactory;

public class RouterTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		TaskManager leastCostPathCalculatorFactory;
		Config config = taskConfiguration.getConfig();
		if (config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.Dijkstra)) {
			leastCostPathCalculatorFactory = new LeastCostPathCalculatorTaskManager(new DijkstraTask(new DijkstraFactory()));
		} else if (config.controler().getRoutingAlgorithmType().equals(RoutingAlgorithmType.AStarLandmarks)) {
			throw new RuntimeException("Had to disable that line, see source code. Sorry!");
			// have to disable the line below because I removed the AStarLandmarksFactory constructor without any argument
			// This constructor did not run any preprocess, effectively resulting in AStarLandmarksFactory
			// returning router-instances that performed like a simple Dijkstra -- pretty sure
			// that's not what people wanted when using the constructor.
			// also I didn't see how to fix your code... actually I think the code was already broken
			// as it would have generated a NullPointerException on execution.
			// Feel free to contact me regarding this. Marcel, 10dec2012
//			leastCostPathCalculatorFactory = new LeastCostPathCalculatorTaskManager(new AStarLandmarksTask(new AStarLandmarksFactory(), new FreespeedTravelTimeAndDisutility(config.planCalcScore())));
		} else {
			throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
		}
		return leastCostPathCalculatorFactory;
	}

}
