package playground.mzilske.cdranalysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.cdr.BerlinPhone;
import playground.mzilske.cdr.PowerPlans;

public class MultiRateRunResource {

	private String WD;

	private String regime;

	public MultiRateRunResource(String wd, String regime) {
		this.WD = wd;
		this.regime = regime;
	}

	final static int TIME_BIN_SIZE = 60*60;
	final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;
	
	public Collection<String> getRates() {
		final Set<String> RATES = new HashSet<String>();
		for (int dailyRate : BerlinPhone.CALLRATES) {
			RATES.add(Integer.toString(dailyRate));
		}
		RATES.add("actevents");
		RATES.add("contbaseplans");
		return RATES;
	}
	
	private void distances() throws FileNotFoundException {
		File file = new File(WD + "/distances.txt");
		Scenario baseScenario = getExperiencedBasePlans();
		
		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
		events.addHandler(baseVolumes);
		new MatsimEventsReader(events).readFile(WD + "/output-berlin/ITERS/it.200/2kW.15.200.events.xml.gz");

		PrintWriter pw = new PrintWriter(file);

		pw.printf("callrate\troutesum\tvolumesum\tvolumesumdiff\n");
		for (String rate : getRates()) {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimPopulationReader(scenario).readFile(WD + "/rates/" + rate + "/ITERS/it.20/20.experienced_plans.xml.gz");
			final Map<Id, Double> distancePerPerson1 = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork());
			final Map<Id, Double> distancePerPerson = distancePerPerson1;
			double km = 0.0;
			for (double distance : distancePerPerson.values()) {
				km += distance;
			}

			EventsManager events1 = EventsUtils.createEventsManager();
			VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
			events1.addHandler(volumes);
			new MatsimEventsReader(events1).readFile(WD + "/rates/" + rate + "/ITERS/it.20/20.events.xml.gz");

			double baseSum = PowerPlans.drivenKilometersWholeDay(baseScenario, baseVolumes);
			double sum = PowerPlans.drivenKilometersWholeDay(baseScenario, volumes);

			pw.printf("%s\t%f\t%f\t%f\n", rate, km, sum, baseSum - sum);
			pw.flush();
		}
		pw.close();
	}

	private void personKilometers() throws FileNotFoundException {
		final File file = new File(WD + "/person-kilometers.txt");
		Scenario baseScenario = getExperiencedBasePlans();
		
		PrintWriter pw = new PrintWriter(file);
		final Map<Id, Double> distancePerPersonBase = PowerPlans.travelledDistancePerPerson(baseScenario.getPopulation(), baseScenario.getNetwork());
		pw.printf("person\tkilometers-base\tvariable\tvalue\tregime\n");
		for (String rate : getRates()) {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			new MatsimPopulationReader(scenario).readFile(WD + "/rates/" + rate + "/ITERS/it.20/20.experienced_plans.xml.gz");
			final Map<Id, Double> distancePerPerson = PowerPlans.travelledDistancePerPerson(scenario.getPopulation(), baseScenario.getNetwork());
			for (Person person : baseScenario.getPopulation().getPersons().values()) {
				pw.printf("%s\t%f\t%s\t%f\t%s\n", 
						person.getId().toString(), 
						zeroForNull(distancePerPersonBase.get(person.getId())),
						rate,
						zeroForNull(distancePerPerson.get(person.getId())),
						regime);
			}
		}
		pw.close();
	}

	private Scenario getExperiencedBasePlans() {
		Scenario baseScenario = new RunResource(WD + "/output-berlin").getIteration(200).getExperiencedPlansAndNetwork();
		return baseScenario;
	}

	private void permutations() throws FileNotFoundException {
		File file = new File(WD + "/permutations.txt");
		Scenario baseScenario = getExperiencedBasePlans();
		PowerPlans.writePermutations(baseScenario, file);
	}

	private void durationsSimulated() throws FileNotFoundException {
		File file = new File(WD + "/durations-simulated.txt");
		Scenario baseScenario = getExperiencedBasePlans();
		PowerPlans.writeActivityDurations(baseScenario, file);
	}

	private static Double zeroForNull(Double maybeDouble) {
		if (maybeDouble == null) {
			return 0.0;
		}
		return maybeDouble;
	}

}
