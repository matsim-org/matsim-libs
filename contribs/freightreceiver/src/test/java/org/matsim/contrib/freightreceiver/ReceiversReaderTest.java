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

package org.matsim.contrib.freightreceiver;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freightreceiver.collaboration.CollaborationUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiversReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This is just a simplified version of {@link #testV2()} with the main
	 * difference being that the input file for this test was actually
	 * generated from the {@link ReceiversWriterTest#testV2()} class
	 * whereas the more complete test below was inherited (and manually adapted)
	 * from the original DFG code.
	 */
	@Test
	public void testBasicV2(){
		Receivers receivers = new Receivers();
		try {
			new ReceiversReader(receivers).readFile(utils.getClassInputDirectory() + "receivers_v2_basic.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should read without exception.");
		}

		/* Check one product type completely. */
		ProductType p1 = receivers.getProductType(Id.create("P1", ProductType.class));
		Assert.assertNotNull("Product type should exists", p1);
		Assert.assertEquals("Wrong origin link Id", Id.createLinkId("j(1,7)"), p1.getOriginLinkId());
		Assert.assertTrue("Wrong ProductType description", p1.getDescription().equalsIgnoreCase("Product 1"));
		Assert.assertEquals("Wrong capacity.", 1.0, p1.getRequiredCapacity(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testV2() {
		Receivers receivers = new Receivers();
		try {
		new ReceiversReader(receivers).readFile(utils.getClassInputDirectory() + "receivers_v2_full.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should read without exception.");
		}
		/* Receivers */
		Assert.assertTrue("Wrong description.", "Chessboard".equalsIgnoreCase(receivers.getDescription()));
		Object r = receivers.getAttributes().getAttribute("date");
		Assert.assertNotNull("No attribute", r);

		/* Product types */
		Assert.assertEquals("Wrong number of product types.", 2L, receivers.getAllProductTypes().size());
		ProductType pt1 = receivers.getProductType(Id.create("P1", ProductType.class));
		Assert.assertNotNull("Could not find ProductType \"P1\"", pt1);
		Assert.assertNotNull("Could not find ProductType \"P2\"", receivers.getProductType(Id.create("P2", ProductType.class)));

		Assert.assertTrue("Wrong ProductType description", pt1.getDescription().equalsIgnoreCase("Product 1"));
		Assert.assertEquals("Wrong capacity.", 1.0, pt1.getRequiredCapacity(), MatsimTestUtils.EPSILON);

		/* Receiver */
		Assert.assertEquals("Wrong number of receivers.", 5, receivers.getReceivers().size());
		Receiver r1 = receivers.getReceivers().get(Id.create("1", Receiver.class));
		Assert.assertNotNull("Should find receiver '1'", r1);

		/* Receiver attributes. */
		Assert.assertFalse("Attributes should not be empty.", r1.getAttributes().isEmpty());
		Assert.assertNotNull("Should find attribute '" + CollaborationUtils.ATTR_GRANDCOALITION_MEMBER + "'", r1.getAttributes().getAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER));
		Assert.assertNotNull("Should find attribute '" + CollaborationUtils.ATTR_COLLABORATION_STATUS + "'", r1.getAttributes().getAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS));
		Assert.assertNotNull("Should find attribute '" + ReceiverUtils.ATTR_RECEIVER_SCORE + "'", r1.getAttributes().getAttribute(ReceiverUtils.ATTR_RECEIVER_SCORE));

		/* Time window */
		Assert.assertEquals("Wrong number of time windows.", 2, r1.getSelectedPlan().getTimeWindows().size());
		TimeWindow t1 = r1.getSelectedPlan().getTimeWindows().get(0);
		Assert.assertEquals("Wrong time window start time.", Time.parseTime("06:00:00"), t1.getStart(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong time window end time.", Time.parseTime("10:00:00"), t1.getEnd(), MatsimTestUtils.EPSILON);
		TimeWindow t2 = r1.getSelectedPlan().getTimeWindows().get(1);
		Assert.assertEquals("Wrong time window start time.", Time.parseTime("15:00:00"), t2.getStart(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong time window end time.", Time.parseTime("18:00:00"), t2.getEnd(), MatsimTestUtils.EPSILON);

		Assert.assertEquals("Wrong number of products.", 2, r1.getProducts().size());

		/* Receiver product */
		ReceiverProduct rp1 = r1.getProduct(Id.create("P1", ProductType.class));
		Assert.assertNotNull("Could not find receiver product \"P1\"", rp1);
		Assert.assertEquals("Wrong stock on hand.", 0.0, rp1.getStockOnHand(), MatsimTestUtils.EPSILON);

		/* Reorder policy */
		ReorderPolicy policy = rp1.getReorderPolicy();
		Assert.assertNotNull("Could not find reorder policy.", policy);
		Assert.assertTrue("Wrong policy type", policy instanceof SSReorderPolicy);
		Assert.assertTrue("Wrong policy name.", policy.getPolicyName().equalsIgnoreCase("(s,S)"));
		Assert.assertNotNull("Could not find attribute 's'", policy.getAttributes().getAttribute("s"));
		Assert.assertNotNull("Could not find attribute 'S'", policy.getAttributes().getAttribute("S"));

		/* (Receiver)Orders */
		Assert.assertEquals("Wrong number of orders/plans", 5, r1.getPlans().size());
		ReceiverPlan plan = r1.getSelectedPlan();
		ReceiverOrder ro = plan.getReceiverOrder(Id.create("Carrier1", Carrier.class));
		Assert.assertNotNull("Should find ReceiverOrder.", ro);

		Assert.assertEquals("Wrong carrier", ro.getCarrierId(), Id.create("Carrier1", Carrier.class));
		Assert.assertEquals("Wrong receiver", ro.getReceiverId(), Id.create("1", Receiver.class));
		Assert.assertNull("Should not have score now", ro.getScore());

		/* Order (items) */
		Assert.assertEquals("Wrong number of order (items)", 2, ro.getReceiverProductOrders().size());
		Order o = ro.getReceiverProductOrders().iterator().next();
		Assert.assertEquals("Wrong order id.", o.getId(), Id.create("Order11", Order.class));
		Assert.assertEquals("Wrong order quantity.", 5000, o.getOrderQuantity(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong product type.", o.getProduct().getProductType().getId(), Id.create("P1", ProductType.class));
		Assert.assertEquals("Wrong receiver.", o.getReceiver().getId(), Id.create("1", Receiver.class));
		Assert.assertEquals("Wrong service duration.", Time.parseTime("03:00:00"), o.getServiceDuration(), MatsimTestUtils.EPSILON);
	}

}
