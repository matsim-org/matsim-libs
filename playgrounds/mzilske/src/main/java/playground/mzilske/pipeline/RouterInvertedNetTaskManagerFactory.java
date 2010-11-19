package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class RouterInvertedNetTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		return new RouterInvertedNetTaskManager();
	}

}
