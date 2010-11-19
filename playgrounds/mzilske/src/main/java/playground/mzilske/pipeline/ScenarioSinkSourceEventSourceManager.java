package playground.mzilske.pipeline;

public class ScenarioSinkSourceEventSourceManager extends TaskManager {

	private ScenarioSinkSourceEventSource task;

	public ScenarioSinkSourceEventSourceManager(ScenarioSinkSourceEventSource task) {
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		connectEventsManager(pipeTasks, task);
	}

}
