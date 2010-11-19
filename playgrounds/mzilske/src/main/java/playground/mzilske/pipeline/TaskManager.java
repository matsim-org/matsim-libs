package playground.mzilske.pipeline;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

public abstract class TaskManager {
	
	public abstract void connect(PipeTasks pipeTasks);

	public final void connectScenarioSinkSource(PipeTasks pipeTasks, ScenarioSinkSource task) {
		connectScenarioSink(pipeTasks, task);
		connectScenarioSource(pipeTasks, task);
	}

	public final void connectScenarioSource(PipeTasks pipeTasks, ScenarioSource scenarioSource) {
		pipeTasks.setScenarioSource(scenarioSource);
	}

	public final void connectScenarioSink(PipeTasks pipeTasks, ScenarioSinkSource task) {
		ScenarioSource source = pipeTasks.getScenarioSource();
		source.setSink(task);
	}
	
	public final void connectEventHandler(PipeTasks pipeTasks, EventHandler task) {
		EventsManager eventsManager = pipeTasks.getEventsManager();
		eventsManager.addHandler(task);
	}
	
	public final void connectEventsManager(PipeTasks pipeTasks, EventSource task) {
		EventsManager eventsManager = pipeTasks.getEventsManager();
		task.setEventsManager(eventsManager);
	}
	
	public void execute() {
		
	}

}
