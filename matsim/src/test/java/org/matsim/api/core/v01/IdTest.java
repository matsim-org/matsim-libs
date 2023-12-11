/* *********************************************************************** *
 * project: org.matsim.*
 * IdTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.api.core.v01;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.List;

	public class IdTest {

	private final static Logger LOG = LogManager.getLogger(IdTest.class);

	 @Test
	 void testConstructor() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TLink> linkId2 = Id.create("2", TLink.class);
		
		Assertions.assertEquals("1", linkId1.toString());
		Assertions.assertEquals("2", linkId2.toString());
	}

	 @Test
	 void testIdConstructor() {
		Id<TNode> nodeId1 = Id.create("1", TNode.class);
		Id<TLink> linkId1 = Id.create(nodeId1, TLink.class);
		
		Assertions.assertEquals("1", linkId1.toString());
	}

	 @Test
	 void testIdConstructor_Null() {
		Id<TLink> linkId1 = Id.create((Id) null, TLink.class);
		Assertions.assertNull(linkId1);
	}

	 @Test
	 void testObjectIdentity_cache() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TLink> linkId2 = Id.create("2", TLink.class);
		Id<TLink> linkId1again = Id.create("1", TLink.class);
		
		Assertions.assertTrue(linkId1 == linkId1again);
		Assertions.assertFalse(linkId1 == linkId2);
	}

	 @Test
	 void testObjectIdentity_types() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TNode> nodeId1 = Id.create("1", TNode.class);
		
		Assertions.assertFalse((Id) linkId1 == (Id) nodeId1);
	}

	 @Test
	 void testCompareTo() {
		Id<TLink> linkId1 = Id.create("1", TLink.class);
		Id<TLink> linkId2 = Id.create("2", TLink.class);
		Id<TLink> linkId1again = Id.create("1", TLink.class);
		Id<TNode> nodeId1 = Id.create("1", TNode.class);

		Assertions.assertTrue(linkId1.compareTo(linkId2) < 0);
		Assertions.assertTrue(linkId1.compareTo(linkId1) == 0);
		Assertions.assertTrue(linkId1.compareTo(linkId1again) == 0);
		Assertions.assertTrue(linkId2.compareTo(linkId1) > 0);

//		try {
//			Assert.assertTrue(linkId1.compareTo((Id) nodeId1) == 0);
//			Assert.fail("expected exception, got none");
//		} catch (IllegalArgumentException e) {
//			// expected exception
//		} // FIXME temporarily deactivated
	}

	 @Test
	 void testResetCaches() {
		Id.create("1", TLink.class);
		Id.create("2", TLink.class);
		int count = Id.getNumberOfIds(TLink.class);
		Assertions.assertTrue(count > 0); // it might be > 2 if other tests have run before creating Ids of this class
		Id.resetCaches();
		Assertions.assertEquals(0, Id.getNumberOfIds(TLink.class));
		Id.create("1", TLink.class);
		Id.create("2", TLink.class);
		Id.create("3", TLink.class);
		Assertions.assertEquals(3, Id.getNumberOfIds(TLink.class));
	}

	 @Test
	 void testResetCaches_onlyFromJUnit() throws InterruptedException {
		Id.create("1", TLink.class);
		int countBefore = Id.getNumberOfIds(TLink.class);
		Assertions.assertTrue(countBefore > 0);

		Runnable runnable = () -> {
			Id.resetCaches();
		};

		List<Tuple<Thread, Throwable>> caughtExceptions = new ArrayList<>();

		Thread th = new Thread(runnable);
		th.setUncaughtExceptionHandler((t, e) -> caughtExceptions.add(new Tuple<>(t, e)));
		th.start();
		th.join();

		for (Tuple<Thread, Throwable> t : caughtExceptions) {
			LOG.info("Caught exception: " + t.getSecond().getMessage());
		}

		Assertions.assertFalse(caughtExceptions.isEmpty(), "There should have be an exception!");
		int countAfter = Id.getNumberOfIds(TLink.class);
		Assertions.assertEquals(countBefore, countAfter, "The number of created Ids should not have changed.");
	}

	private static class TLink {}
	private static class TNode {}

}
