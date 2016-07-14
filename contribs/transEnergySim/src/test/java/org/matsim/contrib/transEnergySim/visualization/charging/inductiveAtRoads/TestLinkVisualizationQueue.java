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

package org.matsim.contrib.transEnergySim.visualization.charging.inductiveAtRoads;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowLinkLevel;
import org.matsim.contrib.transEnergySim.analysis.charging.InductiveChargingAtRoadOutputLog;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public class TestLinkVisualizationQueue extends TestCase {

	public void testBasic() {
		InductiveChargingAtRoadOutputLog log = new InductiveChargingAtRoadOutputLog();

		Id<Link> linkId = Id.create("link-1", Link.class);
		Id<Vehicle> agentId = Id.create("agent-1", Vehicle.class);
		log.add(new ChargingLogRowLinkLevel(agentId, linkId, (24 * 3600) - 10, 20, 3600 * 20));
		log.add(new ChargingLogRowLinkLevel(agentId, linkId, 80, 20, 3600 * 20));
		log.add(new ChargingLogRowLinkLevel(agentId, linkId, 90, 20, 3600 * 20));

		LinkVisualizationQueue linkEventsQueue = log.getLinkEventsQueue();

		assertEquals(3600.0, linkEventsQueue.getValue(linkId, 5));
		assertEquals(3600.0, linkEventsQueue.getValue(linkId, 10));
		assertEquals(0.0, linkEventsQueue.getValue(linkId, 15));
		assertEquals(3600.0, linkEventsQueue.getValue(linkId, 80));
		assertEquals(3600.0, linkEventsQueue.getValue(linkId, 85));
		assertEquals(2 * 3600.0, linkEventsQueue.getValue(linkId, 95));
		assertEquals(0.0, linkEventsQueue.getValue(linkId, 130));
		assertEquals(3600.0, linkEventsQueue.getValue(linkId, (24 * 3600) - 10 + 1));
	}

	public void testNoValueBeginning() {
		InductiveChargingAtRoadOutputLog log = new InductiveChargingAtRoadOutputLog();

		Id<Link> linkId = Id.create("link-1", Link.class);
		Id<Vehicle> agentId = Id.create("agent-1", Vehicle.class);
		log.add(new ChargingLogRowLinkLevel(agentId, linkId, 80, 20, 3600 * 20));

		LinkVisualizationQueue linkEventsQueue = log.getLinkEventsQueue();

		assertEquals(0.0, linkEventsQueue.getValue(linkId, 5));
	}

	public void testLinkIdDoesNotExist() {
		InductiveChargingAtRoadOutputLog log = new InductiveChargingAtRoadOutputLog();

		Id<Link> linkId = Id.create("link-1", Link.class);
		Id<Vehicle> agentId = Id.create("agent-1", Vehicle.class);
		log.add(new ChargingLogRowLinkLevel(agentId, linkId, 80, 20, 3600 * 20));

		LinkVisualizationQueue linkEventsQueue = log.getLinkEventsQueue();

		assertEquals(0.0, linkEventsQueue.getValue(Id.create("link-2", Link.class), 5));
	}

}
