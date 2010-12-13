package playground.mzilske.pipeline;


public class RouterInvertedNetTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new RouterInvertedNetTaskManager();
	}

}
