package org.matsim.core.router.speedy;

import java.util.NoSuchElementException;

/**
 * @author mrieser / Simunto
 */
class NodeMinHeap {
	private final int[] heap;
	private final int[] pos;
	private int size = 0;
	private final CostGetter costGetter;
	private final CostSetter costSetter;

	public interface CostGetter {
		double getCost(int index);
	}

	public interface CostSetter {
		void setCost(int index, double cost);
	}

	NodeMinHeap(int nodeCount, CostGetter costGetter, CostSetter costSetter) {
		this.heap = new int[nodeCount]; // worst case: every node is part of the heap
		this.pos = new int[nodeCount];
		this.costGetter = costGetter;
		this.costSetter = costSetter;
	}

	void insert(int node) {
		int i = this.size;
		this.size++;

		double nodeCost = this.costGetter.getCost(node);
		// sift up
		int parent = parent(i);
		while (i > 0 && nodeCost < this.costGetter.getCost(this.heap[parent])) {
			this.heap[i] = this.heap[parent];
			this.pos[this.heap[parent]] = i;
			i = parent;
			parent = parent(parent);
		}
		this.heap[i] = node;
		this.pos[node] = i;
	}

	void decreaseKey(int node, double cost) {
		int i;
		for (i = 0; i < this.size; i++) {
			if (this.heap[i] == node) {
				break;
			}
		}
		if (this.costGetter.getCost(this.heap[i]) < cost) {
			throw new IllegalArgumentException("existing cost is already smaller than new cost.");
		}

		this.costSetter.setCost(node, cost);

		// sift up
		int parent = parent(i);
		while (i > 0 && cost < this.costGetter.getCost(this.heap[parent])) {
			this.heap[i] = this.heap[parent];
			this.pos[this.heap[parent]] = i;
			i = parent;
			parent = parent(parent);
		}
		this.heap[i] = node;
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

		// remove the last item, set it as new root
		int lastNode = this.heap[this.size - 1];
		this.size--;
		this.heap[0] = lastNode;
		this.pos[lastNode] = 0;

		// sift down
		minHeapify(0);

		return root;
	}

	int peek() {
		if (this.size == 0) {
			throw new NoSuchElementException("heap is empty");
		}
		return this.heap[0];
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

	private void minHeapify(int i) {
		int left = left(i);
		int right = right(i);
		int smallest = i;

		if (left <= (this.size - 1) && this.costGetter.getCost(this.heap[left]) < this.costGetter.getCost(this.heap[i])) {
			smallest = left;
		}
		if (right <= (this.size - 1) && this.costGetter.getCost(this.heap[right]) < this.costGetter.getCost(this.heap[smallest])) {
			smallest = right;
		}
		if (smallest != i) {
			swap(i, smallest);
			minHeapify(smallest);
		}
	}

	private int right(int i) {
		return 2 * i + 2;
	}

	private int left(int i) {
		return 2 * i + 1;
	}

	private int parent(int i) {
		return (i - 1) / 2;
	}

	private void swap(int i, int parent) {
		int tmp = this.heap[parent];
		this.heap[parent] = this.heap[i];
		this.pos[this.heap[i]] = parent;
		this.heap[i] = tmp;
		this.pos[tmp] = i;
	}
}