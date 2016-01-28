package playground.sergioo.eventAnalysisTools2012;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.scoring.PersonExperiencedActivity;
import org.matsim.core.scoring.PersonExperiencedLeg;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class EventsToPlans implements ActivityHandler, LegHandler {

	private final Map<Id<Person>, Plan> agentRecords = new TreeMap<>();
	
	private final Scenario scenario;
	
	public EventsToPlans(Scenario scenario) {
		this.scenario = scenario;
		for (Person person : scenario.getPopulation().getPersons().values())
			this.agentRecords.put(person.getId(), new PlanImpl());
	}

	@Override
	public void handleLeg(PersonExperiencedLeg leg) {
		if(agentRecords.get(leg.getAgentId())!=null)
			agentRecords.get(leg.getAgentId()).addLeg(leg.getLeg());
	}

	@Override
	public void handleActivity(PersonExperiencedActivity activity) {
		if(agentRecords.get(activity.getAgentId())!=null)
			agentRecords.get(activity.getAgentId()).addActivity(activity.getActivity());
	}
	
	public void writeExperiencedPlans(String iterationFilename) {
        Population population = PopulationUtils.createPopulation(scenario.getConfig(), scenario.getNetwork());
		PopulationFactory factory = population.getFactory();
        for (Entry<Id<Person>, Plan> entry : agentRecords.entrySet()) {
			Person person = factory.createPerson(entry.getKey());
			Plan plan = entry.getValue();
			person.addPlan(plan);
			population.addPerson(person);
		}
		new PopulationWriter(population, scenario.getNetwork()).writeV5(iterationFilename);
	}
	
	/**
	 * @param args
	 * 0 - Network file
	 * 1 - Population input file
	 * 2 - Events input file
	 * 3 - Plans output file
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(args[0]);
		new MatsimPopulationReader(scenario).readFile(args[1]);
		EventsToPlans eventsToPlans = new EventsToPlans(scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToActivities eventsToActivities = new EventsToActivities();
		eventsToActivities.addActivityHandler(eventsToPlans);
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		eventsToLegs.addLegHandler(eventsToPlans);
		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);
		new EventsReaderXMLv1(eventsManager).parse(args[2]);
		eventsToPlans.writeExperiencedPlans(args[3]);
	}

}
