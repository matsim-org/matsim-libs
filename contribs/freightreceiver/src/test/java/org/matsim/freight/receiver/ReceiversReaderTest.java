/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversReaderTest.java
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

package org.matsim.freight.receiver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiversReaderTest {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This is just a simplified version of {@link #testV2()} with the main
	 * difference being that the input file for this test was actually
	 * generated from the {@link ReceiversWriterTest#testV2()} class
	 * whereas the more complete test below was inherited (and manually adapted)
	 * from the original DFG code.
	 */
	@Test
	void testBasicV2(){
		Receivers receivers = new Receivers();
		try {
			new ReceiversReader(receivers).readFile(utils.getClassInputDirectory() + "receivers_v2_basic.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Should read without exception.");
		}

		/* Check one product type completely. */
		ProductType p1 = receivers.getProductType(Id.create("P1", ProductType.class));
		Assertions.assertNotNull(p1, "Product type should exists");
		Assertions.assertEquals(Id.createLinkId("j(1,7)"), p1.getOriginLinkId(), "Wrong origin link Id");
		Assertions.assertTrue(p1.getDescription().equalsIgnoreCase("Product 1"), "Wrong ProductType description");
		Assertions.assertEquals(1.0, p1.getRequiredCapacity(), MatsimTestUtils.EPSILON, "Wrong capacity.");
	}

	@Test
	void testV2() {
		Receivers receivers = new Receivers();
		try {
		new ReceiversReader(receivers).readFile(utils.getClassInputDirectory() + "receivers_v2_full.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("Should read without exception.");
		}
		/* Receivers */
		Assertions.assertTrue("Chessboard".equalsIgnoreCase(receivers.getDescription()), "Wrong description.");
		Object r = receivers.getAttributes().getAttribute("date");
		Assertions.assertNotNull(r, "No attribute");

		/* Product types */
		Assertions.assertEquals(2L, receivers.getAllProductTypes().size(), "Wrong number of product types.");
		ProductType pt1 = receivers.getProductType(Id.create("P1", ProductType.class));
		Assertions.assertNotNull(pt1, "Could not find ProductType \"P1\"");
		Assertions.assertNotNull(receivers.getProductType(Id.create("P2", ProductType.class)), "Could not find ProductType \"P2\"");

		Assertions.assertTrue(pt1.getDescription().equalsIgnoreCase("Product 1"), "Wrong ProductType description");
		Assertions.assertEquals(1.0, pt1.getRequiredCapacity(), MatsimTestUtils.EPSILON, "Wrong capacity.");

		/* Receiver */
		Assertions.assertEquals(5, receivers.getReceivers().size(), "Wrong number of receivers.");
		Receiver r1 = receivers.getReceivers().get(Id.create("1", Receiver.class));
		Assertions.assertNotNull(r1, "Should find receiver '1'");

		/* Receiver attributes. */
		Assertions.assertFalse(r1.getAttributes().isEmpty(), "Attributes should not be empty.");
		Assertions.assertNotNull(r1.getAttributes().getAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER), "Should find attribute '" + CollaborationUtils.ATTR_GRANDCOALITION_MEMBER + "'");
		Assertions.assertNotNull(r1.getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS), "Should find attribute '" + CollaborationUtils.ATTR_COLLABORATION_STATUS + "'");
		Assertions.assertNotNull(r1.getAttributes().getAttribute(ReceiverUtils.ATTR_RECEIVER_SCORE), "Should find attribute '" + ReceiverUtils.ATTR_RECEIVER_SCORE + "'");

		/* Time window */
		Assertions.assertEquals(2, r1.getSelectedPlan().getTimeWindows().size(), "Wrong number of time windows.");
		TimeWindow t1 = r1.getSelectedPlan().getTimeWindows().get(0);
		Assertions.assertEquals(Time.parseTime("06:00:00"), t1.getStart(), MatsimTestUtils.EPSILON, "Wrong time window start time.");
		Assertions.assertEquals(Time.parseTime("10:00:00"), t1.getEnd(), MatsimTestUtils.EPSILON, "Wrong time window end time.");
		TimeWindow t2 = r1.getSelectedPlan().getTimeWindows().get(1);
		Assertions.assertEquals(Time.parseTime("15:00:00"), t2.getStart(), MatsimTestUtils.EPSILON, "Wrong time window start time.");
		Assertions.assertEquals(Time.parseTime("18:00:00"), t2.getEnd(), MatsimTestUtils.EPSILON, "Wrong time window end time.");

		Assertions.assertEquals(2, r1.getProducts().size(), "Wrong number of products.");

		/* Receiver product */
		ReceiverProduct rp1 = r1.getProduct(Id.create("P1", ProductType.class));
		Assertions.assertNotNull(rp1, "Could not find receiver product \"P1\"");
		Assertions.assertEquals(0.0, rp1.getStockOnHand(), MatsimTestUtils.EPSILON, "Wrong stock on hand.");

		/* Reorder policy */
		ReorderPolicy policy = rp1.getReorderPolicy();
		Assertions.assertNotNull(policy, "Could not find reorder policy.");
		Assertions.assertTrue(policy instanceof SSReorderPolicy, "Wrong policy type");
		Assertions.assertTrue(policy.getPolicyName().equalsIgnoreCase("(s,S)"), "Wrong policy name.");
		Assertions.assertNotNull(policy.getAttributes().getAttribute("s"), "Could not find attribute 's'");
		Assertions.assertNotNull(policy.getAttributes().getAttribute("S"), "Could not find attribute 'S'");

		/* (Receiver)Orders */
		Assertions.assertEquals(5, r1.getPlans().size(), "Wrong number of orders/plans");
		ReceiverPlan plan = r1.getSelectedPlan();
		ReceiverOrder ro = plan.getReceiverOrder(Id.create("Carrier1", Carrier.class));
		Assertions.assertNotNull(ro, "Should find ReceiverOrder.");

		Assertions.assertEquals(ro.getCarrierId(), Id.create("Carrier1", Carrier.class), "Wrong carrier");
		Assertions.assertEquals(ro.getReceiverId(), Id.create("1", Receiver.class), "Wrong receiver");
		Assertions.assertNull(ro.getScore(), "Should not have score now");

		/* Order (items) */
		Assertions.assertEquals(2, ro.getReceiverProductOrders().size(), "Wrong number of order (items)");
		Order o = ro.getReceiverProductOrders().iterator().next();
		Assertions.assertEquals(o.getId(), Id.create("Order11", Order.class), "Wrong order id.");
		Assertions.assertEquals(5000, o.getOrderQuantity(), MatsimTestUtils.EPSILON, "Wrong order quantity.");
		Assertions.assertEquals(o.getProduct().getProductType().getId(), Id.create("P1", ProductType.class), "Wrong product type.");
		Assertions.assertEquals(o.getReceiver().getId(), Id.create("1", Receiver.class), "Wrong receiver.");
		Assertions.assertEquals(Time.parseTime("03:00:00"), o.getServiceDuration(), MatsimTestUtils.EPSILON, "Wrong service duration.");
	}

}
