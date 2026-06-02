package org.matsim.simwrapper.DbViewer;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public class DbEventsModule extends AbstractModule {
	private final String outputDirectory;
	private final Scenario scenario;

	public DbEventsModule(String outputDirectory, Scenario scenario) {
		this.outputDirectory = outputDirectory;
		this.scenario = scenario;
	}

	@Override
	public void install() {
		bind(String.class).annotatedWith(DbOutputPath.class).toInstance(outputDirectory);
		bind(Scenario.class).annotatedWith(DbScenario.class).toInstance(scenario);
		bind(DbEventHandler.class).in(Singleton.class);
		bind(AgentTable.class).asEagerSingleton();
		addControllerListenerBinding().to(DbEventListener.class);
	}
}
