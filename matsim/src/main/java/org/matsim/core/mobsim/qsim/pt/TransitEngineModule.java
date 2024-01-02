
/* *********************************************************************** *
 * project: org.matsim.*
 * TransitEngineModule.java
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

 package org.matsim.core.mobsim.qsim.pt;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.pt.ReconstructingUmlaufBuilder;
import org.matsim.pt.UmlaufBuilder;

public class TransitEngineModule extends AbstractQSimModule {
	public final static String TRANSIT_ENGINE_NAME = "TransitEngine";

	@Override
	protected void configureQSim() {
		bind(TransitQSimEngine.class).asEagerSingleton();
		addQSimComponentBinding( TRANSIT_ENGINE_NAME ).to( TransitQSimEngine.class );

		if ( this.getConfig().transit().isUseTransit() && this.getConfig().transit().isUsingTransitInMobsim() ) {
			bind( TransitStopHandlerFactory.class ).to( ComplexTransitStopHandlerFactory.class ) ;
		} else {
			// Explicit bindings are required, so although it may not be used, we need provide something.
			bind( TransitStopHandlerFactory.class ).to( SimpleTransitStopHandlerFactory.class );
		}

		bind( UmlaufBuilder.class ).to( ReconstructingUmlaufBuilder.class );

		OptionalBinder.newOptionalBinder(binder(), TransitDriverAgentFactory.class)
			.setDefault().to( DefaultTransitDriverAgentFactory.class );
	}

	@Provides
	@Singleton
	public TransitStopAgentTracker transitStopAgentTracker(QSim qSim) {
		return new TransitStopAgentTracker(qSim.getEventsManager());
	}
}
