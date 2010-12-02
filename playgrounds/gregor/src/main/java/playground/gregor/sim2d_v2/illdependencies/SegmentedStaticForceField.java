/* *********************************************************************** *
 * project: org.matsim.*
 * SegmentedStaticForceField.java
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
package playground.gregor.sim2d_v2.illdependencies;

import java.util.Collection;
import java.util.Map;

import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;


import com.vividsolutions.jts.geom.Coordinate;

public class SegmentedStaticForceField {

	
	private Map<Id, QuadTree<Force>> quads;

	public SegmentedStaticForceField(Map<Id,QuadTree<Force>> quads){
		this.quads = quads;
	}

	public Force getForceWithin(Coordinate location, Id id,	double range) {
		QuadTree<Force> quad = this.quads.get(id);
		Collection<Force> coll = quad.get(location.x, location.y,range/2);
		if (coll.size() > 0) {
//			if (coll.size() > 1) {
//				System.err.println("coll:" + coll.size());
//			}
			Force f = coll.iterator().next();
			if (f == null) {
				throw new RuntimeException();
			} else if (Double.isNaN(f.getFx()) || Double.isNaN(f.getFy())) {
				throw new RuntimeException();
			}
			return f;
		}
				
		return null;
	}
}
