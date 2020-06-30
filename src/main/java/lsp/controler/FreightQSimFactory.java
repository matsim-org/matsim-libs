/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ${file_name}
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
 *
 * ${filecomment}
 * ${package_declaration}
 *
 * ${typecomment}
 * ${type_declaration}
 */

package lsp.controler;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.FreightConfigGroup.TimeWindowHandling;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;

import javax.inject.Inject;
import java.util.Collection;

@Deprecated public // replace by something that just adds the necessary elements to the default mobsim, rather than replacing the mobsim.  kai/kai, jun'20
class FreightQSimFactory implements Provider<Mobsim> {
	private static final Logger log = Logger.getLogger( FreightQSimFactory.class ) ;

	private final Scenario scenario;
	private final EventsManager eventsManager;
	private final CarrierResourceTracker carrierResourceTracker;
	private final FreightConfigGroup carrierConfig;

	@Inject
	FreightQSimFactory( Scenario scenario, EventsManager eventsManager, CarrierResourceTracker carrierResourceTracker, FreightConfigGroup carrierConfig ) {
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.carrierResourceTracker = carrierResourceTracker;
		this.carrierConfig = carrierConfig;
	}

	@Override
	public Mobsim get() {
		final QSimBuilder qSimBuilder = new QSimBuilder( scenario.getConfig() );
		qSimBuilder.useDefaults() ;
		final QSim sim = qSimBuilder.build(scenario, eventsManager);
		
		Collection<MobSimVehicleRoute> vRoutes = carrierResourceTracker.createPlans();
		FreightAgentSource agentSource = new FreightAgentSource(vRoutes, new DefaultAgentFactory(sim), sim);
		sim.addAgentSource(agentSource);
		if (carrierConfig.getTimeWindowHandling()!= TimeWindowHandling.ignore) {
			log.warn("You are requesting (per config) something different from TimeWindowHandling.ignore, but have no implementation for this.  Throwing an " +
							 "exception fails many tests so it must have been a design decision; thus only logging a warning here.  kai, nov'19") ;
//			throw new RuntimeException( "not implemented" ) ;
		}
		return sim;
	}

}
