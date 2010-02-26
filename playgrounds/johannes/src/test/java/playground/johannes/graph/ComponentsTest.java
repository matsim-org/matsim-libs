/* *********************************************************************** *
 * project: org.matsim.*
 * ComponentsTest.java
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
package playground.johannes.graph;

import junit.framework.TestCase;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;

import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.graph.analysis.Components;

/**
 * @author illenberger
 *
 */
public class ComponentsTest extends TestCase {

	public void test() {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph graph = reader.readGraph("/Users/jillenberger/Work/shared-svn/projects/socialnets/data/socialnetworks/cond-mat-2005/cond-mat-2005.graphml");
		Components components = new Components();
		long time = System.currentTimeMillis();
		System.out.println(String.valueOf(components.countComponents(graph)));
		System.out.println("Time " + (System.currentTimeMillis() - time));
		
		time = System.currentTimeMillis();
		System.out.println(Partitions.disconnectedComponents(graph).size());
		System.out.println("Time " + (System.currentTimeMillis() - time));
	}
}
