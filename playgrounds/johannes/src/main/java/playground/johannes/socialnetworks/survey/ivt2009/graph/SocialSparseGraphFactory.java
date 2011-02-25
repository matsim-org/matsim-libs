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
import visad.data.netcdf.UnsupportedOperationException;

import com.vividsolutions.jts.geom.Point;


/**
 * @author illenberger
 *
 */
public class SocialSparseGraphFactory implements GraphFactory<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> {

	private static final Logger logger = Logger.getLogger(SocialSparseGraphFactory.class);
	
	private final CoordinateReferenceSystem crs;
	
	private final int SRID;
	
	public SocialSparseGraphFactory(CoordinateReferenceSystem crs) {
		this.crs = crs;
		SRID = CRSUtils.getSRID(crs);
		if(SRID == 0)
			logger.warn("Coordinate reference system has no SRID. Setting SRID to 0.");
	}
	
	public SocialSparseEdge createEdge() {
		return new SocialSparseEdge();
	}

	public SocialSparseGraph createGraph() {
		return new SocialSparseGraph(crs);
	}

	public SocialSparseVertex createVertex() {
		throw new UnsupportedOperationException();
	}
	
	public SocialSparseVertex createVertex(SocialPerson person, Point point) {
		if(point != null)
			point.setSRID(SRID);
		return new SocialSparseVertex(person, point);
	}

	@Override
	public SocialSparseGraph copyGraph(SocialSparseGraph graph) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SocialSparseVertex copyVertex(SocialSparseVertex vertex) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

	@Override
	public SocialSparseEdge copyEdge(SocialSparseEdge edge) {
		throw new UnsupportedOperationException("Seems like someone is using this method...");
	}

}
