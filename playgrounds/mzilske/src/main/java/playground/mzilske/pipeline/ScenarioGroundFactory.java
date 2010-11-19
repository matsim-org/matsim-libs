package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class ScenarioGroundFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		return new ScenarioSinkManager(new ScenarioGroundTask());
	}

}
