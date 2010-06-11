/* *********************************************************************** *
 * project: org.matsim.*
 * KMLSnowballDescriptor.java
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

import net.opengis.kml._2.PlacemarkType;

import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;
import org.matsim.contrib.sna.snowball.SampledVertex;

/**
 * @author illenberger
 *
 */
public class KMLSnowballDescriptor implements KMLObjectDetail {
	
	@Override
	public void addDetail(PlacemarkType kmlPlacemark, Object object) {
		StringBuilder builder = new StringBuilder();
		builder.append("Detected: ");
		builder.append(String.valueOf(((SampledVertex)object).getIterationDetected()));
		builder.append("<br>");
		builder.append("Sampled: ");
		builder.append(String.valueOf(((SampledVertex)object).getIterationSampled()));
		
		kmlPlacemark.setDescription(builder.toString());
		
	}

}
