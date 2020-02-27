
/* *********************************************************************** *
 * project: org.matsim.*
 * QSimModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.IterationScoped;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsModule;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueueModule;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.SimpleTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitEngineModule;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLanesNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * It becomes only clear quite indirectly from the material below: An easy way to configure the QSim is
 * <pre>
 *       controler.addOverridingQSimModule( new AbstractQSimModule() {
 *          @Override protected void configureQSim() {
 *             ...
 *          }
 *       }
 * </pre>
 * The bindings that can be overridden are defined in the default modules referenced near the end of this class.
 *
 * Some people say that one should try to stay away from <i>overriding</i> modules, and rather just set them
 * (leading to an error message if one tries to bind the same thing twice).  MATSim has a different history, and
 * thus the global level at this point only allows to override.  At the QSim level, there is also
 * <pre>
 *       controler.addQSimModule( new AbstractQSimModule() {
 *          @Override protected void configureQSim() {
 *             ...
 *          }
 *       }
 * </pre>
 * To be used when the intention is to <i>not</i> override.
 */
public class QSimModule extends AbstractModule {
	@Inject Config config ;
	
	private final boolean addDefaultQSimModules;
	
	public QSimModule() {
		this(true);
	}
	
	public QSimModule(boolean addDefaultQSimModules) {
		this.addDefaultQSimModules = addDefaultQSimModules;
	}
	
	@Override
	public void install() {
		install(new QSimComponentsModule());
		// this is essentially a structured collection of strings that determines which QSimModules will later be used.  Can be
		// used to configure from config, but maybe easiest just ignore when getting started with this.  The default QSimModules
		// bind the corresponding implementations under the correct names.
		
		if (addDefaultQSimModules) {
			getDefaultQSimModules().forEach(this::installQSimModule);
			// this binds all the default modules, i.e. sets up the default QSim
		}

		bind(QSim.class).in(IterationScoped.class);
		bind(Netsim.class).to(QSim.class).in(IterationScoped.class);
		bind(Mobsim.class).toProvider(QSimProvider.class).in(IterationScoped.class);
	}

	static public Collection<AbstractQSimModule> getDefaultQSimModules() {
		return Arrays.asList(
				new MessageQueueModule(),  // defines "MessageQueueEngine"
				new ActivityEngineModule(), // defines "ActivityEngine"
				new QNetsimEngineModule(), // defines "NetsimEngine"
				new TeleportationModule(), // etc.
				new PopulationModule(),
				new NetworkChangeEventsModule(),
				new TransitEngineModule()
				);
	}
}
