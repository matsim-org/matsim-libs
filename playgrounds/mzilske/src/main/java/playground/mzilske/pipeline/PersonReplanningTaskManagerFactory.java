package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class PersonReplanningTaskManagerFactory {
	
	protected TaskManager createTaskManagerImpl(Config config) {
		PersonReplanningTaskManager personReplanningTaskManager = new PersonReplanningTaskManager(new PersonReplanningTask(config));
		return personReplanningTaskManager;
	}

}
