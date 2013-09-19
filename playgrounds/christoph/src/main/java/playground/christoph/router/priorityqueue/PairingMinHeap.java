/* *********************************************************************** *
 * project: org.matsim.*
 * PairingMinHeap.java
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

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.matsim.core.router.priorityqueue.HasIndex;
import org.matsim.core.router.priorityqueue.MinHeap;

/**
 * Implements an array-based pairing heap, based on code from Mark Allen Weiss.
 * 
 * @author cdobler
 */
public class PairingMinHeap<E extends HasIndex> implements MinHeap<E> {
	
	// The tree array for combineSiblings
	private final E[] treeArray;
	
	final E[] data;
	final double[] costs;
	
	/**
	 * Each HeapEntry contains a final integer value that points to a
	 * position in the indices array. The value in the indices array points
	 * to the position in the data and costs array where the HeapEntry
	 * is currently located.
	 */
	final E[] leftChild;	// left or parent if no left element available
	final E[] nextSibling;
	final E[] prev;
	
	private E root;
	private int size;
	
	private transient int modCount;
	
	/**
	 * Construct the pairing heap.
	 */
	@SuppressWarnings("unchecked")
	public PairingMinHeap(int size) {
		this.treeArray = (E[]) new HasIndex[size];
		
		this.data = (E[]) new HasIndex[size];
		this.costs = new double[size];
		this.leftChild = (E[]) new HasIndex[size];
		this.nextSibling = (E[]) new HasIndex[size];
		this.prev = (E[]) new HasIndex[size];
		
		root = null;
		size = 0;
		modCount = 0;
		
		// is this necessary?
		for (int i = 0; i < size; i++) {
			data[i] = null;
		}
	}

	/**
	 * Adds the specified element to this priority queue, with the given priority.
	 * If the element is already present in the queue, it is not added a second
	 * time.
	 * @param e
	 * @param priority
	 * @return <tt>true</tt> if the element was added to the collection.
	 */
	public boolean add(E e, double priority) {

		// if the element is already present in the queue, return false
		final int index = e.getArrayIndex();
		if (this.data[index] != null) {
			return false;
		}
		modCount++;
		
		this.data[index] = e;
		this.costs[index] = priority;
		
		if (root == null) root = e;
		else root = compareAndLink(root, e);
		
		size++;
		return true;
	}

	/**
	 * Retrieves, but does not remove, the head of this queue, returning 
	 * <tt>null</tt> if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	public E peek() {
		if (isEmpty()) return null;
		return root;
	}
	
	/**
	 * Retrieves and removes the head of this queue, or <tt>null</tt>
	 * if this queue is empty.
	 *
	 * @return the head of this queue, or <tt>null</tt> if this
	 *         queue is empty.
	 */
	public E poll() {
		if (isEmpty()) return null;
		
		modCount++;
		
		E x = peek();
		int index = x.getArrayIndex();
		E left = this.leftChild[index];
		if (left == null) root = null;
		else root = combineSiblings(left);

		// clean up
		this.data[index] = null;
		this.nextSibling[index] = null;
		this.prev[index] = null;
		this.leftChild[index] = null;
		
		size--;
		return x;
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
		 * Check whether the element is present in the heap 
		 */
		if (data[value.getArrayIndex()] == null) return false;
		else {
			// Move entry to heap's top and then remove the heap's head.
			boolean decreasedKey = decreaseKey(value, Double.MIN_VALUE);
			if (decreasedKey && this.peek() == value) {
				this.poll();
				this.modCount++;
				return true;
			} else return false;
		}
	}

	/**
	 * Increases the priority (=decrease the given double value) of the element.
	 * If the element ins not part of the queue, it is added. If the new priority
	 * is lower than the existing one, the method returns <tt>false</tt>
	 *
	 * @return <tt>true</tt> if the elements priority was decreased.
	 */
	public boolean decreaseKey(E p, double newPriority) {
		if (p == null) return false;

		
		// If the element is not yet present in the heap, simply add it.
		int index = p.getArrayIndex();
		if (data[index] == null) {
			return this.add(p, newPriority);
		}
		
		double oldPriority = this.costs[index];
		if (newPriority > oldPriority) throw new RuntimeException("Old priority (" + oldPriority + ") is lower than new one (" + newPriority + "). Cannot decrease key!");
		
		this.costs[index] = newPriority;
		if (p != root) {
			
			E pPrev = this.prev[index];
			E pNextSibling = this.nextSibling[index];
			
//			if (p.nextSibling != null) p.nextSibling.prev = p.prev;
			if (pNextSibling != null) {
				this.prev[pNextSibling.getArrayIndex()] = pPrev; 
			}
			
//			if (p.prev.leftChild == p) p.prev.leftChild = p.nextSibling;
//			else p.prev.nextSibling = p.nextSibling;
			E pPrevLeftChild = this.leftChild[pPrev.getArrayIndex()];
			if (pPrevLeftChild == p) this.leftChild[pPrev.getArrayIndex()] = pNextSibling;
			else this.nextSibling[pPrev.getArrayIndex()] = pNextSibling;

//			p.nextSibling = null;
			this.nextSibling[index] = null;
			
			root = compareAndLink(root, p);
		}
		
		return true;
	}

	/**
	 * Checks whether the queue is empty.
	 *
	 * @return <tt>true</tt> if the queue is empty.
	 */
	public boolean isEmpty() {
		return root == null;
	}

