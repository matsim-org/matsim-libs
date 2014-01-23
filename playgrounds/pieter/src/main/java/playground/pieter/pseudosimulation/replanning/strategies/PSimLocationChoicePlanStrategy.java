package playground.pieter.pseudosimulation.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.DestinationChoice;
import org.matsim.contrib.locationchoice.LocationChoicePlanStrategy;
import org.matsim.core.config.groups.LocationChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;
import playground.singapore.transitLocationChoice.TransitLocationChoiceStrategy;

public class PSimLocationChoicePlanStrategy implements PlanStrategy {

	private TransitLocationChoiceStrategy delegate;

	// private static int locachoiceWrnCnt;

	public PSimLocationChoicePlanStrategy(Scenario scenario, PSimControler controler) {
		delegate = new TransitLocationChoiceStrategy(scenario);
		delegate.addStrategyModule(new PSimPlanMarkerModule(controler));
	}

	@Override
	public void run(HasPlansAndId<Plan> person) {
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
