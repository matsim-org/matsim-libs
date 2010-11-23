package playground.mzilske.pipeline;

import org.matsim.core.config.Config;


public class ScenarioLoaderTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		Config config = taskConfiguration.getConfig();
		RunnableScenarioSource scenarioSource = new ScenarioLoaderTask(config);
		return new RunnableScenarioSourceManager(scenarioSource);
	}

}
