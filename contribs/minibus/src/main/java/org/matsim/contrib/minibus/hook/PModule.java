/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * Builder.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
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

package org.matsim.contrib.minibus.hook;

import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.fare.TicketMachineDefaultImpl;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.contrib.minibus.operator.Operators;
import org.matsim.contrib.minibus.stats.PStatsModule;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.BVGLines2PtModes;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.LineId2PtMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouter;

public final class PModule {

	public static void configureControler(final Controler controler) {
		final PConfigGroup pConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PConfigGroup.GROUP_NAME, PConfigGroup.class);

		final PTransitRouterFactory pTransitRouterFactory = new PTransitRouterFactory(pConfig.getPtEnabler(), pConfig.getPtRouter(), pConfig.getEarningsPerBoardingPassenger(), pConfig.getEarningsPerKilometerAndPassenger() / 1000.0);

		// For some unknown reason, the core starts failing when I start requesting a trip router out of an injected trip router in 
		// ScoreStatsControlerListener.  Presumably, the manually constructed build procedure here conflicts with the (newer) standard guice
		// procedure.  For the time being, the following two lines seem to fix it.  kai, nov'16
		pTransitRouterFactory.createTransitRouterConfig(controler.getConfig());
		pTransitRouterFactory.updateTransitSchedule(controler.getScenario().getTransitSchedule());

		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bind(PTransitRouterFactory.class).toInstance( pTransitRouterFactory ) ;

				// yyyy the following two should only be bound when pConfig.getReRouteAgentsStuck() is true.  But this
				// does not work without problems (the binding needs to be optional).  kai, jan'17
				bind(PersonReRouteStuckFactory.class).to( PersonReRouteStuckFactoryImpl.class );
				bind(AgentsStuckHandlerImpl.class) ;

				addControlerListenerBinding().to(PControlerListener.class) ;

				bind(TicketMachineI.class).to(TicketMachineDefaultImpl.class);

				bind(Operators.class).to(PBox.class).asEagerSingleton();
				// (needs to be a singleton since it is a data container, and all clients should use the same data container. kai, jan'17)
				
				bindMobsim().toProvider(PQSimProvider.class) ;
				bind(LineId2PtMode.class).to( BVGLines2PtModes.class ) ;
				
				// yyyyyy my intuition is that it should (now) be possible to do the following in a less involved way (using more default material).
				// kai, jan'17
				bind(TripRouter.class).toProvider(new PTripRouterFactoryImpl(controler, pTransitRouterFactory)) ;

				install( new PStatsModule() ) ;
			}
		});

	}
}
