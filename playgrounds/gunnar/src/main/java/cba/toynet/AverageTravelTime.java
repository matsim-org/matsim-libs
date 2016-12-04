package cba.toynet;

import java.util.ArrayList;
import java.util.List;

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
class AverageTravelTime implements TravelTime {

	private final List<TravelTime> myTravelTimes = new ArrayList<>();

	AverageTravelTime() {
	}

	AverageTravelTime(final Scenario scenario, final int avgIts) {
		this.addData(scenario, avgIts);
	}

	void addData(final Scenario scenario, final int avgIts) {

		this.myTravelTimes.clear();

		final int lastIt = scenario.getConfig().controler().getLastIteration();
		for (int it = lastIt - avgIts + 1; it <= lastIt; it++) {
			final EventsManager events = EventsUtils.createEventsManager();
			final TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(scenario.getNetwork(),
					(TravelTimeCalculatorConfigGroup) scenario.getConfig().getModule("travelTimeCalculator"));
			events.addHandler(travelTimeCalculator);
			final MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile("./output/cba/toynet/output/ITERS/it." + it + "/" + it + ".events.xml.gz");
			this.myTravelTimes.add(travelTimeCalculator.getLinkTravelTimes());
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		final int startIndex = this.myTravelTimes.size() / 2;
		double sum = 0;
		for (int i = startIndex; i < this.myTravelTimes.size(); i++) {
			sum += this.myTravelTimes.get(i).getLinkTravelTime(link, time, person, vehicle);
		}
		return (sum / (this.myTravelTimes.size() - startIndex));
	}
}
