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
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.PtMode2LineSetter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripRouter;
import org.matsim.pt.router.TransitRouter;

public class PModule {
	private PTransitRouterFactory pTransitRouterFactory = null;
	private Class<? extends javax.inject.Provider<TripRouter>> tripRouterProvider = null;
	
	public void setPTransitRouterFactory(PTransitRouterFactory pTransitRouterFactory) {
		this.pTransitRouterFactory = pTransitRouterFactory;
	}
	public void setTripRouterFactory(Class<? extends javax.inject.Provider<TripRouter>> tripRouterFactory) {
		this.tripRouterProvider = tripRouterFactory;
	}
	public void configureControler(final Controler controler) {
		final PConfigGroup pConfig = ConfigUtils.addOrGetModule(controler.getConfig(), PConfigGroup.GROUP_NAME, PConfigGroup.class);
		pConfig.validate(controler.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		if (pTransitRouterFactory == null) {
			pTransitRouterFactory = new PTransitRouterFactory(pConfig.getPtEnabler(), pConfig.getPtRouter(), pConfig.getEarningsPerBoardingPassenger(), pConfig.getEarningsPerKilometerAndPassenger() / 1000.0);

			// For some unknown reason, the core starts failing when I start requesting a trip router out of an injected trip router in 
			// ScoreStatsControlerListener.  Presumably, the manually constructed build procedure here conflicts with the (newer) standard guice
			// procedure.  For the time being, the following two lines seem to fix it.  kai, nov'16
			pTransitRouterFactory.createTransitRouterConfig(controler.getConfig());
			pTransitRouterFactory.updateTransitSchedule(controler.getScenario().getTransitSchedule());
		}
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(TransitRouter.class).toInstance( pTransitRouterFactory.get() ) ;

				AgentsStuckHandlerImpl agentsStuckHandler = null;
				PersonReRouteStuckFactory stuckFactory = null;
				if (pConfig.getReRouteAgentsStuck()) {
					agentsStuckHandler = new AgentsStuckHandlerImpl();
					if(stuckFactory == null) {
						stuckFactory = new PersonReRouteStuckFactoryImpl();
					}
					bind(PersonReRouteStuckFactory.class).toInstance( stuckFactory );
					bind(AgentsStuckHandlerImpl.class) ;
				}
				
				addControlerListenerBinding().toInstance(new PControlerListener(controler, pTransitRouterFactory, stuckFactory, agentsStuckHandler));

				bind(TicketMachineI.class).to(TicketMachineDefaultImpl.class);
				bind(Operators.class).to(PBox.class).asEagerSingleton();
				bindMobsim().toProvider(PQSimProvider.class) ;
				bind(PtMode2LineSetter.class).to( BVGLines2PtModes.class ) ;
				install( new PStatsModule() ) ;
				bind(TripRouter.class).toProvider(PTripRouterFactoryFactory.getTripRouterFactoryInstance(controler, tripRouterProvider, pTransitRouterFactory));
			}
		});


	}
}
