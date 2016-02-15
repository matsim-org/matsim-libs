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

package playground.jbischoff.utils;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * @author  jbischoff
 *
 */
public class JbUtils {

	
	public static int getHour(double time){
		int hour = (int) Math.floor(time/(3600));
		if (hour>23){
			hour = hour%24;
		}
		return hour;
	}
	
	public static Map<String,Geometry> readShapeFileAndExtractGeometry(String filename){
		
		Map<String,Geometry> geometry = new TreeMap<>();	
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
					String lor = ft.getAttribute("SCHLUESSEL").toString();
					geometry.put(lor, geo);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			 
		}	
		return geometry;
	}
}
