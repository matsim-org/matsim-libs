package playground.mzilske.pipeline;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;

public class TravelCostCalculatorTask implements ScenarioSinkSource {

	private ScenarioSink sink;
	
	private PlanCalcScoreConfigGroup group;

	private TravelTimeCalculatorTask travelTimeCalculator;

	private TravelDisutilityFactory travelCostCalculatorFactory;

	private PersonalizableTravelDisutility travelCostCalculator;
	
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
		travelCostCalculator = travelCostCalculatorFactory.createTravelDisutility(travelTimeCalculator.getTravelTimeCalculator(), group);	
		sink.process(scenario);
	}

	public TravelCostCalculatorTask(TravelDisutilityFactory travelCostCalculatorFactory, PlanCalcScoreConfigGroup group) {
		super();
		this.travelCostCalculatorFactory = travelCostCalculatorFactory;
		this.group = group;
	}

	PersonalizableTravelDisutility getTravelCostCalculator() {
		return travelCostCalculator;
	}

	void setTravelTimeCalculator(TravelTimeCalculatorTask travelTimeCalculator) {
		this.travelTimeCalculator = travelTimeCalculator;
	}

}
