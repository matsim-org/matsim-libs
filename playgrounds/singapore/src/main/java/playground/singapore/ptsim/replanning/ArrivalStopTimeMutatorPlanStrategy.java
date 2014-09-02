package playground.singapore.ptsim.replanning;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ArrivalStopTimeMutatorPlanStrategy implements PlanStrategy {

	private static final String ORIGINAL_TIME = "originalTime";
	private static final String INPUT_FILE = "originalTimes.xml";
	private PlanStrategyImpl delegate;

	public ArrivalStopTimeMutatorPlanStrategy(Scenario scenario) {
		Map<Id, Double> times = new HashMap<Id, Double>();
		String folder = scenario.getConfig().plans().getInputFile();
		folder = folder.substring(0, folder.lastIndexOf("/"));
		ObjectAttributes agentAttributes = new ObjectAttributes();
		new ObjectAttributesXmlReader(agentAttributes).parse(folder+INPUT_FILE);
		for(Person person:scenario.getPopulation().getPersons().values())
			times.put(person.getId(), (Double)agentAttributes.getAttribute(person.getId().toString(), ORIGINAL_TIME));
		delegate = new PlanStrategyImpl(new RandomPlanSelector());
		delegate.addStrategyModule(new ArrivalTimeToStopMutator(scenario.getConfig(), times));
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
