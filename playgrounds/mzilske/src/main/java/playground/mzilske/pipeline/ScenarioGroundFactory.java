package playground.mzilske.pipeline;


public class ScenarioGroundFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new ScenarioSinkManager(new ScenarioGroundTask());
	}

}
