package org.matsim.core.controler;

import org.matsim.analysis.IterationStopWatch;

public final class NewControlerModule extends AbstractModule {
	@Override
	public void install() {
		bind(ControlerI.class).to(NewControler.class).asEagerSingleton();
		bind(ControlerListenerManagerImpl.class).asEagerSingleton();
		bind(ControlerListenerManager.class).to(ControlerListenerManagerImpl.class);
		
		bind(IterationStopWatch.class).asEagerSingleton();
		bind(OutputDirectoryHierarchy.class).asEagerSingleton();
		bind(TerminationCriterion.class).to(TerminateAtFixedIterationNumber.class);
		bind(MatsimServices.class).to(MatsimServicesImpl.class);

		bind(IterationCounter.class).to(MatsimServicesImpl.class);
		// (I don't want to always inject the whole MatsimServices just to get the iteration number.  If
		// someone has a better idea, please let me know. kai, aug'18)

		bind(PrepareForSim.class).to(PrepareForSimImpl.class);
		bind(PrepareForMobsim.class).to(PrepareForMobsimImpl.class);
	}
}
