/* *********************************************************************** *
 * project: org.matsim.*
 * KMLNeighbors.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import net.opengis.kml._2.PlacemarkType;

import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail;

import playground.johannes.socialnetworks.graph.social.SocialVertex;

/**
 * @author illenberger
 *
 */
public class KMLNeighbors<V extends SocialVertex> implements KMLObjectDetail<V> {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetail#addDetail(net.opengis.kml._2.PlacemarkType, java.lang.Object)
	 */
	@Override
	public void addDetail(PlacemarkType kmlPlacemark, V object) {
		StringBuilder builder = new StringBuilder();
		
		builder.append(kmlPlacemark.getDescription());
		
		builder.append("<b>neihbors:<b>");
		for(SocialVertex neighbor : object.getNeighbours()) {
			builder.append(neighbor.getPerson().getId().toString());
			builder.append(" ");
		}
		
		kmlPlacemark.setDescription(builder.toString());
		
	}

}
