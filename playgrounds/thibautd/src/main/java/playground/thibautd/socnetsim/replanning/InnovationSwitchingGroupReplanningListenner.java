/* *********************************************************************** *
 * project: org.matsim.*
 * InovationSwitchingGroupReplanningListenner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;
import playground.thibautd.socnetsim.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.controller.ControllerRegistry;

/**
 * @author thibautd
 */
public class InnovationSwitchingGroupReplanningListenner implements ReplanningListener {
	private static final Logger log =
		Logger.getLogger(InnovationSwitchingGroupReplanningListenner.class);

	private final GroupStrategyManager mainStrategyManager;
	private final GroupStrategyManager innovativeStrategyManager;
	private final ControllerRegistry registry;

	public InnovationSwitchingGroupReplanningListenner(
			final ControllerRegistry registry,
			final GroupStrategyManager mainStrategyManager,
			final GroupStrategyManager innovativeStrategyManager) {
		this.registry = registry;
		this.mainStrategyManager = mainStrategyManager;
		this.innovativeStrategyManager = innovativeStrategyManager;
	}


	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		final PseudoSimConfigGroup config = (PseudoSimConfigGroup)
			registry.getScenario().getConfig().getModule(
					PseudoSimConfigGroup.GROUP_NAME );
		final GroupReplanningConfigGroup repl = (GroupReplanningConfigGroup) 
			registry.getScenario().getConfig().getModule(
					GroupReplanningConfigGroup.GROUP_NAME );

		if ( event.getIteration() < repl.getDisableInnovationAfterIter() &&
				config.isPSimIter( event.getIteration() ) ) {
			log.info( "performing INNOVATION ONLY iteration (for feeding PSim)" );
			innovativeStrategyManager.run(
				event.getReplanningContext(),
				registry.getScenario() );
		}
		else {
			log.info( "performing normal iteration (with non-innovative strategies)" );
			mainStrategyManager.run(
					event.getReplanningContext(),
					registry.getScenario() );
		}
	}
}
