package playground.mzilske.pipeline;

public class ScenarioSinkManager extends TaskManager {

	private ScenarioSink task;
	
	public ScenarioSinkManager(ScenarioSink task) {
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		ScenarioSource source = pipeTasks.getScenarioSource();
		source.setSink(task);
	}

}
