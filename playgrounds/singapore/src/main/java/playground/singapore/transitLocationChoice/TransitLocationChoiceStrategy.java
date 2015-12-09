package playground.singapore.transitLocationChoice;

import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Inject;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

public class TransitLocationChoiceStrategy implements PlanStrategy {
	
	private PlanStrategyImpl delegate;
	
	@Inject
	public TransitLocationChoiceStrategy(final Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		String planSelector = ((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).getPlanSelector();
		if (planSelector.equals("BestScore")) {
			delegate = new PlanStrategyImpl(new BestPlanSelector());
		} else if (planSelector.equals("ChangeExpBeta")) {
			delegate = new PlanStrategyImpl(new ExpBetaPlanChanger(scenario.getConfig().planCalcScore().getBrainExpBeta()));
		} else if (planSelector.equals("SelectRandom")) {
			delegate = new PlanStrategyImpl(new RandomPlanSelector());
		} else {
			delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(scenario.getConfig().planCalcScore()));
		}
		addStrategyModule(new TransitActsRemoverStrategy(scenario.getConfig()));
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Network net = NetworkImpl.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		((DestinationChoiceConfigGroup)scenario.getConfig().getModule("locationchoice")).setAlgorithm(DestinationChoiceConfigGroup.Algotype.localSearchRecursive);
		addStrategyModule(new DestinationChoice(tripRouterProvider, scenario));
		addStrategyModule(new TimeAllocationMutator(scenario.getConfig(), tripRouterProvider));
		addStrategyModule(new ReRoute(scenario, tripRouterProvider));
	}
	
	public void addStrategyModule(PlanStrategyModule module) {
		delegate.addStrategyModule(module);
	}

	public int getNumberOfStrategyModules() {
		return delegate.getNumberOfStrategyModules();
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		delegate.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		delegate.init(replanningContext);
	}

	@Override
	public void finish() {
		delegate.finish();
	}


}
