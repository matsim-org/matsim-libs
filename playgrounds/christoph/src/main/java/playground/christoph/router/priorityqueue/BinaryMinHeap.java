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

package playground.christoph.router.priorityqueue;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author cdobler
 *
 * @param <E> the type of elements held in this collection
 */
public final class BinaryMinHeap<E extends HeapEntry> implements MinHeap<E> {

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
	private final boolean classicalRemove = false;
	
	private final boolean debug = false;
	
	@SuppressWarnings("unchecked")
	public BinaryMinHeap(int size) {
		this.data = (E[]) new HeapEntry[size];
		this.costs = new double[size];
		this.indices = new int[size];
		heapSize = 0;
		
		for (int i = 0; i < indices.length; i++) {
			indices[i] = -1;
		}
	}
	
	/**
	 * Resets the queue to its initial state.
	 */
	public void reset() {
//		data[0] = null;
//		for (int i = 0; i < indices.length; i++) {
//			indices[i] = -1;
//		}
		
		/*
		 * For a small number of remaining entries in the heap, only removing
		 * them might be faster than overwriting all entries. However, when doing so,
		 * we have to do twice as much array accesses. 
		 */
		if (heapSize < indices.length / 2) {
			for (int i = 0; i < heapSize; i++) {
				indices[data[i].getArrayIndex()] = -1;
			}			
		} else {
			for (int i = 0; i < indices.length; i++) {
				indices[i] = -1;
			}
		}
		
		heapSize = 0;
	}
	
	@Override
	public E peek() {
		if (isEmpty())
			return null;
		else
			return peek(0);
	}

	public E peek(int index) {
		return data[index];
	}

	public double peekCost() {
		return peekCost(0);
	}

	public double peekCost(int index) {
		return costs[index];
	}

	/**
	 * Retrieves and removes the head of this queue, or <tt>null</tt>
	 * if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	@Override
	public E remove() {
		E minValue;
		if (isEmpty()) return null;
		else {
			if (classicalRemove) {
				minValue = data[0];
				data[0] = data[heapSize - 1];
				costs[0] = costs[heapSize - 1];
				indices[data[0].getArrayIndex()] = 0;
				indices[minValue.getArrayIndex()] = -1;
				
				heapSize--;
				if (heapSize > 0) siftDown(0);
				return minValue;
			} else {
				minValue = data[0];

				/*
				 * Set costs to Double.MAX_Value. Afterwards it is
				 * shifted downwards to the heap's bottom.
				 */
				costs[0] = Double.MAX_VALUE;
				removeSiftDown(0);
				
				/*
				 * Swap entry with heap's last entry.
				 */
				heapSize--;
				int index = indices[minValue.getArrayIndex()];
				this.swapData(index, heapSize);

				indices[minValue.getArrayIndex()] = -1;
				
