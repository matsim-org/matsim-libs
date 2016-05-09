package org.matsim.core.scoring;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;

import java.util.HashMap;
import java.util.Map;

class ExperiencedPlansServiceImpl implements ExperiencedPlansService, EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {

	private final static Logger log = Logger.getLogger(ExperiencedPlansServiceImpl.class);

	@Inject private Config config;
	@Inject private Population population;
	@Inject(optional = true) private ScoringFunctionsForPopulation scoringFunctionsForPopulation;

	private final Map<Id<Person>, Plan> agentRecords = new HashMap<>();

	@Inject
	ExperiencedPlansServiceImpl(ControlerListenerManager controlerListenerManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
		controlerListenerManager.addControlerListener(new IterationStartsListener() {
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				for (Person person : population.getPersons().values()) {
					agentRecords.put(person.getId(), new PlanImpl());
				}
			}
		});
		eventsToActivities.addActivityHandler(this);
		eventsToLegs.addLegHandler(this);
	}

	synchronized public void handleLeg(PersonExperiencedLeg o) {
		Id<Person> agentId = o.getAgentId();
		Leg leg = o.getLeg();
		Plan plan = agentRecords.get(agentId);
		if (plan != null) {
			plan.addLeg(leg);
		}
	}

	synchronized public void handleActivity(PersonExperiencedActivity o) {
		Id<Person> agentId = o.getAgentId();
		Activity activity = o.getActivity();
		Plan plan = agentRecords.get(agentId);
		if (plan != null) {
			agentRecords.get(agentId).addActivity(activity);
		}
	}

	@Override
	public void writeExperiencedPlans(String iterationFilename) {
		Population tmpPop = PopulationUtils.createPopulation(config);
		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Person person = PopulationUtils.createPerson(entry.getKey());
			Plan plan = entry.getValue();
			if (scoringFunctionsForPopulation != null) {
				plan.setScore(scoringFunctionsForPopulation.getScoringFunctionForAgent(person.getId()).getScore());
				if (plan.getScore().isNaN()) {
					log.warn("score is NaN; plan:" + plan.toString());
				}
			}
			person.addPlan(plan);
			tmpPop.addPerson(person);
		}
		new PopulationWriter(tmpPop, null).write(iterationFilename);
		// I removed the "V5" here in the assumption that it is better to move along with future format changes.  If this is
		// undesired, please change back but could you then please also add a comment why you prefer this.  Thanks.
		// kai, jan'16
	}

	@Override
	public Map<Id<Person>, Plan> getAgentRecords() {
		return this.agentRecords;
	}

}
