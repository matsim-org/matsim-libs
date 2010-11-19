package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;

public class IteratorTaskManager extends TaskManager {

	private IteratorTask task;
	
	public IteratorTaskManager(IteratorTask task) {
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		EventsManager eventsManager = pipeTasks.getEventsManager();
		task.setEventsManager(eventsManager);
		connectScenarioSink(pipeTasks, task);
		connectScenarioSource(pipeTasks, task.getIterationLoopSource());
		pipeTasks.setIterator(task);
	}

}
