package playground.mzilske.pipeline;

import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorInvertedNetProxyFactory;

public class RouterInvertedNetTaskManager extends TaskManager {

	@Override
	public void connect(PipeTasks pipeTasks) {
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = pipeTasks.getLeastCostPathCalculatorFactory();
		leastCostPathCalculatorFactory = new LeastCostPathCalculatorInvertedNetProxyFactory(leastCostPathCalculatorFactory);
		pipeTasks.setLeastCostPathCalculatorFactory(leastCostPathCalculatorFactory);
	}

}
