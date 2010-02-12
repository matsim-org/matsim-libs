/* *********************************************************************** *
 * project: org.matsim.*
 * SampledSocialNetFactory.java
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
package playground.johannes.socialnetworks.survey.ivt2009.graph;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.GraphFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.socialnetworks.graph.social.SocialPerson;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SampledSocialGraphFactory implements GraphFactory<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> {

	private static final Logger logger = Logger.getLogger(SampledSocialGraphFactory.class);
	
	private final CoordinateReferenceSystem crs;
	
	private final int SRID;
	
	public SampledSocialGraphFactory(CoordinateReferenceSystem crs) {
		this.crs = crs;
		SRID = CRSUtils.getSRID(crs);
		if(SRID == 0)
			logger.warn("Coordinate reference system has no SRID. Setting SRID to 0.");
	}
	
	public SampledSocialEdge createEdge() {
		return new SampledSocialEdge();
	}

	public SampledSocialGraph createGraph() {
		return new SampledSocialGraph(crs);
	}
	
//	public SampledSocialGraph createGraph(CoordinateReferenceSystem crs) {
//		return new SampledSocialGraph(crs);
//	}

	public SampledSocialVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SampledSocialVertex createVertex(SocialPerson person, Point point) {
		point.setSRID(SRID);
		return new SampledSocialVertex(person, point);
	}

}
