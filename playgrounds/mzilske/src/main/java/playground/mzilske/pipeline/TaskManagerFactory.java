package playground.mzilske.pipeline;


public abstract class TaskManagerFactory {
	
	protected abstract TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration);

}
