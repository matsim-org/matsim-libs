package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class LogOutputEventHandlerTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		return new EventHandlerTaskManager(new LogOutputEventHandler());
	}

}
