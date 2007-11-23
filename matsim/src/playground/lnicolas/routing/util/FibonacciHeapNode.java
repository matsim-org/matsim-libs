/* *********************************************************************** *
 * project: org.matsim.*
 * FibonacciHeapNode.java
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

/**
 * Implements a node of the Fibonacci heap. It holds the information necessary
 * for maintaining the structure of the heap. It also holds the reference to the
 * key value (which is used to determine the heap structure).
 *
 * @author Nathan Fiedler
 */
public class FibonacciHeapNode<T>
{

    //~ Instance fields -------------------------------------------------------

    /**
     * Node data.
     */
    T data;

    /**
     * first child node
     */
    FibonacciHeapNode<T> child;

    /**
     * left sibling node
     */
    FibonacciHeapNode<T> left;

    /**
     * parent node
     */
    FibonacciHeapNode<T> parent;

    /**
     * right sibling node
     */
    FibonacciHeapNode<T> right;

    /**
     * true if this node has had a child removed since this node was added to
     * its parent
     */
    boolean mark;

    /**
     * key value for this node
     */
    double key;

    /**
     * number of children of this node (does not count grandchildren)
     */
    int degree;

    //~ Constructors ----------------------------------------------------------

    /**
     * Default constructor.  Initializes the right and left pointers, making
     * this a circular doubly-linked list.
     *
     * @param data data for this node
     * @param key initial key for node
     */
    public FibonacciHeapNode(T data, double key)
    {
        right = this;
        left = this;
        this.data = data;
        this.key = key;
    }

    //~ Methods ---------------------------------------------------------------

    /**
     * Obtain the key for this node.
     *
     * @return the key
     */
    public final double getKey()
    {
        return key;
    }

    /**
     * Obtain the data for this node.
     */
    public final T getData()
    {
        return data;
    }

    /**
     * Return the string representation of this object.
     *
     * @return string representing this object
     */
    public String toString()
    {
        if (true) {
            return Double.toString(key);
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append("Node=[parent = ");

            if (parent != null) {
                buf.append(Double.toString(parent.key));
            } else {
                buf.append("---");
            }

            buf.append(", key = ");
            buf.append(Double.toString(key));
            buf.append(", degree = ");
            buf.append(Integer.toString(degree));
            buf.append(", right = ");

            if (right != null) {
                buf.append(Double.toString(right.key));
            } else {
                buf.append("---");
            }

            buf.append(", left = ");

            if (left != null) {
                buf.append(Double.toString(left.key));
            } else {
                buf.append("---");
            }

            buf.append(", child = ");

            if (child != null) {
                buf.append(Double.toString(child.key));
            } else {
                buf.append("---");
            }

            buf.append(']');

            return buf.toString();
        }
    }

    // toString
}

// End FibonacciHeapNode.java
