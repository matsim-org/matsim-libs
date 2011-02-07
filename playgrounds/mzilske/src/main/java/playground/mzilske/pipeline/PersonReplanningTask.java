package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonReplanningTask implements ScenarioSinkSource {
	
	private Config config;
	
	private TravelCostCalculatorTask travelCostCalc;
	
	private TravelTimeCalculatorTask travelTimeCalc;
	
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	
	int iteration = 0;

	private ScenarioSink sink;

	public PersonReplanningTask(Config config) {
		this.config = config;
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Scenario scenario) {
		this.sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		StrategyManager manager = new StrategyManager();
		manager.setMaxPlansPerAgent(config.strategy().getMaxAgentPlanMemorySize());
		Network network = scenario.getNetwork();
		for (StrategyConfigGroup.StrategySettings settings : config.strategy().getStrategySettings()) {
			double rate = settings.getProbability();
			if (rate == 0.0) {
				continue;
			}
			String classname = settings.getModuleName();

			PlanStrategyImpl strategy = loadStrategy(classname, settings, network, travelTimeCalc.getTravelTimeCalculator(), travelCostCalc.getTravelCostCalculator());

			if (strategy == null) {
				Gbl.errorMsg("Could not initialize strategy named " + classname);
			}

			if (settings.getDisableAfter() >= 0) {
				int maxIter = settings.getDisableAfter();
				if (maxIter <= iteration) {
					manager.addStrategy(strategy, rate);
				}
			} else {
				manager.addStrategy(strategy, rate);
			}
		}
		manager.run(scenario.getPopulation());
		sink.process(scenario);
	}
	
	private PlanStrategyImpl loadStrategy(final String name, final StrategyConfigGroup.StrategySettings settings, Network network, PersonalizableTravelTime travelTimeCalc, PersonalizableTravelCost travelCostCalc) {
		PlanStrategyImpl strategy = null;
		if (name.equals("KeepLastSelected")) {
			strategy = new PlanStrategyImpl(new KeepSelected());
		} else if (name.equals("ReRoute")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new ReRoute(network));
		} else if (name.equals("TimeAllocationMutator") || name.equals("threaded.TimeAllocationMutator")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			TimeAllocationMutator tam = new TimeAllocationMutator(config);
			strategy.addStrategyModule(tam);
		} else if (name.equals("BestScore")) {
			strategy = new PlanStrategyImpl(new BestPlanSelector());
		} else if (name.equals("SelectExpBeta")) {
			strategy = new PlanStrategyImpl(new ExpBetaPlanSelector(config.planCalcScore()));
		} else if (name.equals("ChangeExpBeta")) {
			strategy = new PlanStrategyImpl(new ExpBetaPlanChanger(config.planCalcScore().getBrainExpBeta()));
		} else if (name.equals("SelectRandom")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
		} else if (name.equals("ChangeLegMode")) {
			strategy = new PlanStrategyImpl(new RandomPlanSelector());
			strategy.addStrategyModule(new ChangeLegMode(config));
			strategy.addStrategyModule(new ReRouteDijkstra(config, network, travelCostCalc, travelTimeCalc));
		}
		return strategy;
	}

	public void setTravelTimeCalculator(TravelTimeCalculatorTask travelTimeCalc2) {
		this.travelTimeCalc = travelTimeCalc2;
	}

	public void setTravelCostCalculator(TravelCostCalculatorTask travelCostCalc2) {
		this.travelCostCalc = travelCostCalc2;
	}

	private class ReRoute extends AbstractMultithreadedModule {
	
		private Network network;
	
		public ReRoute(Network network) {
			super(1);
			this.network = network;
		}
	
		@Override
		public PlanAlgorithm getPlanAlgoInstance() {
			return new PlansCalcRoute(config.plansCalcRoute(), network, travelCostCalc.getTravelCostCalculator(), travelTimeCalc.getTravelTimeCalculator(), leastCostPathCalculatorFactory);
		}
	
	}

	void setLeastCostPathCalculatorFactory(
			LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}

}
