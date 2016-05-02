package gunnar.ihop2.integration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.math.Histogram;
import gunnar.ihop2.regent.costwriting.DepartureTimeHistogram;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TollLinkAnalyzer implements LinkEnterEventHandler {

	private static final String[] tollLinkIdStr = new String[] {

	"101350_NW", "112157_SE", "9216_N", "13354_W", "18170_SE", "35424_NW",
			"44566_NE", "51930_W", "53064_SE", "68760_SW", "74188_NW",
			"79555_NE", "90466_SE", "122017_NW", "122017_SE", "22626_W",
			"77626_W", "90317_W", "92866_E", "110210_E", "2453_SW", "6114_S",
			"6114_N", "25292_NE", "28480_S", "34743_NE", "124791_W",
			"71617_SW", "71617_SE", "80449_N", "96508_NW", "96508_SE",
			"108353_SE", "113763_NW", "121908_N", "121908_S", "52416_NE",
			"125425_N"

	};

	private final Vehicle2DriverEventHandler vehicle2DriverHandler;

	private Set<Id<Person>> tollPayers = new LinkedHashSet<>();

	private Set<Id<Person>> maxTollPayers = new LinkedHashSet<>();

	private final Map<String, Histogram> linkIdStr2entryHist;

	private final Histogram all;

	public TollLinkAnalyzer(
			final Vehicle2DriverEventHandler vehicle2DriverHandler) {
		this.vehicle2DriverHandler = vehicle2DriverHandler;
		all = Histogram.newHistogramWithUniformBins(0.0, 3600.0, 24);
		this.linkIdStr2entryHist = new LinkedHashMap<>();
		for (String linkIdStr : tollLinkIdStr) {
			this.linkIdStr2entryHist.put(linkIdStr,
					Histogram.newHistogramWithUniformBins(0.0, 3600.0, 24));
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		for (Map.Entry<String, Histogram> entry : linkIdStr2entryHist
				.entrySet()) {
			result.append(entry.getKey());
			for (int i = 1; i <= 24; i++) {
				result.append("\t");
				result.append(entry.getValue().cnt(i)); // bin 0 is before t=0.0
			}
			result.append("\n");
		}
		return result.toString();
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// System.out.println(event);
		final Histogram hist = this.linkIdStr2entryHist.get(event.getLinkId()
				.toString());
		if (hist != null) {
			this.all.add(event.getTime());
			hist.add(event.getTime());
			// this.tollPayers.add(event.getDriverId());
			this.tollPayers.add(this.vehicle2DriverHandler
					.getDriverOfVehicle(event.getVehicleId()));
			if ((event.getTime() >= 7.5 * 3600 && event.getTime() < 8.5 * 3600)
					|| (event.getTime() >= 16.0 * 3600 && event.getTime() < 17.5 * 3600)) {
				// this.maxTollPayers.add(event.getDriverId());
				this.maxTollPayers.add(this.vehicle2DriverHandler
						.getDriverOfVehicle(event.getVehicleId()));
			}
		}
	}

	// -------------------- MAIN-FUNCTION --------------------

	public static void main(String[] args) {

		final String path = "/Nobackup/Profilen/Documents/proposals/2015/IHOP2/showcase/";
		final String eventsFileName = path + "tla4_toll/1000.events.xml.gz";
		final String plansFileName = path + "tla4_toll/1000.plans.xml.gz";
		final String histFileName = "./ihop2-data/playground/tla4_toll.txt";

		Config config = ConfigUtils.createConfig();

		EventsManager events;
		MatsimEventsReader reader;
		Vehicle2DriverEventHandler vehicle2driverHandler = new Vehicle2DriverEventHandler(); // null
		final TollLinkAnalyzer tollLinkAnalyzer = new TollLinkAnalyzer(
				vehicle2driverHandler);
		events = EventsUtils.createEventsManager(config);
		events.addHandler(vehicle2driverHandler);
		events.addHandler(tollLinkAnalyzer);
		reader = new MatsimEventsReader(events);
		reader.readFile(eventsFileName);

		config.getModule("plans").addParam("inputPlansFile", plansFileName);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		DepartureTimeHistogram hist = new DepartureTimeHistogram(0, 3600, 24);
		for (Id<Person> personId : tollLinkAnalyzer.maxTollPayers) {
			hist.addPerson(scenario.getPopulation().getPersons().get(personId));
		}
		hist.writeHistogramsToFile(histFileName);

		System.out.println();
		System.out.println("ALL");
		System.out.println();
		PlansAnalyzer pa = new PlansAnalyzer();
		pa.process(scenario.getPopulation().getPersons().keySet(),
				scenario.getPopulation());
		System.out.println(pa.getReport());
		System.out.println();
		System.out.println();
		System.out.println("PEAK");
		System.out.println();
		pa = new PlansAnalyzer();
		pa.process(tollLinkAnalyzer.maxTollPayers, scenario.getPopulation());
		System.out.println(pa.getReport());
		System.out.println();
		System.out.println();

		System.out.print("TIME PROFILE");
		for (int i = 1; i <= 24; i++) {
			System.out.print("\t");
			// bin 0 is before t=0.0
			System.out.print(tollLinkAnalyzer.all.cnt(i));
		}
		System.out.println();
		System.out.println();
	}

}
