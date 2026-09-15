package org.matsim.codeexamples.scoring.pseudoRandomErrors.replanning;

import org.matsim.core.controler.AbstractModule;

public class RandomModeModule extends AbstractModule {
	@Override
	public void install() {
		addPlanStrategyBinding("RandomMode").toProvider(RandomModeProvider.class);
	}
}
