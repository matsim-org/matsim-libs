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
package playground.thibautd.socnetsim.controller.listeners;

import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import com.google.inject.Inject;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupStrategyManager;

/**
 * @author thibautd
 */
public class GroupReplanningListenner implements PlansReplanning, ReplanningListener {
	private final GroupStrategyManager strategyManager;
	private final ControllerRegistry registry;

	@Inject
	public GroupReplanningListenner(
			final ControllerRegistry registry,
			final GroupStrategyManager strategyManager) {
		this.registry = registry;
		this.strategyManager = strategyManager;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		strategyManager.run(
				event.getIteration(),
				registry );
	}
}

