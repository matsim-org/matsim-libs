/* *********************************************************************** *
 * project: org.matsim.*
 * SampledGraphBuilder.java
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
package playground.johannes.socialnetworks.snowball;

import org.matsim.contrib.sna.graph.AbstractSparseGraphBuilder;

/**
 * @author illenberger
 *
 */
public class SampledGraphBuilder extends AbstractSparseGraphBuilder<SampledGraph, SampledVertex, SampledEdge> {

	public SampledGraphBuilder() {
		super(new SampledGraphFactory());
	}
}
