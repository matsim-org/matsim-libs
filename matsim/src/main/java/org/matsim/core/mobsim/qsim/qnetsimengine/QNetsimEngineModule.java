
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

import com.google.inject.multibindings.Multibinder;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public final class QNetsimEngineModule extends AbstractQSimModule {
	public final static String COMPONENT_NAME = "NetsimEngine";
	
	@Override
	protected void configureQSim() {
		bind(QNetsimEngineI.class).to(QNetsimEngineWithThreadpool.class).asEagerSingleton();

		bind(VehicularDepartureHandler.class).toProvider(QNetsimEngineDepartureHandlerProvider.class).asEagerSingleton();

		if ( this.getConfig().qsim().isUseLanes() ) {
			bind(QNetworkFactory.class).to( QLanesNetworkFactory.class ) ;
			bind( DefaultQNetworkFactory.class );
			// (need this here because QLanesNetworkFactory uses it as a delegate.  maybe some other design would be better?  kai, jun'23)
		} else {
			bind(QNetworkFactory.class).to( DefaultQNetworkFactory.class ) ;
		}

		// defining this here so we do not have to hedge against null:
		Multibinder.newSetBinder( this.binder(), LinkSpeedCalculator.class );

		// specialized link speed calculators can be set via the syntax
//			Multibinder.newSetBinder( this.binder(), LinkSpeedCalculator.class ).addBinding().to...
		// yyyy maybe move as generalized syntax to AbstractQSimModule

		addQSimComponentBinding( COMPONENT_NAME ).to( VehicularDepartureHandler.class );
		addQSimComponentBinding( COMPONENT_NAME ).to( QNetsimEngineI.class );
	}
}
