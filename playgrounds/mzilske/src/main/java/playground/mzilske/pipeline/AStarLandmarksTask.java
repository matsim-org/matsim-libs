package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelMinCost;

public class AStarLandmarksTask implements ScenarioSinkSourceLeastCostPathCalculator {

	private AStarLandmarksFactory factory;
	
	private ScenarioSink sink;

	public AStarLandmarksTask(AStarLandmarksFactory factory,
			TravelMinCost travelMinCost) {
		super();
		this.factory = factory;
		this.travelMinCost = travelMinCost;
	}

	private TravelMinCost travelMinCost;

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}

	@Override
	public void initialize(Scenario scenario) {
		factory.processNetwork(scenario.getNetwork(), travelMinCost);
		sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		sink.process(scenario);
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return factory;
	}

}
