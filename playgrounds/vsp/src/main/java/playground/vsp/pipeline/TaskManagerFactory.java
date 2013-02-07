package playground.vsp.pipeline;


public abstract class TaskManagerFactory {
	
	public abstract TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration);

}
