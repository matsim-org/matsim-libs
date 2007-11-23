/* *********************************************************************** *
 * project: org.matsim.*
 * PriorityQueueBucket.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.router.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

public class PriorityQueueBucket<T> {

	/** Maximales Fassungsverm&ouml;gen einer Subqueue */
	private int bucketSize; // Groesse jedes Buckets

	/** Array von Subqueues */
	private ArrayList<PriorityQueue<T>> bucketList = new ArrayList<PriorityQueue<T>>();

	KeyComparator<T> comparator;

	/**
	 * Einzelne Priorityqueue.
	 * 
	 * F&uuml;r {@link #bucketSize bucket_size == 1 }.
	 */

	private PriorityQueue<T> singleQueue;

	public PriorityQueueBucket(KeyComparator<T> comparator) {
		this(5000, 5000, comparator);
	}

	/**
	 * Erzeugt eine leere Priorityqueue.
	 * 
	 * @param maxKey
	 *            der h&ouml;chste m&ouml;gliche Key, nur nichtnegative Zahlen
	 *            sind erlaubt
	 * @param bucketSize
	 *            die maximale Anzahl von Elementen in einer Subqueue.
	 */
	public PriorityQueueBucket(int maxKey, int bucketSize,
			KeyComparator<T> comparator) {
		this.bucketSize = bucketSize;
		this.comparator = comparator;

		if (bucketSize == 1) {
			this.singleQueue = newSingleQueue();
		} else {
			int n = maxKey / bucketSize;
			for (int i = 0; i < n; ++i) {
				this.bucketList.add(newSingleQueue());
			}
		}
	}

	private PriorityQueue<T> newSingleQueue() {
		return new PriorityQueue<T>(100, comparator);
	}

	public void add(T element) {
		// Der einfache Fall
		if (bucketSize == 1) {
			singleQueue.add(element);
		} else {
			// Erzeuge ein Element, das ueber sich selbst Auskunft gibt
			// und fge es ein
			int index = addMissingBuckets(comparator.getKey(element));
			bucketList.get(index).add(element);
		}
	}

	private int addMissingBuckets(double key) {
		int index = (int) (key / bucketSize);
		if (index >= bucketList.size()) {
			int additionalBuckets = index - bucketList.size() + 1;
			for (int i = 0; i < additionalBuckets; i++) {
				bucketList.add(newSingleQueue());
			}
		}
		return index;
	}

	public T poll() {
		if (bucketSize == 1) {
			return singleQueue.poll();
		}

		if (isEmpty()) {
			return null;
		}

		// Gib das kleinste ELement aus dem ersten Bucket zurueck
		return bucketList.get(getFirstIndex()).poll();
	}

	public void remove(T element) {

		if (bucketSize == 1) {
			singleQueue.remove(element);
		} else {
			if (isEmpty()) {
				return;
			}

			int index = (int)(comparator.getKey(element) / bucketSize);
			bucketList.get(index).remove(element);
		}
	}

	public boolean isEmpty() {
		boolean empty = true;
		int i = 0;

		if (bucketSize == 1) {
			return singleQueue.isEmpty();
		}

		// berprfe der Reihe nach alle Buckets
		while ((i < bucketList.size()) && empty) {
			if (!bucketList.get(i).isEmpty())
				empty = false;
			else
				++i;
		}

		return empty;
	}

	// --- private Methoden ---//

	/**
	 * Suche den ersten Bucket, der nicht leer ist
	 */
	private int getFirstIndex() {
		int j = 0;

		while ((j < bucketList.size()) && bucketList.get(j).isEmpty())
			++j;

		return j;
	}

	public Iterator<T> iterator() {
		return new BucketIterator<T>(this);
	}
	
	class BucketIterator<T2> implements Iterator<T2> {

		int currentBucketIndex = 0;
		Iterator<T2> currentBucketIterator;
		PriorityQueueBucket<T2> priorityQueue;
		
		public BucketIterator(PriorityQueueBucket<T2> pq) {
			priorityQueue = pq;
			currentBucketIndex = 0;
			while (priorityQueue.bucketList.size() > currentBucketIndex
					&& priorityQueue.bucketList.get(currentBucketIndex).isEmpty() == true) {
				currentBucketIndex++;
			}
			if (priorityQueue.bucketList.size() > currentBucketIndex) {
				currentBucketIterator = priorityQueue.bucketList.get(currentBucketIndex).iterator();
			}
		}
		
		public boolean hasNext() {
			if (currentBucketIterator == null) {
				return false;
			} else if (currentBucketIterator.hasNext()) {
				return true;
			} else {
				return false;
			}
		}

		public T2 next() {
			T2 element = null;
			if (currentBucketIterator != null
					&& currentBucketIterator.hasNext()) {
				element = currentBucketIterator.next();
				if (currentBucketIterator.hasNext() == false) {
					currentBucketIndex++;
					while (priorityQueue.bucketList.size() > currentBucketIndex
							&& priorityQueue.bucketList.get(currentBucketIndex).isEmpty() == true) {
						currentBucketIndex++;
					}
					if (priorityQueue.bucketList.size() > currentBucketIndex) {
						currentBucketIterator = priorityQueue.bucketList.get(currentBucketIndex).iterator();
					}
				}
			}
			return element;
		}

		public void remove() {
			if (currentBucketIterator != null) {
				currentBucketIterator.remove();
			}
		}
	}
}
