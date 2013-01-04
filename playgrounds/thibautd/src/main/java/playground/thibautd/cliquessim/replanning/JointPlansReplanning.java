/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlansReplanning.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.replanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

import playground.thibautd.cliquessim.utils.JointControlerUtils;

/**
 * {@link ReplanningListener} allowing to pass the cliques to the {@link JointStrategyManager}
 * @author thibautd
 */
public class JointPlansReplanning implements ReplanningListener {

	/**
	 * Same as in the "PlansReplanning" class, but passes the population of the
	 * cliques to the StrategyManager, if it exists.
	 * {@inheritDoc}
	 * @see ReplanningListener#notifyReplanning(ReplanningEvent)
	 */
	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		Controler controler = event.getControler();

		controler.getStrategyManager().run(
				JointControlerUtils.getCliques( controler.getScenario() ),
				event.getIteration(), event.getReplanningContext());
	}
}
