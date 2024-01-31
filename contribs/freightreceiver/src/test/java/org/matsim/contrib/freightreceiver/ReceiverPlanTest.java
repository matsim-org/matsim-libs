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

package org.matsim.contrib.freightreceiver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;


public class ReceiverPlanTest {


	@Test
	void testBuilderTwo() {
		Receiver receiver = ReceiverUtils.newInstance( Id.create( "1", Receiver.class ) );
		ReceiverPlan.Builder builder = ReceiverPlan.Builder.newInstance(receiver, true);
		ReceiverPlan plan = builder.build();
		Assertions.assertEquals(Id.create("1", Receiver.class), plan.getReceiver().getId(), "Wrong receiver Id");
		Assertions.assertNull(plan.getScore(), "Score should be null");
	}

	/* TODO Add tests to check ReceiverOrders */

}
