package playground.mzilske.pipeline;

public class ScenarioSinkMultiSourceManager extends TaskManager {

	private ScenarioSinkMultiSource task;
	
	public ScenarioSinkMultiSourceManager(ScenarioSinkMultiSource task) {
		super();
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		ScenarioSource source = pipeTasks.getScenarioSource();
		source.setSink(task);
		for (int i=0; i < task.getSourceCount(); i++) {
			pipeTasks.setScenarioSource(task.getSource(i));
		}
	}

}
