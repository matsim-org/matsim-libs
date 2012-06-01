package playground.pieter.mentalsim.controler.listeners;

import java.util.List;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;

import playground.pieter.mentalsim.replanning.ScoreResettingStrategyModule;

public class ScoreResetStrategyModuleAppender implements StartupListener {

	@Override
	public void notifyStartup(StartupEvent event) {
		StrategyManager stratMan = event.getControler().getStrategyManager();
		List<PlanStrategy> strategies = stratMan.getStrategies();
		for (PlanStrategy strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();

			if (strategyName.contains("selector") && strategyName.contains("_")
			// append the ScoreResettingStrategyModule
			) {
				strategy.addStrategyModule(new ScoreResettingStrategyModule());
			}
		}

	}

}
