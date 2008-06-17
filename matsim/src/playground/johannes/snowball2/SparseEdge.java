/* *********************************************************************** *
 * project: org.matsim.*
 * SparseEdge.java
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
package playground.johannes.snowball2;

/**
 * @author illenberger
 *
 */
public class SparseEdge {

	private SparseVertex[] endPoints = new SparseVertex[2];
	
	protected SparseEdge(SparseVertex v1, SparseVertex v2) {
		endPoints[0] = v1;
		endPoints[1] = v2;
	}
	
	public SparseVertex[] getEndPoints() {
		return endPoints;
	}
	
	public SparseVertex getOpposite(SparseVertex v) {
		if(v == endPoints[0])
			return endPoints[1];
		else if(v == endPoints[1])
			return endPoints[0];
		else
			return null;
	}
}
