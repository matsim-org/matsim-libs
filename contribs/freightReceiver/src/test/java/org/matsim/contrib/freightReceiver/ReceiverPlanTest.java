/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freightReceiver;

import org.matsim.api.core.v01.Id;
import org.junit.Assert;
import org.junit.Test;


public class ReceiverPlanTest {


	@Test
	public void testBuilderTwo() {
		Receiver receiver = ReceiverUtils.newInstance( Id.create( "1", Receiver.class ) );
		ReceiverPlan.Builder builder = ReceiverPlan.Builder.newInstance(receiver, true);
		ReceiverPlan plan = builder.build();
		Assert.assertEquals("Wrong receiver Id", Id.create("1", Receiver.class), plan.getReceiver().getId());
		Assert.assertNull("Score should be null", plan.getScore());
	}

	/* TODO Add tests to check ReceiverOrders */

}
