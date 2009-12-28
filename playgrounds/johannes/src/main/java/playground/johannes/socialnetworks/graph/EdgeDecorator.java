/* *********************************************************************** *
 * project: org.matsim.*
 * EdgeDecorator.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.SparseEdge;

/**
 * Decorator class for any type of {@link Edge}.
 * 
 * @author illenberger
 * 
 */
public class EdgeDecorator<E extends Edge> extends SparseEdge {

	private E delegate;

	/**
	 * Creates a new decorator for edge <tt>delegate</tt>.
	 * 
	 * @param v1
	 *            one of the two vertices this edge is connected to.
	 * @param v2
	 *            one of the two vertices this edge is connected to.
	 *            
	 *  @deprecated
	 */
	@Deprecated
	protected EdgeDecorator(VertexDecorator<?> v1, VertexDecorator<?> v2,
			E delegate) {
		super(v1, v2);
		this.delegate = delegate;
	}
	
	protected EdgeDecorator(E delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns the decorated edge.
	 * 
	 * @return the decorated edge.
	 */
	public E getDelegate() {
		return delegate;
	}
}
