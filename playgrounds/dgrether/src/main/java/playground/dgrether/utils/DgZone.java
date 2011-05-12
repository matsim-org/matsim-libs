/* *********************************************************************** *
 * project: org.matsim.*
 * GridCell
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
package playground.dgrether.utils;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * @author dgrether
 */
public class DgZone {

	private Polygon polygon;
	private Map<DgZone, Integer> toRelationships = new HashMap<DgZone, Integer>();
	private String id;
	
	public DgZone(String id, Polygon p) {
		this.id = id;
		polygon = p;
	}
	
	public String getId(){
		return this.id;
	}

	public Envelope getEnvelope() {
		return this.polygon.getEnvelope().getEnvelopeInternal();
	}
	
	public Map<DgZone, Integer> getToRelationships(){
		return this.toRelationships;
	}

	public void addToRelationship(DgZone endCell) {
		if (! toRelationships.containsKey(endCell)){
			this.toRelationships.put(endCell, 0);
		}
		Integer count = this.toRelationships.get(endCell);
		this.toRelationships.put(endCell, (count + 1));
	}

	public Polygon getPolygon() {
		return this.polygon;
	}
	
	public Coordinate getCenter(){
		return this.getEnvelope().centre();
	}
	
}