/* *********************************************************************** *
 * project: org.matsim.*
 * AttributePartition.java
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
package playground.johannes.socialnetworks.graph.analysis;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectDoubleHashMap;
import gnu.trove.TObjectDoubleIterator;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.statistics.Discretizer;

/**
 * @author illenberger
 *
 */
public class AttributePartition {

	private Discretizer discritizer;
	
	public AttributePartition(Discretizer discretizer) {
		this.discritizer = discretizer;
	}
	
	
	public <V extends Vertex> TDoubleObjectHashMap<Set<V>> partition(TObjectDoubleHashMap<V> vertices) {
		TDoubleObjectHashMap<Set<V>> partitions = new TDoubleObjectHashMap<Set<V>>();
		
		TObjectDoubleIterator<V> it = vertices.iterator();
		for(int i = 0; i < vertices.size(); i++) {
			it.advance();
			double bin = discritizer.discretize(it.value());
			Set<V> partition = partitions.get(bin);
			if(partition == null) {
				partition = new HashSet<V>();
				partitions.put(bin, partition);
			}
			partition.add(it.key());
		}
		return partitions;
	}

}
