package playground.vsp.pipeline;


public class ProgressTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new ScenarioSinkSourceManager(new ProgressTask());
	}

}