	/**
	 * Returns the number of elements in this priority queue. If the collection
	 * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
	 * <tt>Integer.MAX_VALUE</tt>.
	 *
	 * @return the number of elements in this collection.
	 */
	public int size() {
		return size;
	}

	/**
	 * Resets the queue to its initial state.
	 */
	public void reset() {
		root = null;
		size = 0;
		modCount = 0;
		
		for (int i = 0; i < data.length; i++) {
			E e = this.data[i];
			if (e != null) { 
				this.data[i] = null;
				this.nextSibling[i] = null;
				this.prev[i] = null;
				this.leftChild[i] = null;				
			}
		}
	}
	
	/**
	 * Internal method that is the basic operation to maintain order. Links
	 * first and second together to satisfy heap order.
	 * 
	 * @param first
	 *            root of tree 1, which may not be null. first.nextSibling MUST
	 *            be null on entry.
	 * @param second
	 *            root of tree 2, which may be null.
	 * @return result of the tree merge.
	 */
	private E compareAndLink(E first, E second) {
		if (second == null) return first;
		
		final int firstIndex = first.getArrayIndex();
		final int secondIndex = second.getArrayIndex();

		double firstPriority = costs[firstIndex];
		double secondPriority = costs[secondIndex];
		
		boolean secondPriorityIsSmaller = false;
		
		// if priorities are equal, sort by array indices
		if (firstPriority == secondPriority) {
//			if (firstIndex < secondIndex) firstPriority -= 1;
//			else secondPriority -= 1;
			if (secondIndex < firstIndex) secondPriorityIsSmaller = true;
		} else secondPriorityIsSmaller = (secondPriority < firstPriority);
		
//		if (secondPriority < firstPriority) {
		if (secondPriorityIsSmaller) {
		
            // Attach first as leftmost child of second
            this.prev[secondIndex] = this.prev[firstIndex];
            this.prev[firstIndex] = second;
            
            this.nextSibling[firstIndex] = this.leftChild[secondIndex];
            if (this.nextSibling[firstIndex] != null) { 
            	this.prev[this.nextSibling[firstIndex].getArrayIndex()] = first;
            }
            
            this.leftChild[secondIndex] = first;
            
            return second;
		} else {
			
            // Attach second as leftmost child of first
            this.prev[secondIndex] = first;
            this.nextSibling[firstIndex] = nextSibling[secondIndex];
            if (this.nextSibling[firstIndex] != null) {
            	this.prev[this.nextSibling[firstIndex].getArrayIndex()] = first;
            }
            nextSibling[secondIndex] = this.leftChild[firstIndex];
            if (nextSibling[secondIndex] != null) {
            	this.prev[nextSibling[secondIndex].getArrayIndex()] = second;
            }
            this.leftChild[firstIndex]  = second;            
            
            return first;
		}
	}

	/**
	 * Internal method that implements two-pass merging.
	 * 
	 * @param firstSibling the root of the conglomerate; assumed not null.
	 */
	private E combineSiblings(E firstSibling) {
	
//		if (firstSibling.nextSibling == null) return firstSibling;
		int index = firstSibling.getArrayIndex();
		if (this.nextSibling[index] == null) return firstSibling;
		
		// Store the subtrees in an array
		int numSiblings = 0;
		for (; firstSibling != null; numSiblings++) {
			
			index = firstSibling.getArrayIndex();		
			treeArray[numSiblings] = firstSibling;
			
//			firstSibling.prev.nextSibling = null; // break links
//			firstSibling = firstSibling.nextSibling;
			E prev = this.prev[index];
			this.nextSibling[prev.getArrayIndex()] = null;
			firstSibling = this.nextSibling[index];
		}
		treeArray[numSiblings] = null;

		// Combine subtrees two at a time, going left to right
		int i = 0;
		for (; i + 1 < numSiblings; i += 2) treeArray[i] = compareAndLink(treeArray[i], treeArray[i + 1]);

		int j = i - 2;

		// j has the result of last compareAndLink.
		// If an odd number of trees, get the last one.
		if (j == numSiblings - 3) treeArray[j] = compareAndLink(treeArray[j], treeArray[j + 2]);

		// Now go right to left, merging last tree with
		// next to last. The result becomes the new last.
		for (; j >= 2; j -= 2) treeArray[j - 2] = compareAndLink(treeArray[j - 2], treeArray[j]);

		return treeArray[0];
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
		return new ArrayIterator(data, size);
	}
	
	private final class ArrayIterator implements Iterator<E> {

		private final int expectedModCount = modCount;
		
		private final E[] array;
		private int lastEntry = 0;
		private int index = 0;
		
		public ArrayIterator(final E[] array, int heapSize) {
			this.array = array;
			if (heapSize <= 0) {
				lastEntry = 0;
				index = 1;	// ensure that hasNext() returns false for empty heap
			}
			
			/*
			 * Find the last entry in the array. Use to compare
			 * with index, which points to the iterators current
			 * position.
			 */
			else {
				lastEntry = 0;
				for (int i = array.length - 1; i >= 0; i--) {
					if (array[i] != null) {
						lastEntry = i;
						break;
					}
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return (index <= lastEntry);
		}

		@Override
		public E next() {
			this.checkForComodification();
			if (!hasNext()) throw new NoSuchElementException();
			while (index <= lastEntry) {
				E value = array[index];
				index++;
				if (value != null) return value;				
			}
			throw new NoSuchElementException();
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