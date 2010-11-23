package playground.mzilske.pipeline;


public class RouterInvertedNetTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new RouterInvertedNetTaskManager();
	}

}
