/* *********************************************************************** *
 * project: org.matsim.*
 * TopologyFactory.java
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.PersonEvent;

/**
 * @author thibautd
 */
public class TopologyFactory {
	private final LinkTopology linkTopology;
	private final double timeWindowRadius;

	public TopologyFactory(
			final Network network,
			final double acceptableDistance,
			final double timeWindowRadius) {
		this.linkTopology = new LinkTopology(network, acceptableDistance);
		this.timeWindowRadius = timeWindowRadius;
	}

	/**
	 * @return the internal LinkTOpology instance (not a proper factory method:
	 * two consecutive call will return the same instance)
	 */
	public LinkTopology getLinkTopology() {
		return this.linkTopology;
	}

	public EventsTopology createEventTopology(final List<? extends PersonEvent> events) {
		return new EventsTopology(
				events,
				this.timeWindowRadius,
				this.linkTopology); 
	}
}

