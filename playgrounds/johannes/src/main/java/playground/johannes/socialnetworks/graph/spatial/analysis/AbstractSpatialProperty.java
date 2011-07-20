/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractSpatialProperty.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.graph.analysis.AbstractVertexProperty;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.gis.DistanceCalculator;

import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 *
 */
public abstract class AbstractSpatialProperty extends AbstractVertexProperty {

	private Set<Point> targets;
	
	protected DistanceCalculator calculator;
	
	public void setTargets(Set<Point> targets) {
		this.targets = targets;
	}
	
	public void setDistanceCalculator(DistanceCalculator calculator) {
		this.calculator = calculator;
	}
	
	protected Set<Point> getTargets(Set<? extends SpatialVertex> vertices) {
		if(targets == null) {
			targets = new HashSet<Point>(vertices.size());
			for(SpatialVertex vertex : vertices) {
				Point p = vertex.getPoint();
				if(p != null)
					targets.add(p);
			}
		}
		
		return targets;
	}

}
