package playground.mzilske.pipeline;

import org.matsim.core.router.util.DijkstraFactory;

public class DijkstraTaskManager extends TaskManager {

	@Override
	public void connect(PipeTasks pipeTasks) {
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		pipeTasks.setLeastCostPathCalculatorFactory(dijkstraFactory);
	}

}
