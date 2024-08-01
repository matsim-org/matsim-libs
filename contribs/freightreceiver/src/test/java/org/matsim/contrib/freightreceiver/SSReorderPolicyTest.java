/* *********************************************************************** *
 * project: org.matsim.*
 * SSReorderPolicyTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import org.matsim.testcases.MatsimTestUtils;

public class SSReorderPolicyTest {

	@Test
	void testCalculateOrderQuantity() {
		ReorderPolicy policy = ReceiverUtils.createSSReorderPolicy(5.0, 10.0);
		Assertions.assertEquals(0.0, policy.calculateOrderQuantity(6.0), MatsimTestUtils.EPSILON, "Wrong reorder quantity");
		Assertions.assertEquals(5.0, policy.calculateOrderQuantity(5.0), MatsimTestUtils.EPSILON, "Wrong reorder quantity");
		Assertions.assertEquals(6.0, policy.calculateOrderQuantity(4.0), MatsimTestUtils.EPSILON, "Wrong reorder quantity");
	}

}
