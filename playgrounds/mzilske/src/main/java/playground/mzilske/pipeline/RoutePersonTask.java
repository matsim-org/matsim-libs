package playground.mzilske.pipeline;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.PersonPrepareForSim;

public class RoutePersonTask implements PersonSinkSource {

	private PersonPrepareForSim personPrepareForSim;
	private PersonSink sink;

	@Override
	public void setSink(PersonSink sink) {
		this.sink = sink;
	}

	@Override
	public void process(Person person) {
		personPrepareForSim.run(person);
		sink.process(person);
	}

	public RoutePersonTask(Config config, Network network) {
		super();
		PersonalizableTravelTime travelTimes = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(network, config.travelTimeCalculator());
		PersonalizableTravelCost travelCosts = new TravelCostCalculatorFactoryImpl().createTravelCostCalculator(travelTimes, config.planCalcScore());
		personPrepareForSim = new PersonPrepareForSim(new PlansCalcRoute(config.plansCalcRoute(), network, travelCosts, travelTimes, new DijkstraFactory()), (NetworkImpl) network);
	}

	@Override
	public void complete() {
		sink.complete();
	}
	
}
