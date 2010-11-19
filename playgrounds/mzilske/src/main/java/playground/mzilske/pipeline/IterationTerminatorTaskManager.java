package playground.mzilske.pipeline;

public class IterationTerminatorTaskManager extends TaskManager {

	@Override
	public void connect(PipeTasks pipeTasks) {
		ScenarioSource scenarioSource = pipeTasks.getScenarioSource();
		IteratorTask iterator = pipeTasks.getIterator();
		ScenarioSink loopback = iterator.getIterationLoopSink();
		scenarioSource.setSink(loopback);
		pipeTasks.setScenarioSource(iterator);
	}

}
