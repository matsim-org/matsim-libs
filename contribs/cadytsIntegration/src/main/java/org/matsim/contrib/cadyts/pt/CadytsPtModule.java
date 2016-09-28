package org.matsim.contrib.cadyts.pt;


import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.CadytsBuilderImpl;
import org.matsim.core.controler.AbstractModule;

public class CadytsPtModule extends AbstractModule {
	@Override
	public void install() {
		bind(CadytsPtContext.class).asEagerSingleton();
		addControlerListenerBinding().to(CadytsPtContext.class);
		bind(CadytsBuilder.class).to(CadytsBuilderImpl.class);
	}
}
