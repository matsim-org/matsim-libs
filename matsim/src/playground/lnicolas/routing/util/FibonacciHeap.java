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

package playground.lnicolas.routing.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


/**
 * This class implements a Fibonacci heap data structure. Much of the code in
 * this class is based on the algorithms in the "Introduction to Algorithms"by
 * Cormen, Leiserson, and Rivest in Chapter 21. The amortized running time of
 * most of these methods is O(1), making it a very fast data structure. Several
 * have an actual running time of O(1). removeMin() and delete() have O(log n)
 * amortized running times because they do the heap consolidation. If you
 * attempt to store nodes in this heap with key values of -Infinity
 * (Double.NEGATIVE_INFINITY) the <code>delete()</code> operation may fail to
 * remove the correct element.
 *
 * <p><b>Note that this implementation is not synchronized.</b> If multiple
 * threads access a set concurrently, and at least one of the threads modifies
 * the set, it <i>must</i> be synchronized externally. This is typically
 * accomplished by synchronizing on some object that naturally encapsulates the
 * set.</p>
 *
 * <p>This class was originally developed by Nathan Fiedler for the GraphMaker
 * project.  It was imported to JGraphT with permission, courtesy of Nathan
 * Fiedler.</p>
 *
 * @author Nathan Fiedler
 */
public class FibonacciHeap<T>
{

    //~ Instance fields -------------------------------------------------------

    /**
     * Points to the minimum node in the heap.
     */
    private FibonacciHeapNode<T> minNode;

    /**
     * Number of nodes in the heap.
     */
    private int nNodes;

    //~ Constructors ----------------------------------------------------------

    /**
     * Constructs a FibonacciHeap object that contains no elements.
     */
    public FibonacciHeap()
    {
    } // FibonacciHeap

    //~ Methods ---------------------------------------------------------------

    /**
     * Tests if the Fibonacci heap is empty or not. Returns true if the heap is
     * empty, false otherwise.
     *
     * <p>Running time: O(1) actual</p>
     *
     * @return true if the heap is empty, false otherwise
     */
    public boolean isEmpty()
    {
        return minNode == null;
    }

    // isEmpty

    /**
     * Removes all elements from this heap.
     */
    public void clear()
    {
        minNode = null;
        nNodes = 0;
    }

    // clear

    /**
     * Decreases the key value for a heap node, given the new value to take on.
     * The structure of the heap may be changed and will not be consolidated.
     *
     * <p>Running time: O(1) amortized</p>
     *
     * @param x node to decrease the key of
     * @param k new key value for node x
     *
     * @exception IllegalArgumentException Thrown if k is larger than x.key
     *                                     value.
     */
    public void decreaseKey(FibonacciHeapNode<T> x, double k)
    {
        if (k > x.key) {
            throw new IllegalArgumentException(
                "decreaseKey() got larger key value");
        }

        x.key = k;

        FibonacciHeapNode<T> y = x.parent;

        if ((y != null) && (x.key < y.key)) {
            cut(x, y);
            cascadingCut(y);
        }

        if (x.key < minNode.key) {
            minNode = x;
        }
    }

    // decreaseKey

    /**
     * Deletes a node from the heap given the reference to the node. The trees
     * in the heap will be consolidated, if necessary. This operation may fail
     * to remove the correct element if there are nodes with key value
     * -Infinity.
     *
     * <p>Running time: O(log n) amortized</p>
     *
     * @param x node to remove from heap
     */
    public void delete(FibonacciHeapNode<T> x)
    {
        // make x as small as possible
        decreaseKey(x, Double.NEGATIVE_INFINITY);

        // remove the smallest, which decreases n also
        removeMin();
    }

    // delete

    /**
     * Inserts a new data element into the heap. No heap consolidation is
     * performed at this time, the new node is simply inserted into the root
     * list of this heap.
     *
     * <p>Running time: O(1) actual</p>
     *
     * @param node new node to insert into heap
     * @param key key value associated with data object
     */
    public void insert(FibonacciHeapNode<T> node, double key)
    {
        node.key = key;

        // concatenate node into min list
        if (minNode != null) {
            node.left = minNode;
            node.right = minNode.right;
            minNode.right = node;
            node.right.left = node;

            if (key < minNode.key) {
                minNode = node;
            }
        } else {
            minNode = node;
        }

        nNodes++;
    }

    // insert

