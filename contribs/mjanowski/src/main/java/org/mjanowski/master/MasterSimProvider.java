/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * QSimProvider.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.mjanowski.master;

import com.google.inject.*;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

public class MasterSimProvider implements Provider<MasterSim> {
	private static final Logger log = Logger.getLogger(MasterSimProvider.class);

	private Injector injector;
	private Config config;
	private MasterSim masterSim;
	private Collection<AbstractQSimModule> modules;
	private List<AbstractQSimModule> overridingModules;
	private QSimComponentsConfig components;

	@Inject
    MasterSimProvider(Injector injector, Config config, @Named("overrides") List<AbstractQSimModule> overridingModules,
					  MasterSim masterSim) {
		this.injector = injector;
		// (these are the implementations)
		this.config = config;
		this.masterSim = masterSim;
		this.components = components;
		this.overridingModules = overridingModules;
	}

	@Override
	public MasterSim get() {
		return masterSim;
	}

	/**
	 * Historically, some bindings that are used in the QSim were defined in the
	 * outer controller scope. This method checks that those bindings are now
	 * registered in the QSim scope.
	 */
	private void performHistoricalCheck(Injector injector) {
		boolean foundNetworkFactoryBinding = true;

		try {
			injector.getBinding(QNetworkFactory.class);
		} catch (ConfigurationException e) {
			foundNetworkFactoryBinding = false;
		}

		if (foundNetworkFactoryBinding) {
			throw new IllegalStateException("QNetworkFactory should only be bound via AbstractQSimModule");
		}

		boolean foundTransitStopHandlerFactoryBinding = true;

		try {
			injector.getBinding(TransitStopHandlerFactory.class);
		} catch (ConfigurationException e) {
			foundTransitStopHandlerFactoryBinding = false;
		}

		if (foundTransitStopHandlerFactoryBinding) {
			throw new IllegalStateException("TransitStopHandlerFactory should be bound via AbstractQSimModule");
		}
	}
}
