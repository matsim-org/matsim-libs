/* *********************************************************************** *
 * project: org.matsim.*
 * BinaryMinHeap.java
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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author cdobler
 * @author muelleki
 *
 * @param <E> the type of elements held in this collection
 */
public final class BinaryMinHeap<E extends HasIndex> implements MinHeap<E> {

	/**
	 * Each HeapEntry contains a final integer value that points to a
	 * position in the indices array. The value in the indices array points
	 * to the position in the data and costs arrays where the HeapEntry
	 * is currently located.
	 */
	private final E[] data;
	final double[] costs;
	final int[] indices;
	
	private int heapSize;
	private transient int modCount;
	
	/**
	 *  The classic approach of removing the heap's head (poll) is to replace the 
	 *  head with the heap's last entry. Afterwards this entry is sifted downwards
	 *  until a valid position is reached. However, when doing so, two compare
	 *  operations have to be performed for each level (comparing with left and right
	 *  child).
	 * 
	 *  In the alternative approach, the head is sifted downwards to the left level in
	 *  a special way. It replaces always the smaller ones of its children (only they
	 *  are compared!). After reaching the bottom level, it its replaced with the heap's
	 *  last entry. Finally, this entry is sifted upwards until a valid position is found.
	 *  This approach should perform fewer compare operations than the classical approach.
	 *  Idea: see http://magazin.c-plusplus.de/artikel/Binary%20Heaps
	 * 
	 */
	private final boolean classicalRemove;
	
	private final int fanout;

	/*package*/ static final int defaultFanout = 6;
	
	public BinaryMinHeap(int size) {
		this(size, defaultFanout, false);
	}

	@SuppressWarnings("unchecked")
	public BinaryMinHeap(int size, int fanout, boolean classicalRemove) {
		this.data = (E[]) new HasIndex[size];

		this.costs = new double[size];
		for (int i = 0; i < costs.length; i++) {
			costs[i] = Double.MAX_VALUE;
		}

		this.indices = new int[size];
		for (int i = 0; i < indices.length; i++) {
			indices[i] = -1;
		}
		this.heapSize = 0;
		this.modCount = 0;

		this.fanout = fanout;
		this.classicalRemove = classicalRemove;
	}
	
	/**
	 * @return the fan-out of the heap
	 */
	/*package*/ int getFanout() {
		return this.fanout;
	}

	/**
	 * Resets the queue to its initial state.
	 */
	public void reset() {		
		/*
		 * For a small number of remaining entries in the heap, only removing
		 * them might be faster than overwriting all entries. However, when doing so,
		 * we have to do twice as much array accesses. 
		 */
		if (heapSize < indices.length / 10) {
			for (int i = 0; i < heapSize; i++) {
				indices[data[i].getArrayIndex()] = -1;
			}			
		} else {
			for (int i = 0; i < indices.length; i++) {
				indices[i] = -1;
			}
		}
		
		for (int i = 0; i < heapSize; i++) {
			costs[i] = Double.MAX_VALUE;
		}
		
		this.heapSize = 0;
		this.modCount = 0;
	}
	
	@Override
	public E peek() {
		if (isEmpty()) return null;
		else return peek(0);
	}

	private E peek(int index) {
		return data[index];
	}

	/**
	 * Retrieves and removes the head of this queue, or <tt>null</tt> if this
	 * queue is empty.
	 * 
	 * @return the head of this queue, or <tt>null</tt> if this queue is empty.
	 */
	@Override
	public E poll() {
		E minValue;
		if (isEmpty()) return null;
		else {
			this.modCount++;
			minValue = data[0];
			if (classicalRemove) {
				data[0] = data[heapSize - 1];
				costs[0] = costs[heapSize - 1];
				indices[data[0].getArrayIndex()] = 0;
				indices[minValue.getArrayIndex()] = -1;

				heapSize--;
				if (heapSize > 0) siftDown(0);
			} else {
				siftDownUp(0);

				indices[minValue.getArrayIndex()] = -1;
			}
			return minValue;
		}
	}

