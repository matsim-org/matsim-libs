package playground.mzilske.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;

public class TravelTimeCalculatorTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		TravelTimeCalculatorTaskManager taskManager = new TravelTimeCalculatorTaskManager(travelTimeCalculatorFactory, config.travelTimeCalculator());
		return taskManager;
	}
	
}
