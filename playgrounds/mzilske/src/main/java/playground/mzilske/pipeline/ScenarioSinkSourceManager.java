package playground.mzilske.pipeline;

public class ScenarioSinkSourceManager extends TaskManager {

	private ScenarioSinkSource task;

	public ScenarioSinkSourceManager(ScenarioSinkSource task) {
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
	}

}
