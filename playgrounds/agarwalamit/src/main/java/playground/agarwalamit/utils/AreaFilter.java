/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.utils;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author amit
 */

public class AreaFilter {

	private Collection<Geometry> features;
	private final static String MUNICH_SHAPE_FILE  = "../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	/**
	 * @param shapeFile person will be soreted based on this shape file. In general this should be a polygon shape.
	 */
	public AreaFilter (final String shapeFile){
		this.features = GeometryUtils.getSimplifiedGeometries( ShapeFileReader.getAllFeatures(shapeFile) );
	}

	/**
	 * if want to sort person for Munich city area.
	 */
	public AreaFilter (){
		this.features = GeometryUtils.getSimplifiedGeometries( ShapeFileReader.getAllFeatures(MUNICH_SHAPE_FILE) );
		Logger.getLogger(AreaFilter.class).info("Reading Munich city area shape file...");
	}

	public boolean isCellInsideShape(final Coord cellCentroid) {
		return GeometryUtils.isCoordInsideShare(features, cellCentroid);
	}
	
	public boolean isLinkInsideShape(final Link link) {
		return GeometryUtils.isLinkInsideGeometries(features, link);
	}

}
