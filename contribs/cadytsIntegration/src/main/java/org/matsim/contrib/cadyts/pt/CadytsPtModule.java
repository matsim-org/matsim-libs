package org.matsim.contrib.cadyts.pt;


import org.matsim.core.controler.AbstractModule;

public class CadytsPtModule extends AbstractModule {
	@Override
	public void install() {
		bind(CadytsPtContext.class).asEagerSingleton();
		addControlerListenerBinding().to(CadytsPtContext.class);
		bind(CadytsPtOccupancyAnalyzerI.class).to(CadytsPtOccupancyAnalyzer.class);
	}
}