    /**
     * Returns the smallest element in the heap. This smallest element is the
     * one with the minimum key value.
     *
     * <p>Running time: O(1) actual</p>
     *
     * @return heap node with the smallest key
     */
    public FibonacciHeapNode<T> min()
    {
        return minNode;
    }

    // min

    /**
     * Removes the smallest element from the heap. This will cause the trees in
     * the heap to be consolidated, if necessary.
     *
     * <p>Running time: O(log n) amortized</p>
     *
     * @return node with the smallest key
     */
    public FibonacciHeapNode<T> removeMin()
    {
        FibonacciHeapNode<T> z = minNode;

        if (z != null) {
            int numKids = z.degree;
            FibonacciHeapNode<T> x = z.child;
            FibonacciHeapNode<T> tempRight;

            // for each child of z do...
            while (numKids > 0) {
                tempRight = x.right;

                // remove x from child list
                x.left.right = x.right;
                x.right.left = x.left;

                // add x to root list of heap
                x.left = minNode;
                x.right = minNode.right;
                minNode.right = x;
                x.right.left = x;

                // set parent[x] to null
                x.parent = null;
                x = tempRight;
                numKids--;
            }

            // remove z from root list of heap
            z.left.right = z.right;
            z.right.left = z.left;

            if (z == z.right) {
                minNode = null;
            } else {
                minNode = z.right;
                consolidate();
            }

            // decrement size of heap
            nNodes--;
        }

        return z;
    }

    // removeMin

    /**
     * Returns the size of the heap which is measured in the number of elements
     * contained in the heap.
     *
     * <p>Running time: O(1) actual</p>
     *
     * @return number of elements in the heap
     */
    public int size()
    {
        return nNodes;
    }

    // size

    /**
     * Joins two Fibonacci heaps into a new one. No heap consolidation is
     * performed at this time. The two root lists are simply joined together.
     *
     * <p>Running time: O(1) actual</p>
     *
     * @param h1 first heap
     * @param h2 second heap
     *
     * @return new heap containing h1 and h2
     */
    public static <T> FibonacciHeap<T> union(
        FibonacciHeap<T> h1,
        FibonacciHeap<T> h2)
    {
        FibonacciHeap<T> h = new FibonacciHeap<T>();

        if ((h1 != null) && (h2 != null)) {
            h.minNode = h1.minNode;

            if (h.minNode != null) {
                if (h2.minNode != null) {
                    h.minNode.right.left = h2.minNode.left;
                    h2.minNode.left.right = h.minNode.right;
                    h.minNode.right = h2.minNode;
                    h2.minNode.left = h.minNode;

                    if (h2.minNode.key < h1.minNode.key) {
                        h.minNode = h2.minNode;
                    }
                }
            } else {
                h.minNode = h2.minNode;
            }

            h.nNodes = h1.nNodes + h2.nNodes;
        }

        return h;
    }

    // union

    /**
     * Creates a String representation of this Fibonacci heap.
     *
     * @return String of this.
     */
    @Override
		public String toString()
    {
        if (minNode == null) {
            return "FibonacciHeap=[]";
        }

        // create a new stack and put root on it
        Stack<FibonacciHeapNode<T>> stack = new Stack<FibonacciHeapNode<T>>();
        stack.push(minNode);

        StringBuffer buf = new StringBuffer(512);
        buf.append("FibonacciHeap=[");

        // do a simple breadth-first traversal on the tree
        while (!stack.empty()) {
            FibonacciHeapNode<T> curr = stack.pop();
            buf.append(curr);
            buf.append(", ");

            if (curr.child != null) {
                stack.push(curr.child);
            }

            FibonacciHeapNode<T> start = curr;
            curr = curr.right;

            while (curr != start) {
                buf.append(curr);
                buf.append(", ");

                if (curr.child != null) {
                    stack.push(curr.child);
                }

                curr = curr.right;
            }
        }

        buf.append(']');

        return buf.toString();
    }

    // toString

    /**
     * Performs a cascading cut operation. This cuts y from its parent and then
     * does the same for its parent, and so on up the tree.
     *
     * <p>Running time: O(log n); O(1) excluding the recursion</p>
     *
     * @param y node to perform cascading cut on
     */
    protected void cascadingCut(FibonacciHeapNode<T> y)
    {
        FibonacciHeapNode<T> z = y.parent;

        // if there's a parent...
        if (z != null) {
            // if y is unmarked, set it marked
            if (!y.mark) {
                y.mark = true;
            } else {
                // it's marked, cut it from parent
                cut(y, z);

                // cut its parent as well
                cascadingCut(z);
            }
        }
    }

