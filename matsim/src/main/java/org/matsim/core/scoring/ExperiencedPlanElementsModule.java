package org.matsim.core.scoring;

import org.matsim.core.controler.AbstractModule;

public final class ExperiencedPlanElementsModule extends AbstractModule {
	@Override
	public void install() {
		bind(EventsToActivities.class).asEagerSingleton();
		bind(EventsToLegs.class).asEagerSingleton();
		bind(EventsToLegsAndActivities.class).asEagerSingleton();
		addEventHandlerBinding().to(EventsToLegsAndActivities.class);
	}
}
