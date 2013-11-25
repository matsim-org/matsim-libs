package playground.sergioo.eventAnalysisTools2012;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

public class EventsToPlans implements ActivityHandler, LegHandler {

	private final Map<Id,  Plan> agentRecords = new TreeMap<Id,Plan>();
	
	private final Scenario scenario;
	
	public EventsToPlans(Scenario scenario) {
		this.scenario = scenario;
		for (Person person : scenario.getPopulation().getPersons().values())
			this.agentRecords.put(person.getId(), new PlanImpl());
	}
	@Override
	public void handleLeg(Id agentId, Leg leg) {
		if(agentRecords.get(agentId)!=null)
			agentRecords.get(agentId).addLeg(leg);
	}

	@Override
	public void handleActivity(Id agentId, Activity activity) {
		if(agentRecords.get(agentId)!=null)
			agentRecords.get(agentId).addActivity(activity);
	}
	
	public void writeExperiencedPlans(String iterationFilename) {
		PopulationImpl population = new PopulationImpl((ScenarioImpl) scenario);
		for (Entry<Id, Plan> entry : agentRecords.entrySet()) {
			Person person = new PersonImpl(entry.getKey());
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
		new MatsimNetworkReader(scenario).readFile(args[0]);
		new MatsimPopulationReader(scenario).readFile(args[1]);
		EventsToPlans eventsToPlans = new EventsToPlans(scenario);
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventsToActivities eventsToActivities = new EventsToActivities();
		eventsToActivities.setActivityHandler(eventsToPlans);
		EventsToLegs eventsToLegs = new EventsToLegs(scenario);
		eventsToLegs.setLegHandler(eventsToPlans);
		eventsManager.addHandler(eventsToActivities);
		eventsManager.addHandler(eventsToLegs);
		new EventsReaderXMLv1(eventsManager).parse(args[2]);
		eventsToPlans.writeExperiencedPlans(args[3]);
	}

}
