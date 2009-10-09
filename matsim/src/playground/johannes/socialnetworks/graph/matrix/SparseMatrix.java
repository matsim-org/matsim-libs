/* *********************************************************************** *
 * project: org.matsim.*
 * SparseMatrix.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.matrix;

import gnu.trove.TIntIntHashMap;

/**
 * @author illenberger
 *
 */
public class SparseMatrix {

//	private int rows;
	
	private int columns;
	
	private TIntIntHashMap elements;
	
	public SparseMatrix(int rows, int columns) {
		elements = new TIntIntHashMap();
		this.columns = columns;
	}
	
	private int getIndex(int row, int col) {
		return row * columns + col;
	}
	
	public void set(int row, int col, int value) {
		elements.put(getIndex(row, col), value);
	}
	
	public int get(int row, int col) {
		return elements.get(getIndex(row, col));
	}
}
