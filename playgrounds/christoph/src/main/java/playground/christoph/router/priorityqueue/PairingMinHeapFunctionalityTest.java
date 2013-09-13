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

package playground.christoph.router.priorityqueue;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author cdobler
 */
public class PairingMinHeapFunctionalityTest extends MinHeapTest {
	public void testAdd() {
		MinHeap<HeapEntry> pq = new PairingMinHeap<HeapEntry>(10);
		assertEquals(0, pq.size());
		pq.add(new DummyHeapEntry(0), 1.0);
		assertEquals(1, pq.size());
		pq.add(new DummyHeapEntry(1), 2.0);
		assertEquals(2, pq.size());
		pq.add(new DummyHeapEntry(2), 2.0); // different element with same priority
		assertEquals(3, pq.size());
		pq.add(new DummyHeapEntry(2), 3.0); // same element with different priority
		assertEquals(3, pq.size());      	// should not be added!
		assertEquals(3, iteratorElementCount(pq.iterator()));
	}

	public void testAdd_Null() {
		MinHeap<HeapEntry> pq = new BinaryMinHeap<HeapEntry>(10);
		try {
			pq.add(null, 1.0);
			fail("missing NullPointerException.");
		}
		catch (NullPointerException e) {
			log.info("catched expected exception. ", e);
		}
		assertEquals(0, pq.size());
		assertEquals(0, iteratorElementCount(pq.iterator()));
	}

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
		assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.remove());
		assertEquals(2, pq.size());

		pq.add(entry3, 1.0);
		pq.add(entry4, 4.0);
		pq.add(entry5, 9.0);
		assertEquals(5, pq.size());
		assertEqualsHE(entry3, pq.remove());
		assertEqualsHE(entry4, pq.remove());
		assertEqualsHE(entry0, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertEqualsHE(entry5, pq.remove());
		assertEquals(0, pq.size());
		assertNull(pq.remove());
	}
	
	public void testPoll2() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(3);
		DummyHeapEntry entry4 = new DummyHeapEntry(4);
		DummyHeapEntry entry5 = new DummyHeapEntry(5);
		
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.remove());
		assertEquals(2, pq.size());

		pq.add(entry3, 1.0);
		pq.add(entry4, 4.0);
		pq.add(entry5, 9.0);
		assertEquals(5, pq.size());
		assertEqualsHE(entry3, pq.remove());
		assertEqualsHE(entry4, pq.remove());
		assertEqualsHE(entry0, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertEqualsHE(entry5, pq.remove());
		assertEquals(0, pq.size());
		assertNull(pq.remove());
	}

	public void testIterator() {
		MinHeap<HeapEntry> pq = new PairingMinHeap<HeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Collection<HeapEntry> coll = getIteratorCollection(pq.iterator());
		assertEquals(3, coll.size());
		assertTrue(coll.contains(entry0));
		assertTrue(coll.contains(entry1));
		assertTrue(coll.contains(entry2));
		assertFalse(coll.contains(entry3));
	}

	public void testIterator_ConcurrentModification_add() {
		MinHeap<HeapEntry> pq = new PairingMinHeap<HeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HeapEntry> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());

		pq.add(entry3, 4.0);
		assertTrue(iter.hasNext());
		try {
			iter.next();
			fail("missing ConcurrentModificationException");
		}
		catch (ConcurrentModificationException e) {
			log.info("catched expected exception.", e);
		}
		iter = pq.iterator(); // but a new iterator must work again
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
	}

	public void testIterator_ConcurrentModification_poll() {
		MinHeap<HeapEntry> pq = new PairingMinHeap<HeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HeapEntry> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());

		pq.remove();
		assertTrue(iter.hasNext());
		try {
			iter.next();
			fail("missing ConcurrentModificationException");
		}
		catch (ConcurrentModificationException e) {
			log.info("catched expected exception.", e);
		}
		iter = pq.iterator(); // but a new iterator must work again
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
	}

	public void testIterator_ConcurrentModification_remove() {
		MinHeap<HeapEntry> pq = new BinaryMinHeap<HeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HeapEntry> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());

		assertTrue(pq.remove(entry0));
		assertTrue(iter.hasNext());
		try {
			iter.next();
			fail("missing ConcurrentModificationException");
		}
		catch (ConcurrentModificationException e) {
			log.info("catched expected exception.", e);
		}
		iter = pq.iterator(); // but a new iterator must work again
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
		assertFalse(pq.remove(entry0)); // cannot be removed, so it's no change
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
	}

	public void testIterator_RemoveUnsupported() {
		MinHeap<HeapEntry> pq = new BinaryMinHeap<HeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);
		Iterator<HeapEntry> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
		try {
			iter.remove();
			fail("missing UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			log.info("catched expected exception.", e);
		}
	}

	public void testRemove() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		DummyHeapEntry entry3 = new DummyHeapEntry(4);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		Collection<DummyHeapEntry> coll = getIteratorCollection(pq.iterator());
		assertEquals(3, coll.size());
		assertTrue(coll.contains(entry0));
		assertTrue(coll.contains(entry1));
		assertTrue(coll.contains(entry2));
		assertFalse(coll.contains(entry3));

		// remove some element
		assertTrue(pq.remove(entry0));
		assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		assertEquals(2, coll.size());
		assertFalse(coll.contains(entry0));
		assertTrue(coll.contains(entry1));
		assertTrue(coll.contains(entry2));

		// remove the same element again
		assertFalse(pq.remove(entry0));
		assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		assertEquals(2, coll.size());

		// remove null
		assertFalse(pq.remove(null));
		assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		assertEquals(2, coll.size());
		assertTrue(coll.contains(entry1));
		assertTrue(coll.contains(entry2));

		// now poll the pq and ensure, no removed element is returned
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertNull(pq.remove());
	}

	public void testRemoveAndAdd_LowerPriority() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		assertEquals(3, pq.size());

		// test removing an element and adding it with lower priority (=higher value)
		pq.remove(entry0);
		assertEquals(2, pq.size());
		pq.add(entry0, 7.0);
		assertEquals(3, pq.size());
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertEqualsHE(entry0, pq.remove());
		assertNull(pq.remove());
	}

	// increase priority -> decrease key since it is a min-heap
	public void testIncreasePriority() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		assertEquals(3, pq.size());

		// test decreasing an element by increasing priority (=lower value)
		pq.decreaseKey(entry0, 2);
		assertEquals(3, pq.size());
		assertEqualsHE(entry0, pq.remove());
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertNull(pq.remove());
		
		/*
		 * Add two elements with the same priority, then add one with a
		 * lower priority and increase its priority. 
		 */
		pq.add(entry0, 5.0);
		pq.add(entry1, 5.0);
		pq.add(entry2, 6.0);
		assertEquals(3, pq.size());
		pq.decreaseKey(entry2, 4.0);
		assertEquals(3, pq.size());
		assertEqualsHE(entry2, pq.remove());
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry0, pq.remove());
		assertNull(pq.remove());
	}
	
	public void testRemoveAndAdd_HigherPriority() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(5);
		DummyHeapEntry entry1 = new DummyHeapEntry(3);
		DummyHeapEntry entry2 = new DummyHeapEntry(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		assertEquals(3, pq.size());

		// test removing an element and adding it with higher priority (=lower value)
		pq.remove(entry0);
		assertEquals(2, pq.size());
		pq.add(entry0, 2.5);
		assertEquals(3, pq.size());
		assertEqualsHE(entry0, pq.remove());
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertNull(pq.remove());
	}

	public void testEqualCosts() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
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
		assertEqualsHE(entry0, pq.remove());
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertEqualsHE(entry3, pq.remove());
		assertNull(pq.remove());
	}

	public void testOddOrder() {
		MinHeap<DummyHeapEntry> pq = new PairingMinHeap<DummyHeapEntry>(10);
		DummyHeapEntry entry0 = new DummyHeapEntry(0);
		DummyHeapEntry entry1 = new DummyHeapEntry(1);
		DummyHeapEntry entry2 = new DummyHeapEntry(2);
		DummyHeapEntry entry3 = new DummyHeapEntry(3);
		pq.add(entry0, 0.0);
		pq.add(entry3, 3.0);
		pq.add(entry1, 1.0);
		pq.add(entry2, 2.0);
		assertEqualsHE(entry0, pq.remove());
		assertEqualsHE(entry1, pq.remove());
		assertEqualsHE(entry2, pq.remove());
		assertEqualsHE(entry3, pq.remove());
		assertNull(pq.remove());
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
}


