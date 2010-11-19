package playground.mzilske.pipeline;

import org.matsim.core.config.Config;

public class PersonPrepareForSimTaskManagerFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(Config config) {
		PersonPrepareForSimTask task = new PersonPrepareForSimTask();
		return new ScenarioSinkSourceManager(task);
	}

}
