package playground.mzilske.outcome;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;

public class DumpOutcome {
	
	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.TRACE);
		Config config = ConfigUtils.loadConfig("input/config.xml");
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario).readFile("input/network.xml");
		new MatsimPopulationReader(scenario).readFile("output/ITERS/it.100/run0.100.plans.xml.gz");
		// new MatsimPopulationReader(scenario).readFile("input/plans.xml");
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToScore eventsToScore = new EventsToScore(scenario, new CharyparNagelScoringFunctionFactory(scenario.getConfig().planCalcScore(), scenario.getNetwork()));
		eventsManager.addHandler(eventsToScore);
		new MatsimEventsReader(eventsManager).readFile("output/ITERS/it.100/run0.100.events.xml.gz");
		eventsToScore.finish();
		System.out.println(eventsToScore.getAgentScore(new IdImpl("BRB_P_0")));
		System.out.println("Average:");
		double avg = 0.0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			avg += person.getSelectedPlan().getScore();
		}
		System.out.println(avg / scenario.getPopulation().getPersons().size());
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double cap = ((LinkImpl) link).getFlowCapacity() * config.qsim().getFlowCapFactor();
		}
		
	}

}
