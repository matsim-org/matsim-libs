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

package org.matsim.core.mobsim.qsim;

import com.google.inject.*;
import org.apache.log4j.Logger;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.components.QSimComponent;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import java.lang.annotation.Annotation;
import java.util.Collection;

public class QSimProvider implements Provider<QSim> {
	private static final Logger log = Logger.getLogger(QSimProvider.class);
	private final QSim qSim;

	private Injector injector;
	private QSimComponentsConfig components;

	@Inject
	QSimProvider(Injector injector, QSimComponentsConfig components, QSim qSim) {
		this.injector = injector;
		this.components = components;
		this.qSim = qSim;
	}

	@Override
	public QSim get() {
		for (Object activeComponent : components.getActiveComponents()) {
			Key<Collection<Provider<QSimComponent>>> activeComponentKey;
			if (activeComponent instanceof Annotation) {
				activeComponentKey = Key.get(new TypeLiteral<Collection<Provider<QSimComponent>>>(){}, (Annotation) activeComponent);
			} else {
				activeComponentKey = Key.get(new TypeLiteral<Collection<Provider<QSimComponent>>>(){}, (Class<? extends Annotation>) activeComponent);
			}

			Collection<Provider<QSimComponent>> providers = injector.getInstance(activeComponentKey);
			for (Provider<QSimComponent> provider : providers) {
				QSimComponent qSimComponent = provider.get();
				if (qSimComponent instanceof MobsimEngine) {
					MobsimEngine instance = (MobsimEngine) qSimComponent;
					qSim.addMobsimEngine(instance);
					log.info("Added MobsimEngine " + instance.getClass());
				}

				if (qSimComponent instanceof ActivityHandler) {
					ActivityHandler instance = (ActivityHandler) qSimComponent;
					qSim.addActivityHandler(instance);
					log.info("Added Activityhandler " + instance.getClass());
				}

				if (qSimComponent instanceof DepartureHandler) {
					DepartureHandler instance = (DepartureHandler) qSimComponent;
					qSim.addDepartureHandler(instance);
					log.info("Added DepartureHandler " + instance.getClass());
				}

				if (qSimComponent instanceof AgentSource) {
					AgentSource instance = (AgentSource) qSimComponent;
					qSim.addAgentSource(instance);
					log.info("Added AgentSource " + instance.getClass());
				}

				if (qSimComponent instanceof MobsimListener) {
					MobsimListener instance = (MobsimListener) qSimComponent;
					qSim.addQueueSimulationListeners(instance);
					log.info("Added MobsimListener " + instance.getClass());
				}

			}
		}

		return qSim;
	}

}
