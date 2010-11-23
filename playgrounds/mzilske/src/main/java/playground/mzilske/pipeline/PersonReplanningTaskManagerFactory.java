package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class PersonReplanningTaskManagerFactory extends TaskManagerFactory {
	
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		Config config = taskConfiguration.getConfig();
		PersonReplanningTaskManager personReplanningTaskManager = new PersonReplanningTaskManager(new PersonReplanningTask(config));
		return personReplanningTaskManager;
	}

}
