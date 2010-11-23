package playground.mzilske.pipeline;

public class ScenarioMultiSinkSourceManager extends TaskManager {

	private ScenarioMultiSinkSource task;
	
	public ScenarioMultiSinkSourceManager(ScenarioMultiSinkSource task) {
		this.task = task;
	}
	
	@Override
	public void connect(PipeTasks pipeTasks) {
		for (int i=0; i < task.getSinkCount(); i++) {
			ScenarioSource source = pipeTasks.getScenarioSource();
			source.setSink(task.getSink(i));
		}
		pipeTasks.setScenarioSource(task);
	}

}
