package org.matsim.core.scoring;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import javax.inject.Inject;

public class ExperiencedPlanElementsModule extends AbstractModule {
	@Override
	public void install() {
		bind(EventsToActivities.class).asEagerSingleton();
		bind(EventsToLegs.class).asEagerSingleton();
	}
}
