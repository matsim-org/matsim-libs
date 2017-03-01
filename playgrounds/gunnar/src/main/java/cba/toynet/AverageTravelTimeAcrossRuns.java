package cba.toynet;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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

import floetteroed.utilities.Units;
import floetteroed.utilities.math.BasicStatistics;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class AverageTravelTimeAcrossRuns implements TravelTime {

	private final LinkedList<TravelTime> myTravelTimes = new LinkedList<>();

	private final int memory;

	private final String logFileName;

	AverageTravelTimeAcrossRuns(final int memory, final String logFileName) {
		this.memory = memory;
		this.logFileName = logFileName;
		try {
			final PrintWriter writer = new PrintWriter(this.logFileName);
			writer.print("");
			writer.flush();
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		try {
			Files.write(Paths.get(this.logFileName), this.linkStatistics(scenario).getBytes(),
					StandardOpenOption.APPEND);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String linkStatistics(final Scenario scenario) {
		final StringBuffer result = new StringBuffer();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			final BasicStatistics stats = new BasicStatistics();
			for (int time = 0; time < Units.S_PER_D; time += 5 * Units.S_PER_MIN) {
				stats.add(this.getLinkTravelTime(link, time, null, null));
			}
			result.append(link.getId() + "\t" + "avg=" + stats.getAvg() + ", min=" + stats.getMin() + ", max="
					+ stats.getMax() + "\n");
		}
		result.append("\n");
		return result.toString();
	}

	double FACT = 1.0;

	@Override
	public synchronized double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		double sum = 0;
		for (int i = 0; i < this.myTravelTimes.size(); i++) {
			sum += FACT * this.myTravelTimes.get(i).getLinkTravelTime(link, time, person, vehicle);
		}
		return (sum / this.myTravelTimes.size());
	}
}
