package playground.mzilske.pipeline;


public class ScenarioInSwitchTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new ScenarioMultiSinkSourceManager(new ScenarioInSwitchTask(2));
	}

}
