/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
/**
 * A utility class to read in a multipolygon, such as a study area shapefile, given the
 * filename of the shapefile. To use the method, all associated files must be present,
 * and not only the <code>*.shp</code> file.
 * 
 * @author jwjoubert
 */
public class MyShapefileReader {
	private final String shapefileName; 
	
	/**
	 * Constructs a single instance of the <code>MyShapefileReader</code>. The class is
	 * used to read in a single study area. Should you wish to read in many shapefiles,
	 * such as the Geospatial Analsysis Platform (GAP) mesozones, rather use the class
	 * {@link MyGapReader}. 
	 * @param shapefileName the absolute path and name of the shapefile to be read.
	 */
	public MyShapefileReader(String shapefileName){
		this.shapefileName = shapefileName;
	}
	
	/**
	 * Reads the read shapefile as a multipolygon. An error message is given if the 
	 * shapefile is <b><i>not</i></b> a multipolygon.
	 * @return <code>MultiPolygon</code>
	 */
	public MultiPolygon readMultiPolygon() {
		MultiPolygon mp = null;
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(shapefileName)) {
			Object geo = f.getDefaultGeometry();
			if(geo instanceof MultiPolygon){
				mp = (MultiPolygon)geo;
			} else{
				throw new RuntimeException("The shapefile is not a MultiPolygon!");
			}
		}
		return mp;
	}
	
	public List<Point> readPoints() {
		List<Point> list = new ArrayList<Point>();
		for (SimpleFeature f : ShapeFileReader.getAllFeatures(this.shapefileName)) {
			Object geo = f.getDefaultGeometry();
			if(geo instanceof Point){
				Point ps = (Point)geo;
				
				for(int i = 0; i < ps.getNumGeometries(); i++){
					Point p = (Point) ps.getGeometryN(i);
					list.add(p);
				}
			} else{
				throw new RuntimeException("The shapefile does not contain Point(s)!");
			}
		}
		return list;
	}

	public String getShapefileName() {
		return shapefileName;
	}

}
