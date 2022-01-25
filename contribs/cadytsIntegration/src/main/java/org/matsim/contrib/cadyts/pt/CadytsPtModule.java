package org.matsim.contrib.cadyts.pt;


import org.matsim.core.controler.AbstractModule;

import javax.inject.Singleton;

public class CadytsPtModule extends AbstractModule {
	@Override
	public void install() {
		bind(CadytsPtContext.class).in( Singleton.class );
		addControlerListenerBinding().to(CadytsPtContext.class);
		bind(CadytsPtOccupancyAnalyzerI.class).to(CadytsPtOccupancyAnalyzer.class);
	}
}
