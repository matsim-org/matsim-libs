package org.matsim.core.scenario;


import com.google.inject.Provides;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public class ScenarioByConfigModule extends AbstractModule {
	@Override
	public void install() {
		bind( ScenarioLoaderImpl.class ).toInstance( new ScenarioLoaderImpl( getConfig() ) );
		install( new ScenarioByInstanceModule( null ) );
	}

	@Provides
	private Scenario createScenario( final ScenarioLoaderImpl loader ) {
		return loader.loadScenario();
	}
}
