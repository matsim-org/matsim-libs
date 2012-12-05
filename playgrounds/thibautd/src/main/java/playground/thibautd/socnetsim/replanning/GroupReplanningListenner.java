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
package playground.thibautd.socnetsim.replanning;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

/**
 * @author thibautd
 */
public class GroupReplanningListenner implements ReplanningListener {
	private final Population population;
	private GroupStrategyManager strategyManager;

	public GroupReplanningListenner(
			final Population population,
			final GroupStrategyManager strategyManager) {
		this.population = population;
		this.strategyManager = strategyManager;
	}

	@Override
	public void notifyReplanning(final ReplanningEvent event) {
		strategyManager.run( population );
	}
}

