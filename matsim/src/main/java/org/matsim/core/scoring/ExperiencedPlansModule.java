package org.matsim.core.scoring;


import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import javax.inject.Inject;

public class ExperiencedPlansModule extends AbstractModule {
	@Override
	public void install() {
		install(new ExperiencedPlanElementsModule());
		bind(ScoringFunctionsForPopulation.class).asEagerSingleton();
		bind(ExperiencedPlansService.class).to(ScoringFunctionsForPopulation.class);
	}
}
