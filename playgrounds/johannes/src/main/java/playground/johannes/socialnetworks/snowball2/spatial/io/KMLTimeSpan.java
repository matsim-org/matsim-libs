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

import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;

/**
 * @author illenberger
 *
 */
public class KMLTimeSpan implements KMLObjectDetail {

	private ObjectFactory factory = new ObjectFactory();
	
	private Map<?, String> timeStamps;
	
	public KMLTimeSpan(Map<?, String> timeStamps) {
		this.timeStamps = timeStamps;
	}

	@Override
	public void addDetail(PlacemarkType kmlPlacemark, Object obj) {
		TimeSpanType tType = factory.createTimeSpanType();
		
		String timeStamp = timeStamps.get(obj);
		if(timeStamp == null)
			timeStamp = "200000";
		tType.setBegin(timeStamp);
		kmlPlacemark.setAbstractTimePrimitiveGroup(factory.createTimeSpan(tType));
//		kmlPlacemark.setName(timeStamp);
	}

}
