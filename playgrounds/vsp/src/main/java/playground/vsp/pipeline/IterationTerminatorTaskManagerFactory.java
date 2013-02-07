package playground.vsp.pipeline;


public class IterationTerminatorTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new IterationTerminatorTaskManager();
	}

}
