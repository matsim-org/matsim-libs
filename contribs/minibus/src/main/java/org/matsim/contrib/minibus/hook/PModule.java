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

import org.apache.log4j.Logger;
import org.matsim.contrib.minibus.PConfigGroup;
import org.matsim.contrib.minibus.fare.TicketMachineDefaultImpl;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.contrib.minibus.operator.POperators;
import org.matsim.contrib.minibus.operator.PerPassengerSubsidy;
import org.matsim.contrib.minibus.operator.SubsidyI;
import org.matsim.contrib.minibus.stats.PStatsModule;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.BVGLines2PtModes;
import org.matsim.contrib.minibus.stats.abtractPAnalysisModules.LineId2PtMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouter;

public final class PModule extends AbstractModule {
	private final static Logger log = Logger.getLogger(PModule.class);

	@Override public void install() {
		final PTransitRouterFactory pTransitRouterFactory = new PTransitRouterFactory(this.getConfig());
		bind(TransitRouter.class).toProvider(pTransitRouterFactory);
		bind(PTransitRouterFactory.class).toInstance(pTransitRouterFactory);

		addControlerListenerBinding().to(PControlerListener.class) ;
//		addControlerListenerBinding().toInstance(pTransitRouterFactory);
		// (needs to be injected _after_ PControlerListener, so that it is executed _before_ PControlerListener.
		// yyyy injecting the TransitRouterFactory besides the TransitRouter is a fix to re-configure the factory in every iteration.
		// A more general solution suggested by MZ would be to define an iteration scope.  Then the factory could be forced
		// to reconstruct itself in every iteration, thus pulling new information (in this case the updated transit schedule)
		// by itself.  Is on the "list", has not been done yet, will be done eventually, until then this remains the way it is.
		// kai, jan'17)
		// The iteration scope of PControlerListener should be executed before iteration scope of PTransitRouterFactory.
		// It is not clear, how to control over the order of the execution. If transit schedule and thus transit router needs to be updated at every iteration,
		// it needs to be explicitly triggered in PControlerListener which is mainly same as before. See also MATSim-768. GL, AA, AN. Jan'18

		bind(TicketMachineI.class).to(TicketMachineDefaultImpl.class);
		
		if ( ConfigUtils.addOrGetModule(getConfig(), PConfigGroup.class ).getSubsidyApproach() == null ) {
			log.info("There is no subsidy added to the operators' score.");
			
		} else if ( ConfigUtils.addOrGetModule(getConfig(), PConfigGroup.class ).getSubsidyApproach().equals("perPassenger") )  {
			log.warn("There is a subsidy added to the operators' score. Approach: 'perPassenger'."
					+ " This approach is rather an example how to implement a subsidy computation approach and should not be used"
					+ " for real studies...");
			bind(SubsidyI.class).to(PerPassengerSubsidy.class);
		
		} else {
			log.warn("Unknown subsidy approach: " + ConfigUtils.addOrGetModule(getConfig(), PConfigGroup.class ).getSubsidyApproach());
			log.warn("Add the following lines of code to your run class:");
			log.warn("");
			log.warn("controler.addOverridingModule(new AbstractModule() {");
			log.warn("");
			log.warn("	@Override");
			log.warn("	public void install() {");
			log.warn("		this.bind(SubsidyI.class).to(<YourSubsidyApproach>.class)");
			log.warn("	}");
			log.warn("});");
			log.warn("");
			throw new RuntimeException("Aborting...");
		}

		bind(POperators.class).to(PBox.class).asEagerSingleton();
		// (needs to be a singleton since it is a data container, and all clients should use the same data container. kai, jan'17)

		bindMobsim().toProvider(PQSimProvider.class) ;
		bind(LineId2PtMode.class).to( BVGLines2PtModes.class ) ;

		install( new PStatsModule() ) ;

		if ( ConfigUtils.addOrGetModule(getConfig(), PConfigGroup.class ).getReRouteAgentsStuck() ) {
			bind(PersonReRouteStuckFactory.class).to( PersonReRouteStuckFactoryImpl.class );
			bind(AgentsStuckHandlerImpl.class) ;
		}

	}

}
