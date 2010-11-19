package playground.mzilske.pipeline;

import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;


public class TravelCostCalculatorTaskManager extends TaskManager {

	private TravelCostCalculatorTask task;

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		TravelTimeCalculatorTask travelTimeCalculator = pipeTasks.getTravelTimeCalculator();
		task.setTravelTimeCalculator(travelTimeCalculator);
		pipeTasks.setTravelCostCalculator(task);
	}

	public TravelCostCalculatorTaskManager(TravelCostCalculatorFactory travelCostCalculatorFactory, CharyparNagelScoringConfigGroup group) {
		super();
	}

	public TravelCostCalculatorTaskManager(TravelCostCalculatorTask task) {
		this.task = task;
	}
	
}
