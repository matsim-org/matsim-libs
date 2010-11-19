package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class ScenarioLoaderTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		RunnableScenarioSource scenarioSource = new ScenarioLoaderTask(config);
		return new RunnableScenarioSourceManager(scenarioSource);
	}

}
