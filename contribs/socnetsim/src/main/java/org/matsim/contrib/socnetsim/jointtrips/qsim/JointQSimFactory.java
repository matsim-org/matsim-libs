/* *********************************************************************** *
 * project: org.matsim.*
 * JointQSimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.qsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author thibautd
 */
public class JointQSimFactory implements MobsimFactory, Provider<QSim> {
	private static final Logger log =
		Logger.getLogger(JointQSimFactory.class);
	
	private final Scenario sc;
	private final EventsManager events;
	private final Config config;

	@Inject
	public JointQSimFactory( final Scenario sc, final EventsManager events, final Config config ) {
		this.sc = sc;
		this.events = events;
		this.config = config;
	}
	
	public JointQSimFactory() {
		this( null , null, ConfigUtils.createConfig() );
	}
	
	@Override
	public QSim get() {
		return createMobsim( sc , events );
	}

	@Override
	public QSim createMobsim(
			final Scenario sc1,
			final EventsManager eventsManager) {
        final QSimConfigGroup conf = sc1.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		if ( !conf.getMainModes().contains( JointActingTypes.DRIVER ) ) {
			log.warn( "adding the driver mode as a main mode in the config at "+getClass()+" initialisation!" );
			final List<String> ms = new ArrayList<String>( conf.getMainModes() );
			ms.add( JointActingTypes.DRIVER );
			conf.setMainModes( ms );
		}
		
		return new QSimBuilder(config) //
				.useDefaults() //
				.addQSimModule(new JointQSimModule()) //
				.configureComponents(JointQSimModule::configureComponents) //
				.build(sc1, eventsManager);
	}
}

