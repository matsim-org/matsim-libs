/* *********************************************************************** *
 * project: org.matsim.*
 * FibonacciHeap.java
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

package playground.mrieser;

import java.util.HashMap;
import java.util.Map;


public class FibonacciHeap<V> {

	static public class Node<V> {
		private Node<V> left = null;
		private Node<V> right = null;
		private Node<V> parent = null;
		private Node<V> child = null;
		private int rank = 0; // number of childs??
		private boolean marker = false;
		private double key;
		private final V value;

		private Node(final double key, final V value) {
			this.key = key;
			this.value = value;
		}

		private void addChild(final Node<V> other) {
			// remove node "other" from it's old list
			other.left.right = other.right;
			other.right.left = other.left;

			// make "other" a child of this
			other.parent = this;

			// add "other" to this list
			if (this.child == null) {
				other.right = other;
				other.left = other;
				this.child = other;
				this.rank = 1;
			} else {
				other.left = this.child.left;
				other.right = this.child;
				other.left.right = other;
				other.right.left = other;
				this.rank++;
			}
			this.marker = false;
		}

		private void removeChild(final Node<V> child) {
			// remove "other" from the list
			child.left.right = child.right;
			child.right.left = child.left;

			// decrease degree
			this.rank--;

			if (this.rank == 0) this.child = null;

			child.parent = null;

			this.marker = false;
		}

		public double getKey() {
			return this.key;
		}

		public V getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append('[');
			str.append(this.value.toString());
			if (this.child != null) {
				Node<V> node = this.child;
				str.append(node.toString());
				node = node.right;
				while (node != this.child) {
					str.append(node.toString());
					node = node.right;
				}
			}
			str.append(']');
			return str.toString();
		}
	}

	private Node<V> root;
	private int size = 0;

	public FibonacciHeap() {
		this.root = null;
	}

	public int size() {
		return this.size;
	}

	public boolean isEmpty() {
		return this.root == null;
	}

	public Node<V> insert(final double key, final V value) {
		Node<V> node = new Node<V>(key, value);
		if (this.root != null) {
			/* insert the node into the root-list:
			 * root.left - node - root - root.right
			 */
			node.left = this.root.left;
			node.right = this.root;
			node.left.right = node;
			node.right.left = node;

			// point root to the node with minimal key
			if (node.key < this.root.key) {
				this.root = node;
			}

		} else {
			node.left = node;
			node.right = node;
			this.root = node;
		}

		this.size++;

		return node;
	}

	public Node<V> peek() {
		return this.root;
	}

	public Node<V> poll() { // aka "delete min"
		if (this.root == null) return null;

		Node<V> minNode = this.root;

		// remove root, insert root.childS at this position, and do some other magic
		/* before:   A - B(root) - C
		 *                 |
		 *                 B1 - B2 - B3 - B4
		 *
		 * after:   A - B1 - B2 - B3 - B4 - C
		 */

		// the important nodes
		Node<V> a = this.root.left;
		Node<V> c = this.root.right;
		Node<V> b1 = this.root.child;

		// remove the root from the root-list
		if (b1 != null) {
			// root has childs: replace root with its childs
			Node<V> b4 = b1.left;
			if (this.root == a) {
				// root is the only entry in the root list
				this.root = b1; // just point the root to root's childs
			} else {
				// insert root's childs into the root list
				a.right = b1;
				b1.left = a;
				c.left = b4;
				b4.right = c;
				this.root = a;
			}
		} else {
			// root has no childs
			this.root.left.right = this.root.right;
			this.root.right.left = this.root.left;
			this.root = this.root.right;
		}
		this.size--;

		if (this.size == 0) {
			this.root = null;
		} else {
			// just set another root, it mustn't be the min-node.
//			this.root = (this.root == this.root.left) ? this.root.child : this.root.left;

			// joinTrees fixes the tree structure and sets the correct root
			joinTrees();
		}

		// the node is no longer part of a list
		minNode.right = null;
		minNode.left = null;
		minNode.parent = null;

		return minNode;
	}

	public void decreaseKey(final Node<V> node, final double newKey) {
		node.key = newKey;
		cut(node);
		if (node.key < this.root.key) this.root = node;
	}

	public boolean delete(final Node<V> node) {
		if (node.right == null) return false;

		// move node into root-list
		cut(node);

		// now handle as if the node is the root and we poll it
		this.root = node;
		poll();
		return true;
	}

  /**
   * Consolidates the trees in the heap by joining trees of equal degree until
   * there are no more trees of equal degree in the root list.
   */
	private void joinTrees() {
//		Node<V> rankArray[] = new Node[this.size + 1];
		Map<Integer, Node<V>> rankArray = new HashMap<Integer, Node<V>>();

		// count how many elements we have in the root list
		Node<V> next = this.root;
		int counter = 0;
		do {
			counter++;
			next = next.right;
		} while (next != this.root);

		// step through the root list
		// we counted the elements before because we gonna change the root list now and we may make a mess during that
		next = this.root;
		for (int i = 0; i < counter; i++) {
			Node<V> node = next;
			next = next.right;
			int r = node.rank;
			Node<V> other = rankArray.get(r);
//			Node<V> other = rankArray[r];
			while (other != null) {
				if (node.key <= other.key) {
					node.addChild(other);
				} else {
					other.addChild(node);
					node = other;
				}

				rankArray.put(r, null);
//				rankArray[r] = null;
				r++; // adding a child increased the rank by one
				other = rankArray.get(r);
//				other = rankArray[r];
			}
			// there is yet no other node with rank r
			rankArray.put(r, node);
//			rankArray[r] = node;

			// check the next node/rank
			// --> done at the beginning of the loop
		}

		// now loop through the array (=new root list), and find the new root and adjust the parent-pointers
		double minKey = Double.POSITIVE_INFINITY;
//		for (int i = 0, n = rankArray.length; i < n; i++) {
//			Node<V> node = rankArray[i];
		for (Node<V> node : rankArray.values()) {
			if (node == null) continue;
			node.parent = null;
			if (minKey == Double.POSITIVE_INFINITY) {
				// special case for the first root-list entry we find
				minKey = node.key;
				this.root = node;
				node.left = node;
				node.right = node;
			} else {
				// insert node into the root-list: root - node - root.right
				node.right = this.root.right;
				node.left = this.root;
				node.right.left = node;
				node.left.right = node;
				if (minKey > node.key) {
					minKey = node.key;
					this.root = node;
				}
			}
		}
	}

	private void cut(final Node<V> node) {
		Node<V> parent = node.parent;

		if (parent == null) return;

		// remove node from its list
		node.right.left = node.left;
		node.left.right = node.right;
		if (parent.child == node) {
			if (node.left == node) {
				parent.child = null;
			} else {
				// find new minNode for parent
				parent.child = node.right;// why is this safe?? TODO [MR]
//				Node<V> child = node.right;
//				Node<V> minNode = child;
//				for (int i = 0; i < parent.child.rank - 2; i++) {
//					if (minNode.key > child.key) {
//						minNode = child;
//					}
//					child = child.right;
//				}
//				parent.child = minNode;
			}
		}

		// insert node in root list: root - node - root.right
		node.right = this.root.right;
		node.left = this.root;
		node.right.left = node;
		node.left.right = node;
		node.parent = null;
		node.marker = false;

		parent.rank--;

		if (parent.marker) {
			cut(parent);
		} else {
			parent.marker = true;
		}
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append('[');
		if (this.root != null) {
			Node<V> node = this.root;
			str.append(node.toString());
			node = node.right;
			while (node != this.root) {
				str.append(node.toString());
				node = node.right;
			}
		}
		str.append(']');
		return str.toString();
	}

}
