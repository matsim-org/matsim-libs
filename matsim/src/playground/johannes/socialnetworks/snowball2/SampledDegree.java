/* *********************************************************************** *
 * project: org.matsim.*
 * SampledDegree.java
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
package playground.johannes.socialnetworks.snowball2;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Collection;

import playground.johannes.socialnetworks.graph.Degree;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class SampledDegree<V extends SampledVertex> extends Degree<V> {

	@Override
	public Distribution distribution(Collection<V> vertices) {
		return super.distribution(SnowballPartitions.createSampledPartition(vertices));
	}

	@Override
	public TObjectDoubleHashMap<V> values(Collection<V> vertices) {
		return super.values(SnowballPartitions.createSampledPartition(vertices));
	}

}
