package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;

public class TravelTimeCalculatorTaskManager extends TaskManager {

	private TravelTimeCalculatorTask task;

	public TravelTimeCalculatorTaskManager(TravelTimeCalculatorFactory travelTimeCalculatorFactory, TravelTimeCalculatorConfigGroup group) {
		this.task = new TravelTimeCalculatorTask(group, travelTimeCalculatorFactory);
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		EventsManager eventsManager = pipeTasks.getEventsManager();
		eventsManager.addHandler(task);
		pipeTasks.setTravelTimeCalculator(task);
	}
	
}
