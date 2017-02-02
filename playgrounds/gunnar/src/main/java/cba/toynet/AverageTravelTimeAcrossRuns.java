package cba.toynet;

import java.util.LinkedList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class AverageTravelTimeAcrossRuns implements TravelTime {

	private final LinkedList<TravelTime> myTravelTimes = new LinkedList<>();

	private final int memory;

	AverageTravelTimeAcrossRuns(final int memory) {
		this.memory = memory;
	}

	void addData(final Scenario scenario) {
		final int it = scenario.getConfig().controler().getLastIteration();
		final EventsManager events = EventsUtils.createEventsManager();
		final TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(scenario.getNetwork(),
				(TravelTimeCalculatorConfigGroup) scenario.getConfig().getModule("travelTimeCalculator"));
		events.addHandler(travelTimeCalculator);
		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("./output/cba/toynet/output/ITERS/it." + it + "/" + it + ".events.xml.gz");
		this.myTravelTimes.addLast(travelTimeCalculator.getLinkTravelTimes());
		while (this.myTravelTimes.size() > this.memory) {
			this.myTravelTimes.removeFirst();
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double sum = 0;
		for (int i = 0; i < this.myTravelTimes.size(); i++) {
			sum += this.myTravelTimes.get(i).getLinkTravelTime(link, time, person, vehicle);
		}
		return (sum / this.myTravelTimes.size());
	}
}