    // cascadingCut

    /**
     * Consolidates the trees in the heap by joining trees of equal degree until
     * there are no more trees of equal degree in the root list.
     *
     * <p>Running time: O(log n) amortized</p>
     */
    protected void consolidate()
    {
        int arraySize = nNodes + 1;
        List<FibonacciHeapNode<T>> array =
            new ArrayList<FibonacciHeapNode<T>>(arraySize);

        // Initialize degree array
        for (int i = 0; i < arraySize; i++) {
            array.add(null);
        }

        // Find the number of root nodes.
        int numRoots = 0;
        FibonacciHeapNode<T> x = minNode;

        if (x != null) {
            numRoots++;
            x = x.right;

            while (x != minNode) {
                numRoots++;
                x = x.right;
            }
        }

        // For each node in root list do...
        while (numRoots > 0) {
            // Access this node's degree..
            int d = x.degree;
            FibonacciHeapNode<T> next = x.right;

            // ..and see if there's another of the same degree.
            while (array.get(d) != null) {
                // There is, make one of the nodes a child of the other.
                FibonacciHeapNode<T> y = array.get(d);

                // Do this based on the key value.
                if (x.key > y.key) {
                    FibonacciHeapNode<T> temp = y;
                    y = x;
                    x = temp;
                }

                // FibonacciHeapNode<T> y disappears from root list.
                link(y, x);

                // We've handled this degree, go to next one.
                array.set(d, null);
                d++;
            }

            // Save this node for later when we might encounter another
            // of the same degree.
            array.set(d, x);

            // Move forward through list.
            x = next;
            numRoots--;
        }

        // Set min to null (effectively losing the root list) and
        // reconstruct the root list from the array entries in array[].
        minNode = null;

        for (int i = 0; i < arraySize; i++) {
            if (array.get(i) != null) {
                // We've got a live one, add it to root list.
                if (minNode != null) {
                    // First remove node from root list.
                    array.get(i).left.right = array.get(i).right;
                    array.get(i).right.left = array.get(i).left;

                    // Now add to root list, again.
                    array.get(i).left = minNode;
                    array.get(i).right = minNode.right;
                    minNode.right = array.get(i);
                    array.get(i).right.left = array.get(i);

                    // Check if this is a new min.
                    if (array.get(i).key < minNode.key) {
                        minNode = array.get(i);
                    }
                } else {
                    minNode = array.get(i);
                }
            }
        }
    }

    // consolidate

    /**
     * The reverse of the link operation: removes x from the child list of y.
     * This method assumes that min is non-null.
     *
     * <p>Running time: O(1)</p>
     *
     * @param x child of y to be removed from y's child list
     * @param y parent of x about to lose a child
     */
    protected void cut(FibonacciHeapNode<T> x, FibonacciHeapNode<T> y)
    {
        // remove x from childlist of y and decrement degree[y]
        x.left.right = x.right;
        x.right.left = x.left;
        y.degree--;

        // reset y.child if necessary
        if (y.child == x) {
            y.child = x.right;
        }

        if (y.degree == 0) {
            y.child = null;
        }

        // add x to root list of heap
        x.left = minNode;
        x.right = minNode.right;
        minNode.right = x;
        x.right.left = x;

        // set parent[x] to nil
        x.parent = null;

        // set mark[x] to false
        x.mark = false;
    }

    // cut

    /**
     * Make node y a child of node x.
     *
     * <p>Running time: O(1) actual</p>
     *
     * @param y node to become child
     * @param x node to become parent
     */
    protected void link(FibonacciHeapNode<T> y, FibonacciHeapNode<T> x)
    {
        // remove y from root list of heap
        y.left.right = y.right;
        y.right.left = y.left;

        // make y a child of x
        y.parent = x;

        if (x.child == null) {
            x.child = y;
            y.right = y;
            y.left = y;
        } else {
            y.left = x.child;
            y.right = x.child.right;
            x.child.right = y;
            y.right.left = y;
        }

        // increase degree[x]
        x.degree++;

        // set mark[y] false
        y.mark = false;
    }

    // link
}

// FibonacciHeap
