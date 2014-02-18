package playground.mzilske.cdr;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class Events2Plan {

	public static void main(String[] args) {
		for (int dailyRate : BerlinPhone.CALLRATES) {
			EventsManager eventsManager = EventsUtils.createEventsManager();
			Config config = ConfigUtils.createConfig();
			ActivityParams sightingParam = new ActivityParams("sighting");
			// sighting.setOpeningTime(0.0);
			// sighting.setClosingTime(0.0);
			sightingParam.setTypicalDuration(30.0 * 60);
			config.planCalcScore().addActivityParams(sightingParam);
			new MatsimConfigReader(config).parse(Events2Plan.class.getResourceAsStream("2kW.15.xml"));
			Scenario scenario = ScenarioUtils.createScenario(config);
			scenario.getConfig().planCalcScore().setWriteExperiencedPlans(true);
			new MatsimNetworkReader(scenario).readFile(BerlinRun.BERLIN_PATH + "network/bb_4.xml.gz");
			new MatsimPopulationReader(scenario).readFile("car-congested/output-"+dailyRate+"/output_plans.xml.gz");
			EventsToScore events2Score = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getNetwork()));
			eventsManager.addHandler(events2Score);
			new MatsimEventsReader(eventsManager).readFile("car-congested/output-"+dailyRate+"/ITERS/it.20/20.events.xml.gz");
			events2Score.finish();
			events2Score.writeExperiencedPlans("car-congested/output-"+dailyRate+"/ITERS/it.20/20.experienced_plans");

		}
	}

}
