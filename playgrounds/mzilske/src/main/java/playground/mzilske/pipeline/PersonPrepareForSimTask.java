package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.ParallelPersonAlgorithmRunner;
import org.matsim.population.algorithms.PersonPrepareForSim;

public class PersonPrepareForSimTask implements ScenarioSinkSource {

	private static final int NUMBER_OF_THREADS = 0;
	
	private ScenarioSink sink;
	
	private LeastCostPathCalculatorFactory routerFactory;
	
	private PersonalizableTravelCost travelCosts;
	
	private PersonalizableTravelTime travelTimes;

	@Override
	public void initialize(Scenario scenario) {
		this.sink.initialize(scenario);
	}

	@Override
	public void process(final Scenario scenario) {
		ParallelPersonAlgorithmRunner.run(scenario.getPopulation(), NUMBER_OF_THREADS,
				new ParallelPersonAlgorithmRunner.PersonAlgorithmProvider() {


			public AbstractPersonAlgorithm getPersonAlgorithm() {
				return new PersonPrepareForSim(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), (NetworkImpl) scenario.getNetwork(), travelCosts, travelTimes, routerFactory), (NetworkImpl) scenario.getNetwork());
			}
		});
		sink.process(scenario);
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	public void setLeastCostPathCalculatorFactory(LeastCostPathCalculatorFactory routerFactory) {
		this.routerFactory = routerFactory;
	}
	
}
