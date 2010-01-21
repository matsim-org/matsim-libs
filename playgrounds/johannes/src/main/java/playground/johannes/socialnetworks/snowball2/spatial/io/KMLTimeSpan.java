/* *********************************************************************** *
 * project: org.matsim.*
 * KMLTimeSpan.java
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
package playground.johannes.socialnetworks.snowball2.spatial.io;

import java.util.Map;

import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.TimeSpanType;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;

/**
 * @author illenberger
 *
 */
public class KMLTimeSpan implements KMLObjectDetail<SpatialVertex> {

	private ObjectFactory factory = new ObjectFactory();
	
	private Map<Vertex, String> timeStamps;
	
	public KMLTimeSpan(Map<Vertex, String> timeStamps) {
		this.timeStamps = timeStamps;
	}
	

	@Override
	public void addDetail(PlacemarkType kmlPlacemark, SpatialVertex vertex) {
		TimeSpanType tType = factory.createTimeSpanType();
		
		String timeStamp = timeStamps.get(vertex);
		tType.setBegin(timeStamp.replace(" ", "T"));
//		tType.setBegin(String.valueOf(((SampledVertex)vertex).getIterationDetected()+2000));
		kmlPlacemark.setAbstractTimePrimitiveGroup(factory.createTimeSpan(tType));
		
	}

}
