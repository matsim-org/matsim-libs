package playground.mzilske.logevents;

import playground.mzilske.pipeline.EventHandlerTaskManager;
import playground.mzilske.pipeline.TaskConfiguration;
import playground.mzilske.pipeline.TaskManager;
import playground.mzilske.pipeline.TaskManagerFactory;


public class LogOutputEventHandlerTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		return new EventHandlerTaskManager(new LogOutputEventHandler());
	}

}
