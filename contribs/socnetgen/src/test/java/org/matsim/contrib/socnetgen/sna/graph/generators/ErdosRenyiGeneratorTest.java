/* *********************************************************************** *
 * project: org.matsim.*
 * ErdosRenyiGeneratorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.generators;

import junit.framework.TestCase;
import org.matsim.contrib.socnetgen.sna.graph.*;

/**
 * @author jillenberger
 *
 */
public class ErdosRenyiGeneratorTest extends TestCase {

	public void test() {
		ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge> generator = new ErdosRenyiGenerator<SparseGraph, SparseVertex, SparseEdge>(new SparseGraphBuilder());
		Graph g = generator.generate(100, 0.01, 0);
		
		assertEquals(100, g.getVertices().size());
		assertEquals(56, g.getEdges().size());
	}
}
