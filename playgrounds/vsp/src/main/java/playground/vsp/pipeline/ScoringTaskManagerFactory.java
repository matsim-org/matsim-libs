package playground.vsp.pipeline;

import org.matsim.core.config.Config;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class ScoringTaskManagerFactory extends TaskManagerFactory {

	@Override
	public TaskManager createTaskManagerImpl(TaskConfiguration taskConfiguration) {
		Config config = taskConfiguration.getConfig();
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), null);
		return new ScoringTaskManager(scoringFunctionFactory, config.planCalcScore().getLearningRate());
	}

}
