/* *********************************************************************** *
 * project: org.matsim.*
 * RouterPriorityQueue.java
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

package org.matsim.core.utils.collections;

import java.util.Iterator;

/**
 * An interface for classes that can be used as priority queue. It offers a
 * decreaseKey(...) method, which can be used by routers to increase the
 * priority of re-visited nodes.
 * 
 * An object's priority is defined by an additional argument of the add method and
 * not determined based on a Comparator as in other implementations. As a result,
 * objects do not have to be wrapped into QueueEntry objects which also include the
 * object's priority.
 * 
 * @author cdobler
 */
public interface RouterPriorityQueue<E> extends Iterable<E> {

	/**
	 * Adds the specified element to this priority queue, with the given priority.
	 * If the element is already present in the queue, it is not added a second
	 * time.
	 * @param o
	 * @param priority
	 * @return <tt>true</tt> if the element was added to the collection.
	 */
	public boolean add(final E o, final double priority);

	/**
	 * Retrieves and removes the head of this queue, or <tt>null</tt>
	 * if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	public E poll();

	/**
	 * Removes a single instance of the specified element from this
	 * queue, if it is present.
	 *
	 * @return <tt>true</tt> if the queue contained the specified
	 *         element.
	 */
	public boolean remove(final E o);
	
	/**
	 * Retrieves, but does not remove, the head of this queue, returning 
	 * <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	public E peek();
	
	/**
	 * Returns the number of elements in this priority queue. If the collection
	 * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of elements in this collection.
	 */
	public int size();

	/**
	 * Checks whether the queue is empty.
	 *
	 * @return <tt>true</tt> if the queue is empty.
	 */
	public boolean isEmpty();
	
	/**
	 * Returns an iterator over the elements in this queue. The iterator
	 * does not return the elements in any particular order. Removing
	 * elements is not supported via the iterator.
	 *
	 * @return an iterator over the elements in this queue.
	 */
	public Iterator<E> iterator();

	/**
	 * Increases the priority (=decrease the given double value) of the element.
	 * If the element ins not part of the queue, it is added. If the new priority
	 * is lower than the existing one, the method returns <tt>false</tt>
	 *
	 * @return <tt>true</tt> if the elements priority was decreased.
	 */
	public boolean decreaseKey(E value, double priority);
	
	/**
	 * Resets the queue to its initial state.
	 */
	public void reset();
}
