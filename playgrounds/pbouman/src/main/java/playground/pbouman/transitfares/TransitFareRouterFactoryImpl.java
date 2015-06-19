/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.pbouman.transitfares;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.pbouman.agentproperties.AgentProperties;

import javax.inject.Provider;
import java.util.Map;

public class TransitFareRouterFactoryImpl implements Provider<TransitRouter> {

	private final ScenarioImpl scenario;
	
	private final TransitSchedule schedule;
	private final TransitRouterConfig config;
	private final TransitRouterNetwork routerNetwork;
	private final Map<String,AgentProperties> agentProperties;
	
	public TransitFareRouterFactoryImpl(final Scenario scenario, final TransitRouterConfig config, final Map<String,AgentProperties> ap) {
		this.scenario = (ScenarioImpl) scenario;
		this.schedule = scenario.getTransitSchedule();
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.getBeelineWalkConnectionDistance());
		this.agentProperties = ap;
	}
	
	public TransitFareRouterFactoryImpl(final Scenario scenario, final TransitRouterConfig config) {
		this.scenario = (ScenarioImpl) scenario;
		this.schedule = scenario.getTransitSchedule();
		this.config = config;
		this.routerNetwork = TransitRouterNetwork.createFromSchedule(this.schedule, this.config.getBeelineWalkConnectionDistance());
		this.agentProperties = null;
	}
	
	@Override
	public TransitRouter get()
	{	
		TransitFareRouterNetworkTimeAndDisutilityCalc costCalc;
		if (agentProperties != null)
			costCalc = new TransitFareRouterNetworkTimeAndDisutilityCalc(this.config, this.scenario, agentProperties);
		else
			costCalc = new TransitFareRouterNetworkTimeAndDisutilityCalc(this.config, this.scenario);
			
		
		return new TransitRouterImpl(config, new PreparedTransitSchedule(schedule), routerNetwork, costCalc, costCalc);
	}

}
