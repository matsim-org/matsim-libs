package org.matsim.core.mobsim.qsim.components;

import org.matsim.core.config.Config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class QSimComponentsModule extends AbstractModule {
	@Provides
	@Singleton
	public QSimComponentKeysRegistry provideDefaultQSimComponentsConfig( Config config ) {
		QSimComponentKeysRegistry components = new QSimComponentKeysRegistry();
		new StandardQSimComponentConfigurator(config).configure(components);
		return components;
	}

	@Override
	protected void configure() {
	}
}
