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

package playground.mrieser.core.mobsim.impl;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;

import playground.mrieser.core.mobsim.api.DriverAgent;
import playground.mrieser.core.mobsim.network.api.MobsimLink2;

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

	@Test
	public void testGetSizeInEquivalents() {
		DefaultSimVehicle vehicle = new DefaultSimVehicle(null);
		Assert.assertEquals(1.0, vehicle.getSizeInEquivalents(), MatsimTestUtils.EPSILON);
		vehicle = new DefaultSimVehicle(null, 1.2);
		Assert.assertEquals(1.2, vehicle.getSizeInEquivalents(), MatsimTestUtils.EPSILON);
		vehicle = new DefaultSimVehicle(null, 6.0);
		Assert.assertEquals(6.0, vehicle.getSizeInEquivalents(), MatsimTestUtils.EPSILON);
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
		public void handleNextAction(final MobsimLink2 link) {
		}
	}
}
