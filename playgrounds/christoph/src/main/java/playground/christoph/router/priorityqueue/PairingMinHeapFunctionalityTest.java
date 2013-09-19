/* *********************************************************************** *
 * project: org.matsim.*
 * PairingMinHeapFunctionalityTest.java
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

package playground.christoph.router.priorityqueue;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.router.priorityqueue.MinHeap;

/**
 * @author cdobler
 */
public class PairingMinHeapFunctionalityTest {
	
	private final static Logger log = Logger.getLogger(PairingMinHeapFunctionalityTest.class);
	
	@Test
	public void testAdd() {
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
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
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry2, 5.0);
		pq.add(entry3, 5.0);
		assertEqualsHE(entry2, pq.peek());
		pq.add(entry1, 5.0);
		assertEqualsHE(entry1, pq.peek());
		pq.add(entry0, 5.0);
		assertEqualsHE(entry0, pq.peek());
		assertEqualsHE(entry0, pq.poll());
		assertEqualsHE(entry1, pq.poll());
		assertEqualsHE(entry2, pq.poll());
		assertEqualsHE(entry3, pq.poll());
		Assert.assertNull(pq.poll());
	}

	@Test
	public void testOddOrder() {
		MinHeap<HasIndex> pq = new PairingMinHeap<HasIndex>(10);
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

