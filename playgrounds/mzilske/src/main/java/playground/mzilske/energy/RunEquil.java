package playground.mzilske.energy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEquil {

	public static void main(String[] args) {
		String configFile = "/Users/zilske/matsim-without-history/matsim/trunk/examples/equil/config.xml";
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// People leave home around 6:00 in Equil, so this is when this counter listens
		final LinkActivityOccupancyCounter atNight = new LinkActivityOccupancyCounter(scenario.getPopulation(), 0, 6*60*60);
		controler.getEvents().addHandler(atNight);
		
		// People work only 30 minutes in Equil, so the interval for work-time is 6:01 to 6:30
		final LinkActivityOccupancyCounter atWork = new LinkActivityOccupancyCounter(scenario.getPopulation(), 6*60*60 + 1, 6*60*60 + 30 * 60);
		controler.getEvents().addHandler(atWork);
		
		// This one listens all day
		final LinkActivityOccupancyCounter allDay = new LinkActivityOccupancyCounter(scenario.getPopulation());
		controler.getEvents().addHandler(allDay);
		
		controler.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				System.out.println("All-day maximum link occupancy ---");
				allDay.finish();
				allDay.dump();
				System.out.println("Night maximum link occupancy ---");
				atNight.finish();
				atNight.dump();
				System.out.println("Work-time maximum link occupancy ---");
				atWork.finish();
				atWork.dump();
				System.out.println("---");
			}
		});
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
}
