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

import java.util.ArrayList;
import java.util.TreeMap;

import org.matsim.gbl.Gbl;
import org.matsim.matrices.algorithms.MatricesAlgorithm;
import org.matsim.world.Layer;

public class Matrices {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private static Matrices singleton = new Matrices();

	private String name = null;
	private final TreeMap<String, Matrix> matrices = new TreeMap<String, Matrix>();
	private final ArrayList<MatricesAlgorithm> algorithms = new ArrayList<MatricesAlgorithm>();

	//////////////////////////////////////////////////////////////////////
	// Constructors
	//////////////////////////////////////////////////////////////////////

	private Matrices() {
	}

	//////////////////////////////////////////////////////////////////////
	// singleton access method
	//////////////////////////////////////////////////////////////////////

	public static final Matrices getSingleton() {
		return singleton;
	}

	//////////////////////////////////////////////////////////////////////
	// add methods
	//////////////////////////////////////////////////////////////////////

	public final void addAlgorithm(final MatricesAlgorithm algo) {
		this.algorithms.add(algo);
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public final void runAlgorithms() {
		for (int i = 0; i < this.algorithms.size(); i++) {
			MatricesAlgorithm algo = this.algorithms.get(i);
			algo.run(this);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// clear methods
	//////////////////////////////////////////////////////////////////////

	public final void clearAlgorithms() {
		this.algorithms.clear();
	}

	//////////////////////////////////////////////////////////////////////
	// create methods
	//////////////////////////////////////////////////////////////////////

	public final Matrix createMatrix(final String id, final String world_layer, final String desc) {
		// check id string for uniqueness
		if (this.matrices.containsKey(id)) {
			Gbl.errorMsg("[id="+id+" already exists.]");
		}
		// find layer in the world
		Layer layer = Gbl.getWorld().getLayer(world_layer);
		if (layer == null) {
			Gbl.errorMsg("[world_layer="+world_layer+" not found]");
		}
		// create the matrix
		Matrix m = new Matrix(id,layer,desc);
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
				"[nof_matrices=" + this.matrices.size() + "]" +
				"[nof_algorithms=" + this.algorithms.size() + "]";
	}
}
