package playground.mzilske.cdr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class Potsdam {

	public static void main(String[] args) throws FileNotFoundException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("input/potsdam/network.xml");
		new MatsimPopulationReader(scenario).readFile("output-homogeneous-37/ITERS/it.0/0.plans.xml.gz");
		PrintWriter pw = new PrintWriter(new File("output/quality-over-callrate.txt"));
		for (int dailyRate=0; dailyRate<160; dailyRate+=20) {
			run(scenario, dailyRate, pw);
		}
		pw.close();
	}

	private static void run(Scenario scenario, final double dailyRate, PrintWriter pw) {
		EventsManager events = EventsUtils.createEventsManager();
		CompareMain compareMain = new CompareMain(scenario, events, new CallBehavior() {

			@Override
			public boolean makeACall(ActivityEndEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(ActivityStartEvent event) {
				return false;
			}

			@Override
			public boolean makeACall(Id id, double time) {			
				double secondlyProbability = dailyRate / (double) (24*60*60);
				return Math.random() < secondlyProbability;
			}
			
		});
		new MatsimEventsReader(events).readFile("output-homogeneous-37/ITERS/it.0/0.events.xml.gz");
		compareMain.runOnceWithSimplePlans();
		pw.printf("%f\t%f\t%f\t%f\n", dailyRate, compareMain.compareAllDay(), compareMain.compareTimebins(), compareMain.compareEMDMassPerLink());
	}

}
