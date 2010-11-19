package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.scoring.ScoringFunctionFactory;

public class ScoringTaskManager extends TaskManager {

	private ScoringTask task;

	public ScoringTaskManager(ScoringFunctionFactory scoringFunctionFactory, double learningRate) {
		this.task = new ScoringTask(scoringFunctionFactory, learningRate);
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		EventsManager eventsManager = pipeTasks.getEventsManager();
		eventsManager.addHandler(task);
	}

}
