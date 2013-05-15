package playground.pieter.pseudosim.controler.listeners;

import java.util.List;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.replanning.PseudoSimPlanMarkerModule;

public class PseudoSimPlanMarkerModuleAppender implements StartupListener {

	private PseudoSimControler controler;

	public PseudoSimPlanMarkerModuleAppender(PseudoSimControler c) {
		this.controler = c;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		StrategyManager stratMan = controler.getStrategyManager();
		List<PlanStrategy> strategies = stratMan.getStrategies();
		for (PlanStrategy strategy : strategies) {
			// first read off the weights of the strategies and classify them
			String strategyName = strategy.toString().toLowerCase();

			if (strategyName.contains("selector") && strategyName.contains("_")
			// append the ScoreResettingStrategyModule
			) {
				((PlanStrategyImpl) strategy).addStrategyModule(new PseudoSimPlanMarkerModule(controler));
			}
		}

	}

}