				/*
				 * Sift up entry that was previously at the heap's end.
				 */
				siftUp(index);			
				return minValue;
			}
		}
	}
		
	/*
	 * Used by alternative remove() approach. The costs have been set to
	 * Double.MAX_VALUE. Therefore we only have to compare the nodes children.
	 */
	private void removeSiftDown(int nodeIndex) {
		for (;;) {
			int leftChildIndex, rightChildIndex, minIndex;
			double leftCosts, rightCosts;

			leftChildIndex = getLeftChildIndex(nodeIndex);
			if (leftChildIndex >= heapSize)
				return;
			rightChildIndex = getRightChildIndex(nodeIndex);
			if (rightChildIndex >= heapSize) {
				minIndex = leftChildIndex;
			} else {
				leftCosts = costs[leftChildIndex];
				rightCosts = costs[rightChildIndex];
				if (leftCosts < rightCosts) {
					minIndex = leftChildIndex;
				} else if (leftCosts > rightCosts) {
					minIndex = rightChildIndex;
				} else if (data[leftChildIndex].getArrayIndex() < data[rightChildIndex]
						.getArrayIndex()) {
					minIndex = leftChildIndex;
				} else
					minIndex = rightChildIndex;
			}

			swapData(nodeIndex, minIndex);
			nodeIndex = minIndex;
		}
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
		
		// if the element is already present in the queue, return false
		if (indices[value.getArrayIndex()] >= 0) {
			return false;
		}
		
		if (heapSize == data.length) throw new RuntimeException("Heap's underlying storage is overflow!");
		else {
			data[heapSize] = value;
			costs[heapSize] = priority;
			indices[value.getArrayIndex()] = heapSize;
			
			siftUp(heapSize);
			heapSize++;
		}
		
		return true;
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
				boolean decreasedKey = decreaseKey(value, Double.MIN_VALUE);
				if (decreasedKey && data[0] == value) {
					this.remove();
					return true;
				} else {
					if (debug) {
						System.out.println("Could not remove Element?!");					
					}
					return false;
				}
			} else {
				/*
				 * Set costs to Double.MAX_Value. Afterwards it is
				 * shifted downwards to the heap's bottom.
				 */
				costs[index] = Double.MAX_VALUE;
				removeSiftDown(index);
				
				heapSize--;
				
				/*
				 * Swap entry with heap's last entry.
				 */
				// update index
				index = indices[value.getArrayIndex()];		
				this.swapData(index, heapSize);

				// index has changed, therefore we cannot use "index" again
				indices[value.getArrayIndex()] = -1;
				
				/*
				 * Sift up entry that was previously at the heap's end.
				 */
				siftUp(index);
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

		/*
		 * If the element is not yet present in the heap, simply add it.
		 */
		int index = indices[value.getArrayIndex()];
		if (index < 0) {
			return this.add(value, cost);
		}
		
		/*
		 * If the cost should be increased, we cannot do this. Therefore we
		 * return false.
		 */
		double oldCost = costs[index];
		if (oldCost < cost) {
			if (debug) {
				check();
				throw new RuntimeException("Old costs (" + oldCost + ") are lower than new ones (" + cost + "). Cannot decrease key!");
			}
			else
				return false;
		}

		// update costs in array
		costs[index] = cost;
		
		int parentIndex = getParentIndex(index);
		double parentCost = costs[parentIndex];
		while (index > 0 && cost < parentCost) {
			
			this.swapData(index, parentIndex);
			
			// for next iteration
			index = parentIndex;
			parentIndex = getParentIndex(index);
			parentCost = costs[parentIndex];
		}
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
	
	private void check() {
		int i = 0;
		for (HeapEntry entry : data) {
			i++;
			if (i > heapSize) break;
			if (entry == null) continue;
			
			int i1 = indices[entry.getArrayIndex()];
			if (i1 + 1 != i) {
				System.out.println("Indices do not match!");
			}
			
//			double c1 = ((DijkstraNodeData) entry).getCost();
//			double c2 = costs[indices[entry.getArrayIndex()]];
//			if (c1 != c2) {
//				System.out.println("costs do not match!");
//			}
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

	private int getLeftChildIndex(int nodeIndex) {
		return 2 * nodeIndex + 1;
	}

	private int getRightChildIndex(int nodeIndex) {
		return 2 * nodeIndex + 2;
	}

	private int getParentIndex(int nodeIndex) {
		return (nodeIndex - 1) / 2;
	}
	
	private void siftUp(int nodeIndex) {
		int parentIndex;
		if (nodeIndex != 0) {
			parentIndex = getParentIndex(nodeIndex);
			
			/*
			 * If the costs are equal, use the array indices to define the sort order.
			 * Doing so should guarantee a deterministic order of the heap entries.
			 */
			double parentCosts = costs[parentIndex];
			double nodeCosts = costs[nodeIndex];
			if (parentCosts > nodeCosts ||
					(parentCosts == nodeCosts && 
					 data[parentIndex].getArrayIndex() > data[nodeIndex].getArrayIndex())) {
				
				swapData(nodeIndex, parentIndex);				
				siftUp(parentIndex);
			}
		}
	}

	private void siftDown(int nodeIndex) {
		int leftChildIndex, rightChildIndex, minIndex;
		double leftCosts, rightCosts, minCosts, nodeCosts;
		
		leftChildIndex = getLeftChildIndex(nodeIndex);
		rightChildIndex = getRightChildIndex(nodeIndex);
		if (rightChildIndex >= heapSize) {
			if (leftChildIndex >= heapSize) return;
			else {
				minCosts = costs[leftChildIndex];
				minIndex = leftChildIndex;
			}
		} else {
			leftCosts = costs[leftChildIndex];
			rightCosts = costs[rightChildIndex];
			if (leftCosts <= rightCosts) {
				minCosts = leftCosts;
				minIndex = leftChildIndex;
			} else {
				minCosts = rightCosts;
				minIndex = rightChildIndex;
			}
		}

		/*
		 * If the costs are equal, use the array indices to define the sort order.
		 * Doing so should guarantee a deterministic order of the heap entries.
		 */
		nodeCosts = costs[nodeIndex];
		if (nodeCosts > minCosts ||
				(nodeCosts == minCosts && 
				 data[nodeIndex].getArrayIndex() > data[minIndex].getArrayIndex())) {
			swapData(nodeIndex, minIndex);
			siftDown(minIndex);
		}
	}
		
	private final class ArrayIterator implements Iterator<E> {

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
			if (!hasNext()) throw new NoSuchElementException();
			final E value = array[index];
			index++;
			return value;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported operation!");
		}
	}
}