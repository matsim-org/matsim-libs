package org.matsim.core.router.speedy;

import java.util.NoSuchElementException;

/**
 * Implementation of a d-ary min-heap.
 *
 * A d-ary min-heap is a generalization of the binary min-heap,
 * which provides better cpu cache locality and has faster decrease-key operations
 * but slower poll operations. But in Dijkstra's algorithm, decrease-key is more common
 * than poll, so it should still be beneficial.
 *
 * In most literature, the poll-operation is implemented by removing the top element and
 * replacing it with the last entry in the heap, and then let this new root sift down again
 * until the heap-conditions are all met. This requires multiple comparisons on each level (the
 * value must be compared with each of its children).
 *
 * An alternative implementation tries to fix this in the following way: the "whole" at the root
 * after removing the top is first sifted down by replacing it with the smaller of its children.
 * This requires one less comparison, as only the children need to be compared against each other.
 * Once there are no more children (the whole is at the bottom level), replace the whole with the
 * last entry of the heap. Now, sift this entry up until the heap-conditions are all met.
 *
 * This implementation uses the alternative implementation for additional speedup.
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
class DAryMinHeap {
	private final int[] heap;
	private final int[] pos;
	private int size = 0;
	private final int d;
	private final double[] cost;

	DAryMinHeap(int nodeCount, int d) {
		this.heap = new int[nodeCount]; // worst case: every node is part of the heap
		this.pos = new int[nodeCount]; // worst case: every node is part of the heap
		this.cost = new double[nodeCount];
		this.d = d;
	}

	void insert(int node, double cost) {
		int i = this.size;
		this.size++;

		// sift up
		int parent = parent(i);
		while (i > 0 && cost < this.cost[parent]) {
			this.heap[i] = this.heap[parent];
			this.pos[this.heap[parent]] = i;
			this.cost[i] = this.cost[parent];
			i = parent;
			parent = parent(parent);
		}
		this.heap[i] = node;
		this.cost[i] = cost;
		this.pos[node] = i;
	}

	void decreaseKey(int node, double cost) {
		int i = this.pos[node];
		if (i < 0) {
			// The element is not yet in the heap, add it.
			// in the ALT algorithm, it is actually possible that a node gets expanded twice,
			// and that its weight is decreased after already having been expanded once.
			insert(node, cost);
			return;
		}
		if (this.heap[i] != node) {
			throw new NoSuchElementException("The element with index " + i + " could not be found at the proper location in the heap.");
		}
		if (this.cost[i] < cost) {
			throw new IllegalArgumentException("existing cost is already smaller than new cost.");
		}

		// sift up
		int parent = parent(i);
		while (i > 0 && cost < this.cost[parent]) {
			this.heap[i] = this.heap[parent];
			this.cost[i] = this.cost[parent];
			this.pos[this.heap[parent]] = i;
			i = parent;
			parent = parent(parent);
		}
		this.heap[i] = node;
		this.cost[i] = cost;
		this.pos[node] = i;
	}

	int poll() {
		if (this.size == 0) {
			throw new NoSuchElementException("heap is empty");
		}
		if (this.size == 1) {
			this.size--;
			return this.heap[0];
		}

		int root = this.heap[0];
		this.pos[root] = -1;

		fixWhole(0);
		return root;
	}

	int peek() {
		if (this.size == 0) {
			throw new NoSuchElementException("heap is empty");
		}
		return this.heap[0];
	}

	public boolean remove(int node) {
		int i = this.pos[node];
		if (i < 0) {
			return false;
		}

		this.fixWhole(i);
		return true;
	}

	int size() {
		return this.size;
	}

	boolean isEmpty() {
		return this.size == 0;
	}

	void clear() {
		this.size = 0;
	}

	private void fixWhole(int index) {
		// move the whole down
		while (true) {
			int left = left(index);
			if (left >= this.size) {
				break;
			}
			int right = right(index);
			if (right >= this.size) {
				right = this.size - 1;
			}

			int smallest = left;
			double smallestCost = this.cost[left];
			for (int child = left + 1; child <= right; child++) {
				double childCost = this.cost[child];
				if (childCost <= smallestCost) {
					if (childCost < smallestCost || this.heap[child] < this.heap[smallest]) {
						smallest = child;
						smallestCost = childCost;
					}
				}
			}

			this.heap[index] = this.heap[smallest];
			this.cost[index] = smallestCost;
			this.pos[this.heap[index]] = index;

			index = smallest;
		}

		// move last entry to whole, unless the whole is already at the end
		this.size--;
		if (index < this.size) {
			this.heap[index] = this.heap[this.size];
			this.cost[index] = this.cost[this.size];
			this.pos[this.heap[index]] = index;
		}

		// move entry up as far as needed
		double nodeCost = this.cost[index];
		while (index > 0) {
			int parent = parent(index);
			double parentCost = this.cost[parent];
			if (nodeCost < parentCost) {
				swap(index, parent);
				index = parent;
			} else {
				break;
			}
		}
	}

	private int right(int i) {
		return this.d * i + this.d;
	}

	private int left(int i) {
		return this.d * i + 1;
	}

	private int parent(int i) {
		return (i - 1) / this.d;
	}

	private void swap(int i, int parent) {
		int tmp = this.heap[parent];
		this.heap[parent] = this.heap[i];
		this.pos[this.heap[i]] = parent;
		this.heap[i] = tmp;
		this.pos[tmp] = i;

		double tmpC = this.cost[parent];
		this.cost[parent] = this.cost[i];
		this.cost[i] = tmpC;
	}

	public IntIterator iterator() {
		return new HeapIntIterator();
	}

	public interface IntIterator {
		boolean hasNext();
		int next();
	}

	private class HeapIntIterator implements IntIterator {
		private int pos = 0;

		@Override
		public boolean hasNext() {
			return this.pos > DAryMinHeap.this.size;
		}

		@Override
		public int next() {
			int node = DAryMinHeap.this.heap[this.pos];
			this.pos++;
			return node;
		}
	}

}