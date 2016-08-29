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
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import playground.agarwalamit.munich.utils.MunichPersonFilter;

/**
 * @author amit
 */

public class AreaFilter {

	private Collection<SimpleFeature> features;
	private boolean isSortingForShapeFile = false;

	private final static String MUNICH_SHAPE_FILE  = "../../../repos/shared-svn/projects/detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	/**
	 * @param shapeFile person will be soreted based on this shape file. In general this should be a polygon shape.
	 */
	public AreaFilter (String shapeFile){
		this.isSortingForShapeFile = true;
		this.features = ShapeFileReader.getAllFeatures(shapeFile);
	}

	/**
	 * if want to sort person for Munich city area.
	 */
	public AreaFilter (){
		this.features = ShapeFileReader.getAllFeatures(MUNICH_SHAPE_FILE);
		this.isSortingForShapeFile = true;
		Logger.getLogger(MunichPersonFilter.class).info("Reading Munich city area shape file...");
	}

	public boolean isCellInsideShape(Coord cellCentroid) {
		if(! this.isSortingForShapeFile) throw new RuntimeException("No shape file is assigned to check if the centroid falls inside it. Aborting ...");
		boolean isInsideMunich = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.features){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInsideMunich = true;
				break;
			}
		}
		return isInsideMunich;
	}

}