	private void siftDownUp(int index) {
		index = removeSiftDown(index);

		// Swap entry with heap's last entry.
		heapSize--;

		// Sift up entry that was previously at the heap's end.
		siftUp(index, data[heapSize], costs[heapSize]);

		// Reset sentinel here:
		costs[heapSize] = Double.MAX_VALUE;
	}

	/*
	 * Used by alternative remove() approach. The costs have been set to
	 * Double.MAX_VALUE. Therefore we only have to compare the nodes children.
	 */
	private int removeSiftDown(int nodeIndex) {
		while(true) {
			int leftChildIndex = getLeftChildIndex(nodeIndex);
			if (leftChildIndex >= heapSize) break;
			
			double leftCosts = costs[leftChildIndex];

			int limitChildIndex = Math.min(leftChildIndex + fanout, heapSize);

			for (int rightChildIndex = leftChildIndex + 1; rightChildIndex < limitChildIndex; rightChildIndex++) {
				/*
				 *  We use the sentinel values Double.MAX_VALUE to protect 
				 *  ourselves from looking beyond the heap's true size
				 */
				double rightCosts = costs[rightChildIndex];
				if (leftCosts >= rightCosts && 
						(leftCosts > rightCosts || data[leftChildIndex].getArrayIndex() > data[rightChildIndex].getArrayIndex())) {
					leftChildIndex = rightChildIndex;
					leftCosts = rightCosts;
				}
			}

			copyData(nodeIndex, leftChildIndex);
			nodeIndex = leftChildIndex;
		}

		return nodeIndex;
	}
	
	/**
	 * Returns the number of elements in this priority queue.
	 *
	 * @return the number of elements in this collection.
	 */
	@Override
	public int size() {
		return this.heapSize;
	}
	
	/**
	 * Checks whether the queue is empty.
	 *
	 * @return <tt>true</tt> if the queue is empty.
	 */
	@Override
	public boolean isEmpty() {
		return (heapSize == 0);
	}
	
	/**
	 * Adds the specified element to this priority queue, with the given priority.
	 * If the element is already present in the queue, it is not added a second
	 * time.
	 * @param value
	 * @param priority
	 * @return <tt>true</tt> if the element was added to the collection.
	 */
	@Override
	public boolean add(E value, double priority) {
		
		if (heapSize == data.length) throw new RuntimeException("Heap's underlying storage is overflow!");

		this.modCount++;
		
		// if the element is already present in the queue, return false
		if (indices[value.getArrayIndex()] >= 0) {
			return false;
		} else {
			siftUp(heapSize, value, priority);
			heapSize++;			
			return true;
		}		
	}
	
	/**
	 * Removes a single instance of the specified element from this
	 * queue, if it is present.
	 *
	 * @return <tt>true</tt> if the queue contained the specified
	 *         element.
	 */
	public boolean remove(E value) {
		
		if (value == null) return false;
				
		/*
		 * Check the elements index. "-1" means that the element is not
		 * present in the heap. 
		 */
		int index = indices[value.getArrayIndex()];
		if (index < 0) {
			return false;
		} else {
			if (classicalRemove) {
				// Move entry to heap's top and then remove the heap's head.
				boolean decreasedKey = decreaseKey(value, Double.NEGATIVE_INFINITY);
				if (decreasedKey && data[0] == value) {
					this.poll();
					return true;
				} else return false;
			} else {
				siftDownUp(index);

				// index has changed, therefore we cannot use "index" again
				indices[value.getArrayIndex()] = -1;
				this.modCount++;
				return true;
			}
		}
	}
	
	/**
	 * Increases the priority (=decrease the given double value) of the element.
	 * If the element ins not part of the queue, it is added. If the new priority
	 * is lower than the existing one, the method returns <tt>false</tt>
	 *
	 * @return <tt>true</tt> if the elements priority was decreased.
	 */
	@Override
	public boolean decreaseKey(E value, double cost) {

		// If the element is not yet present in the heap, simply add it.
		int index = indices[value.getArrayIndex()];
		if (index < 0) {
			return this.add(value, cost);
		}
		
		/*
		 * If the cost should be increased, we cannot do this. Therefore we
		 * return false.
		 */
		double oldCost = costs[index];
		if (oldCost < cost) return false;

		// update costs in array
		siftUp(index, data[index], cost);
		return true;
	}

