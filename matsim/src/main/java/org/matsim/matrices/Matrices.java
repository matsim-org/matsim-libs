/* *********************************************************************** *
 * project: org.matsim.*
 * Matrices.java
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

package org.matsim.matrices;

import java.util.TreeMap;

public class Matrices {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private String name = null;
	private final TreeMap<String, Matrix> matrices = new TreeMap<String, Matrix>();

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Matrix createMatrix(final String id, final String desc) {
		// check id string for uniqueness
		if (this.matrices.containsKey(id)) {
			throw new RuntimeException("[id="+id+" already exists.]");
		}
		// create the matrix
		Matrix m = new Matrix(id, desc);
		this.matrices.put(id,m);
		return m;
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	protected final void setName(final String name) {
		this.name = name;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final String getName() {
		return this.name;
	}

	public final TreeMap<String, Matrix> getMatrices() {
		return this.matrices;
	}

	public final Matrix getMatrix(final String id) {
		return this.matrices.get(id);
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[name=" + this.name + "]" +
				"[nof_matrices=" + this.matrices.size() + "]";
	}

}
