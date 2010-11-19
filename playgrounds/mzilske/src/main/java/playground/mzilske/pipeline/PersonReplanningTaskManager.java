package playground.mzilske.pipeline;

import org.apache.log4j.Logger;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

public class PersonReplanningTaskManager extends TaskManager {
	
	private static Logger log = Logger.getLogger(PersonReplanningTaskManager.class);
	
	private PersonReplanningTask task;
	
	public PersonReplanningTaskManager(PersonReplanningTask task) {
		this.task = task;
	}

	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		TravelTimeCalculatorTask travelTimeCalc = pipeTasks.getTravelTimeCalculator();
		TravelCostCalculatorTask travelCostCalc = pipeTasks.getTravelCostCalculator();
		LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = pipeTasks.getLeastCostPathCalculatorFactory();
		task.setTravelTimeCalculator(travelTimeCalc);
		task.setTravelCostCalculator(travelCostCalc);
		task.setLeastCostPathCalculatorFactory(leastCostPathCalculatorFactory);
	}

}
