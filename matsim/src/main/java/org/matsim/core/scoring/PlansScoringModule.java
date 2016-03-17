package org.matsim.core.scoring;


import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.PlansScoring;

public class PlansScoringModule extends AbstractModule {
	@Override
	public void install() {
		bind(ScoringFunctionsForPopulation.class).asEagerSingleton();
		bind(PlansScoring.class).to(PlansScoringImpl.class);
	}
}
