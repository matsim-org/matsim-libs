package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public abstract class TaskManagerFactory {
	
	protected abstract TaskManager createTaskManagerImpl(Config config);

}
