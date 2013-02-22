package playground.pieter.mentalsim.controler.listeners;

import java.util.List;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;

import playground.pieter.mentalsim.controler.MentalSimControler;
import playground.pieter.mentalsim.replanning.MentalSimPlanMarkerModule;

public class MentalSimPlanMarkerModuleAppender implements StartupListener {

	private MentalSimControler controler;

	public MentalSimPlanMarkerModuleAppender(MentalSimControler c) {
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
				((PlanStrategyImpl) strategy).addStrategyModule(new MentalSimPlanMarkerModule(controler));
			}
		}

	}

}
