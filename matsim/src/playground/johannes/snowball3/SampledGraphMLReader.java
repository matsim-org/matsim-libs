/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphMLReader.java
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
package playground.johannes.snowball3;

import org.xml.sax.Attributes;

import playground.johannes.graph.GraphMLReader;
import playground.johannes.graph.SparseGraph;

/**
 * @author illenberger
 *
 */
public class SampledGraphMLReader extends GraphMLReader {

	@Override
	protected SparseGraph newGraph(Attributes attrs) {
		return new SampledGraph();
	}

	@Override
	public SampledGraph readGraph(String file) {
		return (SampledGraph) super.readGraph(file);
	}

	
}
