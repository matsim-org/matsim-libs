/* *********************************************************************** *
 * project: org.matsim.*
 * PseudoRemovePriorityQueueTest.java
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

package org.matsim.core.utils.collections;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser
 */
public class PseudoRemovePriorityQueueTest {

	private static final Logger log = LogManager.getLogger(PseudoRemovePriorityQueueTest.class);

	@Test
	void testAdd() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		assertEquals(0, pq.size());
		pq.add(Integer.valueOf(1), 1.0);
		assertEquals(1, pq.size());
		pq.add(Integer.valueOf(2), 2.0);
		assertEquals(2, pq.size());
		pq.add(Integer.valueOf(3), 2.0); // different element with same priority
		assertEquals(3, pq.size());
		pq.add(Integer.valueOf(3), 3.0); // same element with different priority
		assertEquals(3, pq.size());      // should not be added!
		assertEquals(3, iteratorElementCount(pq.iterator()));
	}

	@Test
	void testAdd_Null() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
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

	@Test
	void testPoll() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);
		assertEquals(3, pq.size());
		assertEquals(Integer.valueOf(3), pq.poll());
		assertEquals(2, pq.size());
		pq.add(Integer.valueOf(1), 1.0);
		pq.add(Integer.valueOf(4), 4.0);
		pq.add(Integer.valueOf(9), 9.0);
		assertEquals(5, pq.size());
		assertEquals(Integer.valueOf(1), pq.poll());
		assertEquals(Integer.valueOf(4), pq.poll());
		assertEquals(Integer.valueOf(5), pq.poll());
		assertEquals(Integer.valueOf(6), pq.poll());
		assertEquals(Integer.valueOf(9), pq.poll());
		assertEquals(0, pq.size());
		assertNull(pq.poll());
	}

	@Test
	void testIterator() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);
		Collection<Integer> coll = getIteratorCollection(pq.iterator());
		assertEquals(3, coll.size());
		assertTrue(coll.contains(Integer.valueOf(5)));
		assertTrue(coll.contains(Integer.valueOf(3)));
		assertTrue(coll.contains(Integer.valueOf(6)));
		assertFalse(coll.contains(Integer.valueOf(4)));
	}

	@Test
	void testIterator_ConcurrentModification_add() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);
		Iterator<Integer> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());

		pq.add(Integer.valueOf(4), 4.0);
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

	@Test
	void testIterator_ConcurrentModification_poll() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);
		Iterator<Integer> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());

		pq.poll();
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

	@Test
	void testIterator_ConcurrentModification_remove() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);
		Iterator<Integer> iter = pq.iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());

		assertTrue(pq.remove(Integer.valueOf(5)));
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
		assertFalse(pq.remove(Integer.valueOf(5))); // cannot be removed, so it's no change
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
	}

	@Test
	void testIterator_RemoveUnsupported() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);
		Iterator<Integer> iter = pq.iterator();
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

	@Test
	void testRemove() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);

		Collection<Integer> coll = getIteratorCollection(pq.iterator());
		assertEquals(3, coll.size());
		assertTrue(coll.contains(Integer.valueOf(5)));
		assertTrue(coll.contains(Integer.valueOf(3)));
		assertTrue(coll.contains(Integer.valueOf(6)));
		assertFalse(coll.contains(Integer.valueOf(4)));

		// remove some element
		assertTrue(pq.remove(Integer.valueOf(5)));
		assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		assertEquals(2, coll.size());
		assertFalse(coll.contains(Integer.valueOf(5)));
		assertTrue(coll.contains(Integer.valueOf(3)));
		assertTrue(coll.contains(Integer.valueOf(6)));

		// remove the same element again
		assertFalse(pq.remove(Integer.valueOf(5)));
		assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		assertEquals(2, coll.size());

		// remove null
		assertFalse(pq.remove(null));
		assertEquals(2, pq.size());
		coll = getIteratorCollection(pq.iterator());
		assertEquals(2, coll.size());
		assertTrue(coll.contains(Integer.valueOf(3)));
		assertTrue(coll.contains(Integer.valueOf(6)));

		// now poll the pq and ensure, no removed element is returned
		assertEquals(Integer.valueOf(3), pq.poll());
		assertEquals(Integer.valueOf(6), pq.poll());
		assertNull(pq.poll());
	}

	@Test
	void testRemoveAndAdd_LowerPriority() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);

		assertEquals(3, pq.size());

		// test removing an element and adding it with lower priority (=higher value)
		pq.remove(Integer.valueOf(5));
		assertEquals(2, pq.size());
		pq.add(Integer.valueOf(5), 7.0);
		assertEquals(3, pq.size());
		assertEquals(Integer.valueOf(3), pq.poll());
		assertEquals(Integer.valueOf(6), pq.poll());
		assertEquals(Integer.valueOf(5), pq.poll());
		assertNull(pq.poll());
	}

	@Test
	void testRemoveAndAdd_HigherPriority() {
		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		pq.add(Integer.valueOf(5), 5.0);
		pq.add(Integer.valueOf(3), 3.0);
		pq.add(Integer.valueOf(6), 6.0);

		assertEquals(3, pq.size());

		// test removing an element and adding it with higher priority (=lower value)
		pq.remove(Integer.valueOf(5));
		assertEquals(2, pq.size());
		pq.add(Integer.valueOf(5), 2.5);
		assertEquals(3, pq.size());
		assertEquals(Integer.valueOf(5), pq.poll());
		assertEquals(Integer.valueOf(3), pq.poll());
		assertEquals(Integer.valueOf(6), pq.poll());
		assertNull(pq.poll());
	}

	@Test
	void testDecreaseKey() {
//		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
//		pq.add(Integer.valueOf(5), 5.0);
//		pq.add(Integer.valueOf(3), 3.0);
//		pq.add(Integer.valueOf(6), 6.0);
//
//		assertEquals(3, pq.size());
//
//		// test removing an element and adding it with higher priority (=lower value)
////		pq.remove(Integer.valueOf(5));
////		assertEquals(2, pq.size());
////		pq.add(Integer.valueOf(5), 2.5);
//		pq.decreaseKey(Integer.valueOf(5), 2.5);
//		assertEquals(3, pq.size());
//		assertEquals(Integer.valueOf(5), pq.poll());
//		assertEquals(Integer.valueOf(3), pq.poll());
//		assertEquals(Integer.valueOf(6), pq.poll());
//		assertNull(pq.poll());

		PseudoRemovePriorityQueue<Integer> pq = new PseudoRemovePriorityQueue<Integer>(10);
		Integer entry0 = new Integer(5);
		Integer entry1 = new Integer(3);
		Integer entry2 = new Integer(6);
		pq.add(entry0, 5.0);
		pq.add(entry1, 3.0);
		pq.add(entry2, 6.0);

		assertEquals(3, pq.size());

		// test decreasing an element by increasing priority (=lower value)
		pq.decreaseKey(entry0, 2);
		assertEquals(3, pq.size());
		assertEquals(entry0, pq.poll());
		assertEquals(entry1, pq.poll());
		assertEquals(entry2, pq.poll());
		assertNull(pq.poll());

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
		assertEquals(entry2, pq.poll());
		assertEquals(entry1, pq.poll());
		assertEquals(entry0, pq.poll());
		assertNull(pq.poll());
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
