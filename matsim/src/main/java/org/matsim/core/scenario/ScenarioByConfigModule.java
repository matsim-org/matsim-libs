package org.matsim.core.scenario;


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public class ScenarioByConfigModule extends AbstractModule {
	@Override
	public void install() {
		final ScenarioLoaderImpl loader = new ScenarioLoaderImpl( getConfig() );
		binder().requestInjection( loader );
		Scenario scenario = loader.loadScenario();

		install(new ScenarioByInstanceModule(scenario));
	}
}
