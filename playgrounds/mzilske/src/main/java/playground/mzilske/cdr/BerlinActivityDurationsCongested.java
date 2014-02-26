package playground.mzilske.cdr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;


public class BerlinActivityDurationsCongested {

	final static int TIME_BIN_SIZE = 60*60;
	final static int MAX_TIME = 30 * TIME_BIN_SIZE - 1;


	public static void main(String[] args) throws FileNotFoundException {

		Scenario baseScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(baseScenario).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-berlin/ITERS/it.200/2kW.15.200.experienced_plans.xml.gz");
		new MatsimNetworkReader(baseScenario).readFile(BerlinRun.BERLIN_PATH + "network/bb_4.xml.gz");
//		{
//			File file = new File("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/durations-simulated.txt");
//			PowerPlans.writeActivityDurations(baseScenario, file);
//		}
//		{
//			File file = new File("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/permutations.txt");
//			PowerPlans.writePermutations(baseScenario, file);
//		}
		{
			PowerPlans.sumDistancesAndPutInBasePopulation(baseScenario, baseScenario, "base");

			EventsManager events = EventsUtils.createEventsManager();
			VolumesAnalyzer baseVolumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
			events.addHandler(baseVolumes);
			new MatsimEventsReader(events).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-berlin/ITERS/it.200/2kW.15.200.events.xml.gz");
			{
				File file = new File("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/distances.txt");
				PrintWriter pw = new PrintWriter(file);

				pw.printf("callrate\troutesum\tvolumesum\tvolumesumdiff\n");
				for (int dailyRate : BerlinPhone.CALLRATES) {
					distances(pw, dailyRate, baseVolumes, baseScenario);
				}
				sumDistancesAndPutInBasePopulation(baseScenario, "infinite");
				pw.close();
			}



			PrintWriter pw = new PrintWriter(new File("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/person-kilometers2.txt"));
			pw.printf("person\tkilometers-base\t");
			for (int dailyRate : BerlinPhone.CALLRATES) {
				pw.printf("kilometers-%d\t", dailyRate);
			}
			pw.printf("kilometers-%s\t", "infinite");
			pw.printf("\n");
			for (Person person : baseScenario.getPopulation().getPersons().values()) {
				pw.printf("%s\t", person.getId().toString());
				pw.printf("%f\t", person.getCustomAttributes().get("kilometers-base"));
				for (int dailyRate : BerlinPhone.CALLRATES) {
					Double km = (Double) person.getCustomAttributes().get("kilometers-"+dailyRate);
					if (km == null) {
						// person not seen even once
						km = 0.0;
					}
					pw.printf("%f\t", km);
				}
				Double km = (Double) person.getCustomAttributes().get("kilometers-"+"infinite");
				if (km == null) {
					// person not seen even once
					km = 0.0;
				}
				pw.printf("%f\t", km);

				pw.printf("\n");
			}
			pw.close();
		}
	}


	private static void distances(PrintWriter pw, int callrate, VolumesAnalyzer baseVolumes, Scenario baseScenario) {
		String suffix = Integer.toString(callrate);
		
		double km = sumDistancesAndPutInBasePopulation(baseScenario, suffix);


		EventsManager events = EventsUtils.createEventsManager();
		VolumesAnalyzer volumes = new VolumesAnalyzer(TIME_BIN_SIZE, MAX_TIME, baseScenario.getNetwork());
		events.addHandler(volumes);
		new MatsimEventsReader(events).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-"+suffix+"/ITERS/it.20/20.events.xml.gz");


		double baseSum = PowerPlans.drivenKilometersWholeDay(baseScenario, baseVolumes);
		double sum = PowerPlans.drivenKilometersWholeDay(baseScenario, volumes);



		pw.printf("%d\t%f\t%f\t%f\n", callrate, km, sum, baseSum - sum);
		pw.flush();
	}


	private static double sumDistancesAndPutInBasePopulation(
			Scenario baseScenario, String suffix) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimPopulationReader(scenario).readFile("/Users/michaelzilske/runs-svn/synthetic-cdr/ant2014/car-congested/output-"+suffix+"/ITERS/it.20/20.experienced_plans.xml.gz");

		double km = PowerPlans.sumDistancesAndPutInBasePopulation(scenario, baseScenario, suffix);
		return km;
	}

}
