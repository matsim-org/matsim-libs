package playground.mzilske.pipeline;

public class RunnableScenarioSourceManager extends TaskManager {

	private RunnableScenarioSource task;

	public RunnableScenarioSourceManager(RunnableScenarioSource task) {
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		pipeTasks.setScenarioSource(task);
	}

	@Override
	public void execute() {
		task.run();
	}

}
