/* *********************************************************************** *
 * project: org.matsim.*
 * GroupReplanningListenner.java
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
package playground.thibautd.socnetsim.framework.controller.listeners;

import org.matsim.analysis.IterationStopWatch;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import playground.thibautd.socnetsim.framework.replanning.GroupStrategyManager;
import playground.thibautd.socnetsim.framework.replanning.GroupStrategyRegistry;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author thibautd
 */
@Singleton
public class GroupReplanningListenner implements PlansReplanning, ReplanningListener {
	private final GroupStrategyManager strategyManager;
	private final Scenario sc;

	// This class might look useless at first, but separating strategy manager from
	// the controler listener allows to build more complex listeners, for instance
	// switching between strategy managers depending on the iteration (PSim)
	@Inject
	public GroupReplanningListenner(
			final Scenario sc,
			final IterationStopWatch stopWatch,
			final GroupIdentifier groupIdentifier,
			final GroupStrategyRegistry registry ) {
		this.sc = sc;
		this.strategyManager =
			new GroupStrategyManager(
					stopWatch,
					groupIdentifier,
					registry );
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		strategyManager.run( event.getReplanningContext() , sc );
	}

	public GroupStrategyManager getStrategyManager() {
		return strategyManager;
	}
}

