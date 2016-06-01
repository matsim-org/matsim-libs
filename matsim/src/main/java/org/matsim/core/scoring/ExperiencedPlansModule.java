package org.matsim.core.scoring;


import org.matsim.core.controler.AbstractModule;

public class ExperiencedPlansModule extends AbstractModule {
	@Override
	public void install() {
		install(new ExperiencedPlanElementsModule());
		bind(ExperiencedPlansService.class).to(ExperiencedPlansServiceImpl.class).asEagerSingleton();
	}
}
