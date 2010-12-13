package playground.mzilske.pipeline;


public class PersonPrepareForSimTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		PersonPrepareForSimTask task = new PersonPrepareForSimTask();
		return new ScenarioSinkSourceManager(task);
	}

}
