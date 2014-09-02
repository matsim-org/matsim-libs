package playground.sergioo.replaceExpBeta2013;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.ExpBetaPlanSelector;

public class ReplaceExpBetaPlanStrategy implements PlanStrategy {

	private PlanStrategyImpl delegate;

	public ReplaceExpBetaPlanStrategy(Scenario scenario) {
		delegate = new PlanStrategyImpl(new ExpBetaPlanSelector(scenario.getConfig().planCalcScore().getBrainExpBeta()));
		delegate.addStrategyModule(new DoNothingMutator(scenario.getConfig()));
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
