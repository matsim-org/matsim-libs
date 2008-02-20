/* *********************************************************************** *
 * project: org.matsim.*
 * GuidedAgentFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.eut;

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;
import org.matsim.withinday.WithindayAgent;
import org.matsim.withinday.WithindayAgentLogicFactory;
import org.matsim.withinday.contentment.AgentContentmentI;
import org.matsim.withinday.routeprovider.RouteProvider;

/**
 * @author illenberger
 *
 */
public class GuidedAgentFactory extends WithindayAgentLogicFactory {

	private double equipmentFraction = 0.1;
	
	private ForceReplan forceReplan = new ForceReplan();
	
	private PreventReplan preventReplan = new PreventReplan();
	
	private ReactRouteGuidance router;
	/**
	 * @param network
	 * @param scoringConfig
	 */
	public GuidedAgentFactory(NetworkLayer network,
			CharyparNagelScoringConfigGroup scoringConfig, TravelTimeI reactTTs) {
		super(network, scoringConfig);
		router = new ReactRouteGuidance(network, reactTTs);
	}

	@Override
	public AgentContentmentI createAgentContentment(WithindayAgent agent) {
		Gbl.random.nextDouble();
		if(Gbl.random.nextDouble() < equipmentFraction)
			return forceReplan;
		else
			return preventReplan;
	}

	@Override
	public RouteProvider createRouteProvider() {
		return router;
	}

}
