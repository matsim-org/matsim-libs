package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

public class DijkstraTask implements ScenarioSinkSourceLeastCostPathCalculator {

	private DijkstraFactory dijkstraFactory;
	private ScenarioSink sink;
	
	public DijkstraTask(DijkstraFactory dijkstraFactory) {
		super();
		this.dijkstraFactory = dijkstraFactory;
	}

	@Override
	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		return dijkstraFactory;
	}

	@Override
	public void initialize(Scenario scenario) {
		sink.initialize(scenario);
	}

	@Override
	public void process(Scenario scenario) {
		this.sink.process(scenario);
	}

	@Override
	public void setSink(ScenarioSink sink) {
		this.sink = sink;
	}
	
}
