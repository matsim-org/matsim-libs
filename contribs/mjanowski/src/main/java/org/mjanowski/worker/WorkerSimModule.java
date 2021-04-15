
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

 package org.mjanowski.worker;


import com.google.inject.name.Names;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;

import javax.inject.Inject;

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
public class WorkerSimModule extends AbstractModule {
	@Inject Config config ;

	private final boolean addDefaultQSimModules;

	public WorkerSimModule() {
		this(true);
	}

	public WorkerSimModule(boolean addDefaultQSimModules) {
		this.addDefaultQSimModules = addDefaultQSimModules;
	}
	
	@Override
	public void install() {
		bind(Mobsim.class).to(WorkerSim.class).asEagerSingleton();
//		bind(Mobsim.class).toProvider(WorkerSimProvider.class);
		
		// yyyy the following will eventually be moved to QSim scope, and into QNetsimEngineModule:
//		if ( config.qsim().isUseLanes() ) {
//			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
//		} else {
//			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
//		}
		
		// yyyy the following will eventually be moved to QSim scope, and into TranistEngineModule:
//		if ( config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
//			bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
//		} else {
//			// Explicit bindings are required, so although it may not be used, we need provide something.
//			bind( TransitStopHandlerFactory.class ).to( SimpleTransitStopHandlerFactory.class );
//		}
		// yy see MATSIM-756
	}

}
