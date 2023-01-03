package playground.vsp.buildingEnergy.linkOccupancy;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEquil {

	public static void main(String[] args) {
		String configFile = "examples/equil/config.xml";
		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setLastIteration(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);



		// People leave home at 6:00 in Equil, so this is when this counter listens
		final LinkActivityOccupancyCounter atNight = new LinkActivityOccupancyCounter(scenario.getPopulation(), 0, 6*60*60);
		controler.getEvents().addHandler(atNight);

		// This counter looks at a time interval where everyone is travelling, so occupancy should be 0 everywhere
		final LinkActivityOccupancyCounter duringMorningCommute = new LinkActivityOccupancyCounter(scenario.getPopulation(), 6*60*60 + 1, 6*60*60 + 10);
		controler.getEvents().addHandler(duringMorningCommute);

		// People work only 30 minutes in Equil, so the interval for work-time is 6:01 to 6:30
		final LinkActivityOccupancyCounter atWork = new LinkActivityOccupancyCounter(scenario.getPopulation(), 6*60*60 + 1, 6*60*60 + 30 * 60);
		controler.getEvents().addHandler(atWork);
		
		// This one briefly listens in when everyone is already at home again
		final LinkActivityOccupancyCounter nextNight = new LinkActivityOccupancyCounter(scenario.getPopulation(), 13*60*60 + 45 * 60, 13*60*60 + 47 * 60);
		controler.getEvents().addHandler(nextNight);

		// During this window, most people are already home, a few are still working, and one person is actually seen both at work and at home.
		final LinkActivityOccupancyCounter middle = new LinkActivityOccupancyCounter(scenario.getPopulation(), 27810, 31440);
		controler.getEvents().addHandler(middle);
		
		// This one listens all day
		final LinkActivityOccupancyCounter allDay = new LinkActivityOccupancyCounter(scenario.getPopulation());
		controler.getEvents().addHandler(allDay);


		controler.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				System.out.println("Night maximum link occupancy ---");
				atNight.finish();
				atNight.dump();
				System.out.println("Morning commute link occupancy ---");
				duringMorningCommute.finish();
				duringMorningCommute.dump();
				System.out.println("Work-time maximum link occupancy ---");
				atWork.finish();
				atWork.dump();
				System.out.println("Next night maximum link occupancy ---");
				nextNight.finish();
				nextNight.dump();
				System.out.println("Middle of afternoon commute link occupancy ---");
				middle.finish();
				middle.dump();
				System.out.println("All-day maximum link occupancy ---");
				allDay.finish();
				allDay.dump();
				System.out.println("---");
			}
		});
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.run();
	}

}
