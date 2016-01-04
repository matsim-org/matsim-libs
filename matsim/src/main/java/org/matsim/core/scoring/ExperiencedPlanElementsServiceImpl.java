package org.matsim.core.scoring;


import com.google.common.eventbus.EventBus;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;

import javax.inject.Inject;

class ExperiencedPlanElementsServiceImpl implements ExperiencedPlanElementsService {

	EventBus eventBus = new EventBus("experiencedPlanElements");

	@Inject
	ExperiencedPlanElementsServiceImpl(EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
		eventsToActivities.setActivityHandler(new EventsToActivities.ActivityHandler() {
			@Override
			public void handleActivity(Id<Person> agentId, Activity activity) {
				eventBus.post(new PersonExperiencedActivity(agentId, activity));
			}
		});
		eventsToLegs.setLegHandler(new EventsToLegs.LegHandler() {
			@Override
			public void handleLeg(Id<Person> agentId, Leg leg) {
				eventBus.post(new PersonExperiencedLeg(agentId, leg));
			}
		});
	}

	@Override
	public void register(Object subscriber) {
		eventBus.register(subscriber);
	}

	@Override
	public void unregister(Object subscriber) {
		eventBus.unregister(subscriber);
	}

}
