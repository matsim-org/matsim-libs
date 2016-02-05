package org.matsim.core.scoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import javax.inject.Inject;

public class ExperiencedPlanElementsModule extends AbstractModule {
	@Override
	public void install() {
		bind(EventsToActivities.class).asEagerSingleton();
		addControlerListenerBinding().toInstance(new AfterMobsimListener() {
			@Inject
			EventsToActivities eventsToActivities;

			@Override
			public void notifyAfterMobsim(AfterMobsimEvent event) {
				eventsToActivities.finish();
			}
		});
		bind(EventsToLegs.class).asEagerSingleton();
	}
}
