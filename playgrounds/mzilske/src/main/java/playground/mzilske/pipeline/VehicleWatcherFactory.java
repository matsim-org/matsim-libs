package playground.mzilske.pipeline;

import org.matsim.core.events.handler.EventHandler;

import playground.mzilske.deteval.VehicleWatchingEventHandler;

public class VehicleWatcherFactory extends TaskManagerFactory {

	@Override
	protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		EventHandler eventHandler = new VehicleWatchingEventHandler();
		return new EventHandlerTaskManager(eventHandler);
	}

}
