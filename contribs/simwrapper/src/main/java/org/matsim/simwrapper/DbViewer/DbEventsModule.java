package org.matsim.simwrapper.DbViewer;

import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.matsim.core.controler.AbstractModule;

public class DbEventsModule extends AbstractModule {
	private final String outputDirectory;

	public DbEventsModule(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}


	@Override
	public void install() {
		bind(String.class).annotatedWith(DbOutputPath.class).toInstance(outputDirectory);
		bind(DbEventHandler.class).in(Singleton.class);
		addControllerListenerBinding().to(DbEventListener.class);
	}
}
