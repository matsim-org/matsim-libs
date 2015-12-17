package org.matsim.analysis;


import org.matsim.core.controler.AbstractModule;

public class TravelDistanceStatsModule extends AbstractModule {

	@Override
	public void install() {
		bind(TravelDistanceStats.class).asEagerSingleton();
		addControlerListenerBinding().to(TravelDistanceStatsControlerListener.class);
	}

}
