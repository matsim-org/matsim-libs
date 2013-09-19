/* *********************************************************************** *
 * project: org.matsim.*
 * BinaryMinHeapTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.router.priorityqueue;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author cdobler
 */
public class BinaryMinHeapTest {
	
	protected static final Logger log = Logger.getLogger(BinaryMinHeapTest.class);
	
	private int maxElements = 10;
	
	@Test
	public void testAdd() {
		testAdd(createMinHeap(true));
		testAdd(createMinHeap(false));
		testAdd(createWrappedMinHeap(true));
		testAdd(createWrappedMinHeap(false));
	}
	
	private void testAdd(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		
		Assert.assertEquals(0, pq.size());
		pq.add(entry0, 1.0);
		Assert.assertEquals(1, pq.size());
		pq.add(entry1, 2.0);
		Assert.assertEquals(2, pq.size());
		pq.add(entry2, 2.0); // different element with same priority
		Assert.assertEquals(3, pq.size());
		pq.add(entry2, 3.0); // same element with different priority
		Assert.assertEquals(3, pq.size());      	// should not be added!
		Assert.assertEquals(3, iteratorElementCount(pq.iterator()));
	}

	@Test
	public void testAdd_Null() {
		testAdd_Null(createMinHeap(true));
		testAdd_Null(createMinHeap(false));
		testAdd_Null(createWrappedMinHeap(true));
		testAdd_Null(createWrappedMinHeap(false));
	}
	
	private void testAdd_Null(MinHeap<HasIndex> pq) {
		try {
			pq.add(null, 1.0);
			Assert.fail("missing NullPointerException.");
		}
		catch (NullPointerException e) {
			log.info("catched expected exception. ", e);
		}
		Assert.assertEquals(0, pq.size());
		Assert.assertEquals(0, iteratorElementCount(pq.iterator()));
	}

	@Test
	public void testPoll() {
		testPoll(createMinHeap(true));
		testPoll(createMinHeap(false));
		testPoll(createWrappedMinHeap(true));
		testPoll(createWrappedMinHeap(false));
	}
	
	private void testPoll(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(1);
		DummyHeapEntry entry4 = new DummyHeapEntry(4);
		DummyHeapEntry entry5 = new DummyHeapEntry(9);
		
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.poll());
		Assert.assertEquals(2, pq.size());

