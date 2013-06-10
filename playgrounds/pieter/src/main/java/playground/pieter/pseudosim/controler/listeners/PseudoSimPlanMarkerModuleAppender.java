package playground.pieter.pseudosim.controler.listeners;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.LocationChoicePlanStrategy;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scenario.ScenarioUtils;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.replanning.modules.PSimPlanMarkerModule;

public class PseudoSimPlanMarkerModuleAppender implements StartupListener {

	private PseudoSimControler controler;

	public PseudoSimPlanMarkerModuleAppender(PseudoSimControler c) {
		this.controler = c;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		StrategyManager stratMan = controler.getStrategyManager();
		List<PlanStrategy> strategies = stratMan.getStrategies();
		PlanStrategyRegistrar psr = new PlanStrategyRegistrar();
		String[] exclusiveStrategyModules = controler.getConfig()
				.getParam("PseudoSim", "nonMutatingStrategies").split(",");
		ArrayList<String> exclusiveStrategies = new ArrayList<String>();
		Scenario dummyScenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		EventsManager dummyEvents = new DummyEventManager();
		for (String strat : exclusiveStrategyModules) {
			PlanStrategy exclusiveStrategy = psr
					.getFactoryRegister()
					.getInstance(
							controler.getConfig().strategy().getParams()
									.get(strat.trim()))
					.createPlanStrategy(dummyScenario, dummyEvents);
			exclusiveStrategies.add(exclusiveStrategy.toString());
		}
		for (PlanStrategy strategy : strategies) {
			String strategyName = strategy.toString();
			if (!(strategyName.toLowerCase().contains("locationchoice") || exclusiveStrategies
					.contains(strategyName))) {
				((PlanStrategyImpl) strategy)
						.addStrategyModule(new PSimPlanMarkerModule(
								controler));

			}
		}

	}

}

class DummyEventManager implements EventsManager {

	@Override
	public void resetHandlers(int iteration) {
	}

	@Override
	public void resetCounter() {
	}

	@Override
	public void removeHandler(EventHandler handler) {
	}

	@Override
	public void processEvent(Event event) {
	}

	@Override
	public void printEventsCount() {
	}

	@Override
	public void printEventHandlers() {
	}

	@Override
	public void initProcessing() {
	}

	@Override
	public EventsFactory getFactory() {
		return null;
	}

	@Override
	public void finishProcessing() {
	}

	@Override
	public void clearHandlers() {
	}

	@Override
	public void afterSimStep(double time) {
	}

	@Override
	public void addHandler(EventHandler handler) {
	}
};