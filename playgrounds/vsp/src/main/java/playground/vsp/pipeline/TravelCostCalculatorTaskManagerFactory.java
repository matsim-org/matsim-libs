package playground.vsp.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

public class TravelCostCalculatorTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		Config config = taskConfiguration.getConfig();
		TravelDisutilityFactory factory = new TravelTimeAndDistanceBasedTravelDisutilityFactory();
		TravelCostCalculatorTask task = new TravelCostCalculatorTask(factory, config.planCalcScore());
		TravelCostCalculatorTaskManager travelCostCalculatorTaskManager = new TravelCostCalculatorTaskManager(task);
		return travelCostCalculatorTaskManager;
	}

}
