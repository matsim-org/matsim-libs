package playground.mzilske.pipeline;


public class ScenarioInSwitchTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new ScenarioMultiSinkSourceManager(new ScenarioInSwitchTask(2));
	}

}
