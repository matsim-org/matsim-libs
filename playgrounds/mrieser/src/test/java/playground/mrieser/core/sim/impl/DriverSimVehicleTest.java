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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;

import playground.mrieser.core.sim.api.DriverAgent;

/**
 * @author mrieser
 */
public class DriverSimVehicleTest {

	@Test
	public void testSetGetDriver() {
		DriverSimVehicle vehicle = new DriverSimVehicle();
		FakeDriverAgent driver = new FakeDriverAgent();
		Assert.assertNull(vehicle.getDriver());
		vehicle.setDriver(driver);
		Assert.assertEquals(driver, vehicle.getDriver());
		vehicle.setDriver(null);
		Assert.assertNull(vehicle.getDriver());
	}

	@Test
	public void testNotifyMoveToNextLink() {
		DriverSimVehicle vehicle = new DriverSimVehicle();
		FakeDriverAgent driver = new FakeDriverAgent();
		vehicle.setDriver(driver);

		Assert.assertEquals(0, driver.getNextLinkIdCount);
		Assert.assertEquals(0, driver.notiftyMoveToNextLinkCount);
		vehicle.notifyMoveToNextLink();
		Assert.assertEquals(1, driver.notiftyMoveToNextLinkCount);
		vehicle.notifyMoveToNextLink();
		Assert.assertEquals(2, driver.notiftyMoveToNextLinkCount);
		vehicle.notifyMoveToNextLink();
		Assert.assertEquals(3, driver.notiftyMoveToNextLinkCount);
		Assert.assertEquals(0, driver.getNextLinkIdCount);
	}

	@Test
	public void testGetNextLinkId() {
		DriverSimVehicle vehicle = new DriverSimVehicle();
		FakeDriverAgent driver = new FakeDriverAgent();
		vehicle.setDriver(driver);

		Assert.assertEquals(0, driver.notiftyMoveToNextLinkCount);
		Assert.assertEquals(0, driver.getNextLinkIdCount);
		vehicle.getNextLinkId();
		Assert.assertEquals(1, driver.getNextLinkIdCount);
		vehicle.getNextLinkId();
		Assert.assertEquals(2, driver.getNextLinkIdCount);
		vehicle.getNextLinkId();
		Assert.assertEquals(3, driver.getNextLinkIdCount);
		Assert.assertEquals(0, driver.notiftyMoveToNextLinkCount);
	}

	/*package*/ static class FakeDriverAgent implements DriverAgent {

		public int getNextLinkIdCount = 0;
		public int notiftyMoveToNextLinkCount = 0;

		@Override
		public Id getNextLinkId() {
			this.getNextLinkIdCount++;
			return null;
		}

		@Override
		public void notifyMoveToNextLink() {
			this.notiftyMoveToNextLinkCount++;
		}

	}
}
