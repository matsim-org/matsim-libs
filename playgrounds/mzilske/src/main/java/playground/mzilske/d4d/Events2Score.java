package playground.mzilske.d4d;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class Events2Score {
	
	public static void main(String[] args) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		Scenario scenario = ScenarioUtils.createScenario(RunSimulation.createConfig("", 1.0));
		scenario.getConfig().planCalcScore().setWriteExperiencedPlans(true);
		new MatsimNetworkReader(scenario).readFile("/Users/zilske/d4d/output/network.xml");
		new MatsimPopulationReader(scenario).readFile("/Users/zilske/matsim-without-history/playgrounds/trunk/mzilske/output2freespeed/output_plans.xml.gz");
		EventsToScore events2Score = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getConfig().scenario(), scenario.getNetwork()));
		eventsManager.addHandler(events2Score);
		new MatsimEventsReader(eventsManager).readFile("/Users/zilske/matsim-without-history/playgrounds/trunk/mzilske/output2freespeed/ITERS/it.180/180.events.xml.gz");
		// new MatsimEventsReader(eventsManager).readFile("/Users/zilske/matsim-without-history/playgrounds/trunk/mzilske/output2freespeed/ITERS/it.180/my_guy.xml");
		events2Score.finish();
		events2Score.writeExperiencedPlans("/Users/zilske/matsim-without-history/playgrounds/trunk/mzilske/output2freespeed/scored-plans");
	}

}