		pq.add(entry3, 1.0);
		pq.add(entry4, 4.0);
		pq.add(entry5, 9.0);
		Assert.assertEquals(5, pq.size());
		assertEqualsHE(entry3, pq.poll());
		assertEqualsHE(entry4, pq.poll());
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry5, pq.poll());
		Assert.assertEquals(0, pq.size());
		Assert.assertNull(pq.poll());
	}
	
	@Test
	public void testPoll2() {
		testPoll2(createMinHeap(true));
		testPoll2(createMinHeap(false));
		testPoll2(createWrappedMinHeap(true));
		testPoll2(createWrappedMinHeap(false));
		
	}
	
	private void testPoll2(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(3);
		DummyHeapEntry entry4 = new DummyHeapEntry(4);
		DummyHeapEntry entry5 = new DummyHeapEntry(5);
		
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.poll());
		Assert.assertEquals(2, pq.size());

		pq.add(entry3, 1.0);
		pq.add(entry4, 4.0);
		pq.add(entry5, 9.0);
		Assert.assertEquals(5, pq.size());
		assertEqualsHE(entry3, pq.poll());
		assertEqualsHE(entry4, pq.poll());
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry5, pq.poll());
		Assert.assertEquals(0, pq.size());
		Assert.assertNull(pq.poll());
	}

	@Test
	public void testIterator() {
		testIterator(createMinHeap(true));
		testIterator(createMinHeap(false));
		testIterator(createWrappedMinHeap(true));
		testIterator(createWrappedMinHeap(false));
	}
	
	private void testIterator(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Collection<?> coll = getIteratorCollection(pq.iterator());
		Assert.assertEquals(3, coll.size());
		Assert.assertTrue(coll.contains(entry0));
		Assert.assertTrue(coll.contains(entry1));
		Assert.assertTrue(coll.contains(entry2));
		Assert.assertFalse(coll.contains(entry3));
	}

	@Test
	public void testIterator_ConcurrentModification_add() {
		testIterator_ConcurrentModification_add(createMinHeap(true));
		testIterator_ConcurrentModification_add(createMinHeap(false));
		testIterator_ConcurrentModification_add(createWrappedMinHeap(true));
		testIterator_ConcurrentModification_add(createWrappedMinHeap(false));
	}
	
	private void testIterator_ConcurrentModification_add(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<?> iter = pq.iterator();
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());

		pq.add(entry3, 4.0);
		Assert.assertTrue(iter.hasNext());
		try {
			iter.next();
			Assert.fail("missing ConcurrentModificationException");
		}
		catch (ConcurrentModificationException e) {
			log.info("catched expected exception.", e);
		}
		iter = pq.iterator(); // but a new iterator must work again
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());
	}

	@Test
	public void testIterator_ConcurrentModification_poll() {
		testIterator_ConcurrentModification_poll(createMinHeap(true));
		testIterator_ConcurrentModification_poll(createMinHeap(false));
		testIterator_ConcurrentModification_poll(createWrappedMinHeap(true));
		testIterator_ConcurrentModification_poll(createWrappedMinHeap(false));
	}
	
	private void testIterator_ConcurrentModification_poll(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<?> iter = pq.iterator();
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());

		pq.poll();
		Assert.assertTrue(iter.hasNext());
		try {
			iter.next();
			Assert.fail("missing ConcurrentModificationException");
		}
		catch (ConcurrentModificationException e) {
			log.info("catched expected exception.", e);
		}
		iter = pq.iterator(); // but a new iterator must work again
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());
	}

	@Test
	public void testIterator_ConcurrentModification_remove() {
		testIterator_ConcurrentModification_remove(createMinHeap(true));
		testIterator_ConcurrentModification_remove(createMinHeap(false));
		testIterator_ConcurrentModification_remove(createWrappedMinHeap(true));
		testIterator_ConcurrentModification_remove(createWrappedMinHeap(false));
	}
	
	private void testIterator_ConcurrentModification_remove(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<?> iter = pq.iterator();
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());

		Assert.assertTrue(pq.remove(entry0));
		Assert.assertTrue(iter.hasNext());
		try {
			iter.next();
			Assert.fail("missing ConcurrentModificationException");
		}
		catch (ConcurrentModificationException e) {
			log.info("catched expected exception.", e);
		}
		iter = pq.iterator(); // but a new iterator must work again
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());
		Assert.assertFalse(pq.remove(entry0)); // cannot be removed, so it's no change
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());
	}

	@Test
	public void testIterator_RemoveUnsupported() {
		testIterator_RemoveUnsupported(createMinHeap(true));
		testIterator_RemoveUnsupported(createMinHeap(false));
		testIterator_RemoveUnsupported(createWrappedMinHeap(true));
		testIterator_RemoveUnsupported(createWrappedMinHeap(false));
	}
	
	private void testIterator_RemoveUnsupported(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<?> iter = pq.iterator();
		Assert.assertTrue(iter.hasNext());
		Assert.assertNotNull(iter.next());
		try {
			iter.remove();
			Assert.fail("missing UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	public void testRemove() {
		testRemove(createMinHeap(true));
		testRemove(createMinHeap(false));
		testRemove(createWrappedMinHeap(true));
		testRemove(createWrappedMinHeap(false));
	}
	
	private void testRemove(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		Collection<?> coll = getIteratorCollection(pq.iterator());
		Assert.assertEquals(3, coll.size());
		Assert.assertTrue(coll.contains(entry0));
		Assert.assertTrue(coll.contains(entry1));
		Assert.assertTrue(coll.contains(entry2));
		Assert.assertFalse(coll.contains(entry3));

		// remove some element
		Assert.assertTrue(pq.remove(entry0));
		Assert.assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		Assert.assertEquals(2, coll.size());
		Assert.assertFalse(coll.contains(entry0));
		Assert.assertTrue(coll.contains(entry1));
		Assert.assertTrue(coll.contains(entry2));

		// remove the same element again
		Assert.assertFalse(pq.remove(entry0));
		Assert.assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		Assert.assertEquals(2, coll.size());

		// remove null
		Assert.assertFalse(pq.remove(null));
		Assert.assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		Assert.assertEquals(2, coll.size());
		Assert.assertTrue(coll.contains(entry1));
		Assert.assertTrue(coll.contains(entry2));

		// now poll the pq and ensure, no removed element is returned
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		Assert.assertNull(pq.poll());
	}

	@Test
	public void testRemoveAndAdd_LowerPriority() {
		testRemoveAndAdd_LowerPriority(createMinHeap(true));
		testRemoveAndAdd_LowerPriority(createMinHeap(false));
		testRemoveAndAdd_LowerPriority(createWrappedMinHeap(true));
		testRemoveAndAdd_LowerPriority(createWrappedMinHeap(false));
	}
	
	private void testRemoveAndAdd_LowerPriority(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		Assert.assertEquals(3, pq.size());

		// test removing an element and adding it with lower priority (=higher value)
		pq.remove(entry0);
		Assert.assertEquals(2, pq.size());
		pq.add(entry0, 7.0);
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry0, pq.poll());
		Assert.assertNull(pq.poll());
	}

	@Test
	// increase priority -> decrease key since it is a min-heap
	public void testIncreasePriority() {
		testIncreasePriority(createMinHeap(true));
		testIncreasePriority(createMinHeap(false));
		testIncreasePriority(createWrappedMinHeap(true));
		testIncreasePriority(createWrappedMinHeap(false));
	}
	
	private void testIncreasePriority(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);

		/*
		 * Only for WrappedBinaryMinHeap: ensure that the indices are in the same order
		 * as used above for the array based implementation.
		 */
		if (pq instanceof WrappedBinaryMinHeap) {
			pq.add(entry1, Double.MAX_VALUE);
			pq.add(entry0, Double.MAX_VALUE);
			pq.add(entry2, Double.MAX_VALUE);
			while(!pq.isEmpty()) pq.poll();
		}
		
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		
		Assert.assertEquals(3, pq.size());

		// test decreasing an element by increasing priority (=lower value)
		pq.decreaseKey(entry0, 2);
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		Assert.assertNull(pq.poll());
		
		/*
		 * Add two elements with the same priority, then add one with a
		 * lower priority and increase its priority. 
		 */
		pq.add(entry0, 5.0);
		pq.add(entry1, 5.0);
		pq.add(entry2, 6.0);
		Assert.assertEquals(3, pq.size());
		pq.decreaseKey(entry2, 4.0);
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry0, pq.poll());
		Assert.assertNull(pq.poll());
	}
	
	@Test
	public void testRemoveAndAdd_HigherPriority() {
		testRemoveAndAdd_HigherPriority(createMinHeap(true));
		testRemoveAndAdd_HigherPriority(createMinHeap(false));
		testRemoveAndAdd_HigherPriority(createWrappedMinHeap(true));
		testRemoveAndAdd_HigherPriority(createWrappedMinHeap(false));
	}
	
	private void testRemoveAndAdd_HigherPriority(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		Assert.assertEquals(3, pq.size());

		// test removing an element and adding it with higher priority (=lower value)
		pq.remove(entry0);
		Assert.assertEquals(2, pq.size());
		pq.add(entry0, 2.5);
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		Assert.assertNull(pq.poll());
	}

	@Test
	public void testEqualCosts() {
		testEqualCosts(createMinHeap(true));
		testEqualCosts(createMinHeap(false));
		testEqualCosts(createWrappedMinHeap(true));
		testEqualCosts(createWrappedMinHeap(false));
	}
	
	private void testEqualCosts(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		
		/*
		 * Only for WrappedBinaryMinHeap: ensure that the indices are in the same order
		 * as used above for the array based implementation.
		 */
		if (pq instanceof WrappedBinaryMinHeap) {
			pq.add(entry0, Double.MAX_VALUE);
			pq.add(entry1, Double.MAX_VALUE);
			pq.add(entry2, Double.MAX_VALUE);
			pq.add(entry3, Double.MAX_VALUE);
			while(!pq.isEmpty()) pq.poll();
		}
		
		pq.add(entry2, 5.0);
		pq.add(entry3, 5.0);
		pq.add(entry1, 5.0);
		pq.add(entry0, 5.0);
		Assert.assertEquals(4, pq.size());
		assertEqualsHE(entry0, pq.poll());
		Assert.assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.poll());
		Assert.assertEquals(2, pq.size());
		assertEqualsHE(entry2, pq.poll());
		Assert.assertEquals(1, pq.size());
		assertEqualsHE(entry3, pq.poll());
		Assert.assertEquals(0, pq.size());
		Assert.assertNull(pq.poll());
	}

	@Test
	public void testEqualCosts2() {
		testEqualCosts2(createMinHeap(true));
		testEqualCosts2(createMinHeap(false));
		testEqualCosts2(createWrappedMinHeap(true));
		testEqualCosts2(createWrappedMinHeap(false));
	}
	
	private void testEqualCosts2(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(3);
		DummyHeapEntry entry4 = new DummyHeapEntry(4);
		DummyHeapEntry entry5 = new DummyHeapEntry(5);
		DummyHeapEntry entry6 = new DummyHeapEntry(6);
		DummyHeapEntry entry7 = new DummyHeapEntry(7);
		DummyHeapEntry entry8 = new DummyHeapEntry(8);
		DummyHeapEntry entry9 = new DummyHeapEntry(9);
				
		/*
		 * Only for WrappedBinaryMinHeap: ensure that the indices are in the same order
		 * as used above for the array based implementation.
		 */
		if (pq instanceof WrappedBinaryMinHeap) {
			pq.add(entry0, Double.MAX_VALUE);
			pq.add(entry1, Double.MAX_VALUE);
			pq.add(entry2, Double.MAX_VALUE);
			pq.add(entry3, Double.MAX_VALUE);
			pq.add(entry4, Double.MAX_VALUE);
			pq.add(entry5, Double.MAX_VALUE);
			pq.add(entry6, Double.MAX_VALUE);
			pq.add(entry7, Double.MAX_VALUE);
			pq.add(entry8, Double.MAX_VALUE);
			pq.add(entry9, Double.MAX_VALUE);
			while(!pq.isEmpty()) pq.poll();
		}
		
		pq.add(entry3, 5.0);
		pq.add(entry7, 5.0);
		pq.add(entry2, 5.0);
		pq.add(entry4, 5.0);
		pq.add(entry1, 5.0);
		pq.add(entry9, 5.0);
		pq.add(entry6, 5.0);
		pq.add(entry5, 5.0);
		pq.add(entry0, 5.0);
		pq.add(entry8, 5.0);
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry3, pq.poll());
		assertEqualsHE(entry4, pq.poll());
		assertEqualsHE(entry5, pq.poll());
		assertEqualsHE(entry6, pq.poll());
		assertEqualsHE(entry7, pq.poll());
		assertEqualsHE(entry8, pq.poll());
		assertEqualsHE(entry9, pq.poll());
		Assert.assertNull(pq.poll());
	}
	
	@Test
	public void testExceedCapacity() {
		testExceedCapacity(createMinHeap(true));
		testExceedCapacity(createMinHeap(false));
		testExceedCapacity(createWrappedMinHeap(true));
		testExceedCapacity(createWrappedMinHeap(false));
	}
	
	private void testExceedCapacity(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(3);
		DummyHeapEntry entry4 = new DummyHeapEntry(4);
		DummyHeapEntry entry5 = new DummyHeapEntry(5);
		DummyHeapEntry entry6 = new DummyHeapEntry(6);
		DummyHeapEntry entry7 = new DummyHeapEntry(7);
		DummyHeapEntry entry8 = new DummyHeapEntry(8);
		DummyHeapEntry entry9 = new DummyHeapEntry(9);
		DummyHeapEntry entry10 = new DummyHeapEntry(10);
		pq.add(entry0, 5.0);
		pq.add(entry1, 5.0);
		pq.add(entry2, 5.0);
		pq.add(entry3, 5.0);
		pq.add(entry4, 5.0);
		pq.add(entry5, 5.0);
		pq.add(entry6, 5.0);
		pq.add(entry7, 5.0);
		pq.add(entry8, 5.0);
		pq.add(entry9, 5.0);
		
		// this entry should exceed the heaps capacity
		try {
			pq.add(entry10, 5.0);
			Assert.fail("missing NullPointerException.");
		}
		catch (RuntimeException e) {
			log.info("catched expected exception. ", e);
		}
	}
	
	@Test
	public void testOddOrder() {
		testOddOrder(createMinHeap(true));
		testOddOrder(createMinHeap(false));
		testOddOrder(createWrappedMinHeap(true));
		testOddOrder(createWrappedMinHeap(false));
	}
	
	private void testOddOrder(MinHeap<HasIndex> pq) {
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(3);
		pq.add(entry0, 0.0);
		pq.add(entry3, 3.0);
		pq.add(entry1, 1.0);
		pq.add(entry2, 2.0);
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry3, pq.poll());
		Assert.assertNull(pq.poll());
	}
	
	private MinHeap<HasIndex> createMinHeap(boolean classicalRemove) {
		MinHeap<HasIndex> pq = new BinaryMinHeap<HasIndex>(maxElements, BinaryMinHeap.defaultFanout, classicalRemove);
		return pq;
	}

	private MinHeap<HasIndex> createWrappedMinHeap(boolean classicalRemove) {
		MinHeap<HasIndex> pq = new WrappedBinaryMinHeap<HasIndex>(maxElements, BinaryMinHeap.defaultFanout, classicalRemove);
		return pq;
	}
	
	private int iteratorElementCount(final Iterator<?> iterator) {
		int cnt = 0;
		while (iterator.hasNext()) {
			cnt++;
			iterator.next();
		}
		return cnt;
	}
	
	private <T> Collection<T> getIteratorCollection(final Iterator<T> iterator) {
		LinkedList<T> list = new LinkedList<T>();
		while (iterator.hasNext()) {
			list.add(iterator.next());
		}
		return list;
	}
	
	private void assertEqualsHE(HasIndex e1, HasIndex e2) {
		Assert.assertEquals(e1.getArrayIndex(), e2.getArrayIndex());
		Assert.assertEquals(e1, e2);
	}
	
	private static class DummyHeapEntry implements HasIndex {
		
		final int index;
		
		public DummyHeapEntry(int index) {
			this.index = index;
		}
		
		@Override
		public int getArrayIndex() {
			return index;
		}
		
		@Override
		public String toString() {
			return String.valueOf(this.index);
		}
	}
}