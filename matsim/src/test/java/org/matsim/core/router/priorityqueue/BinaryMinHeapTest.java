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
	
	@Test
	public void testAdd() {
		testAdd(true);
		testAdd(false);
	}
		
	private void testAdd(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		Assert.assertEquals(0, pq.size());
		pq.add(new DummyHeapEntry(0), 1.0);
		Assert.assertEquals(1, pq.size());
		pq.add(new DummyHeapEntry(1), 2.0);
		Assert.assertEquals(2, pq.size());
		pq.add(new DummyHeapEntry(2), 2.0); // different element with same priority
		Assert.assertEquals(3, pq.size());
		pq.add(new DummyHeapEntry(2), 3.0); // same element with different priority
		Assert.assertEquals(3, pq.size());      	// should not be added!
		Assert.assertEquals(3, iteratorElementCount(pq.iterator()));
	}

	@Test
	public void testAdd_Null() {
		testAdd_Null(true);
		testAdd_Null(false);
	}
	
	private void testAdd_Null(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testPoll(true);
		testPoll(false);
	}
	
	private void testPoll(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testPoll2(true);
		testPoll2(false);
	}
	
	private void testPoll2(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testIterator(true);
		testIterator(false);
	}
	
	private void testIterator(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Collection<HasIndex> coll = getIteratorCollection(pq.iterator());
		Assert.assertEquals(3, coll.size());
		Assert.assertTrue(coll.contains(entry0));
		Assert.assertTrue(coll.contains(entry1));
		Assert.assertTrue(coll.contains(entry2));
		Assert.assertFalse(coll.contains(entry3));
	}

	@Test
	public void testIterator_ConcurrentModification_add() {
		testIterator_ConcurrentModification_add(true);
		testIterator_ConcurrentModification_add(false);
	}
	
	private void testIterator_ConcurrentModification_add(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HasIndex> iter = pq.iterator();
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
		testIterator_ConcurrentModification_poll(true);
		testIterator_ConcurrentModification_poll(false);
	}
	
	private void testIterator_ConcurrentModification_poll(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HasIndex> iter = pq.iterator();
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
		testIterator_ConcurrentModification_remove(true);
		testIterator_ConcurrentModification_remove(false);
	}
	
	private void testIterator_ConcurrentModification_remove(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HasIndex> iter = pq.iterator();
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
		testIterator_RemoveUnsupported(true);
		testIterator_RemoveUnsupported(false);
	}
	
	private void testIterator_RemoveUnsupported(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HasIndex> iter = pq.iterator();
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
		testRemove(true);
		testRemove(false);
	}
	
	private void testRemove(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		Collection<HasIndex> coll = getIteratorCollection(pq.iterator());
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
		testRemoveAndAdd_LowerPriority(true);
		testRemoveAndAdd_LowerPriority(false);
	}
	
	private void testRemoveAndAdd_LowerPriority(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testIncreasePriority(true);
		testIncreasePriority(false);
	}
	
	private void testIncreasePriority(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
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
		testRemoveAndAdd_HigherPriority(true);
		testRemoveAndAdd_HigherPriority(false);
	}
	
	private void testRemoveAndAdd_HigherPriority(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testEqualCosts(true);
		testEqualCosts(false);
	}
	
	private void testEqualCosts(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
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
		testEqualCosts2(true);
		testEqualCosts2(false);
	}
	
	private void testEqualCosts2(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testExceedCapacity(true);
		testEqualCosts2(false);
	}
	
	private void testExceedCapacity(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		testOddOrder(true);
		testOddOrder(false);
	}
	
	private void testOddOrder(boolean classicalRemove) {
		MinHeap<HasIndex> pq = createMinHeap(classicalRemove);
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
		BinaryMinHeap<HasIndex> pq = new BinaryMinHeap<HasIndex>(10, BinaryMinHeap.defaultFanout, classicalRemove);
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