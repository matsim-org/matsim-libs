/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author mrieser
 */
public class NetworkRouteDriverTest {

	@Test
	public void testNotifyMoveToNextLink() {
		Id ids[] = new Id[10];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = new IdImpl(i);
		}
		NetworkRoute route = new LinkNetworkRouteImpl(ids[0], ids[9]);
		List<Id> linkIds = new ArrayList<Id>(ids.length);
		Collections.addAll(linkIds, ids[1], ids[2], ids[3], ids[4], ids[5], ids[6], ids[7], ids[8]);
		route.setLinkIds(ids[0], linkIds, ids[9]);

		NetworkRouteDriver driver = new NetworkRouteDriver(null, null, route, null);
		Assert.assertEquals(ids[0], driver.getNextLinkId());
		Assert.assertEquals(ids[0], driver.getNextLinkId()); // multiple calls should return the same
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[1], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[2], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[3], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[4], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[5], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[6], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[7], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[8], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertEquals(ids[9], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertNull(driver.getNextLinkId());
		driver.notifyMoveToNextLink(); // there should not be any crash
		Assert.assertNull(driver.getNextLinkId());
	}

	@Test
	public void testGetNextLinkId_emptyRoute() {
		Id ids[] = new Id[10];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = new IdImpl(i);
		}
		NetworkRoute route = new LinkNetworkRouteImpl(ids[0], ids[0]);
		List<Id> linkIds = new ArrayList<Id>(ids.length);
		route.setLinkIds(ids[0], linkIds, ids[0]);

		NetworkRouteDriver driver = new NetworkRouteDriver(null, null, route, null);
		Assert.assertEquals(ids[0], driver.getNextLinkId());
		driver.notifyMoveToNextLink();
		Assert.assertNull(driver.getNextLinkId());
	}
}