	/**
	 * Returns an iterator over the elements in this queue. The iterator
	 * does not return the elements in any particular order. Removing
	 * elements is not supported via the iterator.
	 *
	 * @return an iterator over the elements in this queue.
	 */
	@Override
	public Iterator<E> iterator() {
		return new ArrayIterator(data, heapSize);
	}
		
	private void copyData(int indexTarget, int indexSource) {
		// copy HeapEntries
		E entry = data[indexSource];
		data[indexTarget] = entry;

		// copy costs
		costs[indexTarget] = costs[indexSource];

		// copy indices
		indices[entry.getArrayIndex()] = indexTarget;
	}

	private int getLeftChildIndex(int nodeIndex) {
		return fanout * nodeIndex + 1;
	}

	private int getParentIndex(int nodeIndex) {
		return (nodeIndex - 1) / fanout;
	}
	
	private void siftUp(int index, E newEntry, double newCost) {
		while (index > 0) {
			int parentIndex = getParentIndex(index);
			double parentCost = costs[parentIndex];
			if (newCost > parentCost)
				break;
			if (newCost == parentCost && newEntry.getArrayIndex() > data[parentIndex].getArrayIndex())
				break;
			this.copyData(index, parentIndex);
			
			// for next iteration
			index = parentIndex;
		}

		data[index] = newEntry;
		costs[index] = newCost;
		indices[newEntry.getArrayIndex()] = index;
	}

	private void siftDown(int nodeIndex) {
		
		int leftChildIndex = getLeftChildIndex(nodeIndex);
		if (leftChildIndex >= heapSize) return;
		
		int minIndex = -1;
		double minCosts = Double.POSITIVE_INFINITY;
		
		int limitChildIndex = Math.min(leftChildIndex + fanout, heapSize + 1);
		
		for (int childIndex = leftChildIndex; childIndex < limitChildIndex; childIndex++) {
			/*
			 * If the costs are equal, use the array indices to define the sort order.
			 * Doing so should guarantee a deterministic order of the heap entries.
			 */
			double childCosts = costs[childIndex];
			if (childCosts <= minCosts) {
				if (childCosts < minCosts || data[childIndex].getArrayIndex() < data[minIndex].getArrayIndex()) {
					minIndex = childIndex;
					minCosts = childCosts;
				}
			}
		}
		
		if (minIndex >= 0) {
			swapData(nodeIndex, minIndex);
			siftDown(minIndex);				
		}
	}
	

	private void swapData(int index1, int index2) {

		// swap HeapEntries
		E entry1 = data[index1];
		E entry2 = data[index2];
		data[index1] = entry2;
		data[index2] = entry1;
		
		// swap costs
		double tmpCost = costs[index1];
		costs[index1] = costs[index2];
		costs[index2] = tmpCost;
		
		// swap indices
		indices[entry1.getArrayIndex()] = index2;
		indices[entry2.getArrayIndex()] = index1;
	}
	
	private final class ArrayIterator implements Iterator<E> {

		private final int expectedModCount = modCount;
		
		private final E[] array;
		private final int heapSize;
		private int index = 0;
		
		public ArrayIterator(final E[] array, int heapSize) {
			this.array = array;
			this.heapSize = heapSize;
		}
		
		@Override
		public boolean hasNext() {
			return (heapSize > index);
		}

		@Override
		public E next() {
			this.checkForComodification();
			if (!hasNext()) throw new NoSuchElementException();
			final E value = array[index];
			index++;
			return value;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported operation!");
		}
		
		private void checkForComodification() {
			if (modCount != expectedModCount) throw new ConcurrentModificationException();
		}
	}
}