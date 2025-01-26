
/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngineModule.java
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

 package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public final class QNetsimEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "NetsimEngine";
	
	@Override
	protected void configureQSim() {
		bind(QNetsimEngineI.class).to(QNetsimEngineWithThreadpool.class).in( Singleton.class );
		bind( NetworkModeDepartureHandler.class ).to(NetworkModeDepartureHandlerDefaultImpl.class ).in( Singleton.class );
		// (given the "overriding" architecture, these are default bindings which may be overridden later)

		if ( this.getConfig().qsim().isUseLanes() ) {
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ).in( Singleton.class ) ;
			bind( DefaultQNetworkFactory.class ).in( Singleton.class );
			// (need this here because QLanesNetworkFactory uses it as a delegate.)
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ).in( Singleton.class) ;
		}

		// defining this here so we do not have to hedge against null:
		Multibinder.newSetBinder( this.binder(), LinkSpeedCalculator.class );

		// specialized link speed calculators can be set via the syntax
//			Multibinder.newSetBinder( this.binder(), LinkSpeedCalculator.class ).addBinding().to...
		// yyyy maybe move as generalized syntax to AbstractQSimModule

		// ---
		// the following will automatically register the corresponding capabilities with the qsim:

		addQSimComponentBinding( COMPONENT_NAME ).to( NetworkModeDepartureHandler.class );
		// (this will register the DepartureHandler functionality.  It will, however, use whatever is bound to the interface, and not
		// necessarily the above binding.)

		addQSimComponentBinding( COMPONENT_NAME ).to( QNetsimEngineI.class );
		// (this will register the MobsimEngine functionality)

		// kai, jan'25:

		// I am currently thinking that the NetworkModeDepartureHandler as a separate interface is not needed.  It used to be hardwired into
		// the QNetsimEngine, but that is no longer the case.  Technically, it just does something like
		// qNetsimEngine.getNetsimNetwork.getNetsimLink.letVehicleDepart, so as long as all those classes have the necessary functionality, it
		// does not have to be tightly integrated.

		// Internally, it handles departures of `if (agent instanceof MobsimDriverAgent)', meaning that it does not check for actual mode.
		// (Would need to check how this is implemented ... does the agent change its capabilities for every leg?)
	}
}
