/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;


import com.google.inject.*;
import com.google.inject.multibindings.Multibinder;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigWriter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class ConfigModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(ConfigModule.class);
	// a set of classes instead of instances makes sure one gets one object of each class,
	// even ich class added severeal times
	private final Collection< Class<? extends ConfigGroup> > groupList = new HashSet<>();
	private final String fileName;

	public ConfigModule( final String fileName,
						 final Class<? extends ConfigGroup>... groups ) {
		this.fileName = fileName;
		groupList.add(PreprocessedModelRunnerConfigGroup.class);
		groupList.add(SocialNetworkGenerationConfigGroup.class);

		for ( Class<? extends ConfigGroup> group : groups ) {
			groupList.add( group );
		}
	}

	@Override
	protected void configure() {
		log.debug( "Configuring "+getClass().getSimpleName() );
		final Multibinder<ConfigGroup> configGroups = Multibinder.newSetBinder(binder(), ConfigGroup.class);
		for (Class<? extends ConfigGroup> g : groupList) {
			log.debug( "Binding "+g.getSimpleName() );
			bind(g).in( Singleton.class );
			configGroups.addBinding().to(g);
		}

		bind(Config.class).toProvider(
				new Provider<Config>() {
					@Inject
					final Set<ConfigGroup> groups = null;

					@Override
					public Config get() {
						final Config config = new Config();

						for ( ConfigGroup g : groups ) config.addModule( g );

						new ConfigReader( config ).readFile( fileName );
						logGroups( config );

						return config;
					}
				} ).asEagerSingleton();

		// Dirty trick to force loading config.
		// Sure it can be done better...
		requestInjection( new Object() { @Inject Config config; } );
		log.debug("Configuring " + getClass().getSimpleName() + ": DONE");
	}

	private static void logGroups( final Config config ) {
		final String newline = System.getProperty( "line.separator" );// use native line endings for logfile
		final StringWriter writer = new StringWriter();
		new ConfigWriter( config ).writeStream( new PrintWriter( writer ), newline );

		log.info( "Config params:" );
		log.info(writer.getBuffer().toString());
	}
}
