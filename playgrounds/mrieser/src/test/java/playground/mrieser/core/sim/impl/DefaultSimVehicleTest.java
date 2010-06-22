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
import playground.mrieser.core.sim.network.api.SimLink;

/**
 * @author mrieser
 */
public class DefaultSimVehicleTest {

	@Test
	public void testSetGetDriver() {
		DefaultSimVehicle vehicle = new DefaultSimVehicle(null);
		FakeDriverAgent driver = new FakeDriverAgent();
		Assert.assertNull(vehicle.getDriver());
		vehicle.setDriver(driver);
		Assert.assertEquals(driver, vehicle.getDriver());
		vehicle.setDriver(null);
		Assert.assertNull(vehicle.getDriver());
	}

	/*package*/ static class FakeDriverAgent implements DriverAgent {
		@Override
		public Id getNextLinkId() {
			return null;
		}
		@Override
		public void notifyMoveToNextLink() {
		}
		@Override
		public double getNextActionOnCurrentLink() {
			return -1.0;
		}
		@Override
		public void handleNextAction(final SimLink link) {
		}
	}
}
