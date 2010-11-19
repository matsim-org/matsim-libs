package playground.mzilske.pipeline;


public class LeastCostPathCalculatorTaskManager extends TaskManager {

	private ScenarioSinkSourceLeastCostPathCalculator task;

	public LeastCostPathCalculatorTaskManager(ScenarioSinkSourceLeastCostPathCalculator task) {
		this.task = task;
	}

	@Override
	public void connect(PipeTasks pipeTasks) {
		connectScenarioSinkSource(pipeTasks, task);
		pipeTasks.setLeastCostPathCalculatorFactory(task.getLeastCostPathCalculatorFactory());
	}

}
