package playground.mzilske.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.MobsimFactory;

import playground.mrieser.core.mobsim.usecases.OptimizedCarSimFactory;

public class MobsimTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		MobsimFactory mobsimFactory = new OptimizedCarSimFactory(0);
		MobsimTask task = new MobsimTask(mobsimFactory);
		TaskManager taskManager = new ScenarioSinkSourceEventSourceManager(task);
		return taskManager;
	}

}
