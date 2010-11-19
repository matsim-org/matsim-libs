package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class IteratorTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		return new IteratorTaskManager(new IteratorTask(config.controler().getFirstIteration(), config.controler().getLastIteration(), config.global().getRandomSeed()));
	}

}
