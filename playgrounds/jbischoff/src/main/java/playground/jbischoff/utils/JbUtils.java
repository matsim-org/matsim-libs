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

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
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
		return readShapeFileAndExtractGeometry(filename, "SCHLUESSEL");
	}

	public static Map<String,Geometry> readShapeFileAndExtractGeometry(String filename, String key){
		
		Map<String,Geometry> geometry = new TreeMap<>();	
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(filename)) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
					String lor = ft.getAttribute(key).toString();
					geometry.put(lor, geo);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			 
		}	
		return geometry;
	}
	
	public static <K,V> void map2Text(Map<K,V> map, String filename, String delim, String headerLine){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try { 
			bw.write(headerLine);
			for (Entry<K,V> e: map.entrySet()){
				bw.newLine();
				bw.write(e.getKey().toString()+delim+e.getValue().toString());
				
			}
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static <T> void collection2Text(Collection<T> c, String filename){
		collection2Text(c, filename, null);
	}

	public static <T> void collection2Text(Collection<T> c, String filename, String header){
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			if (header!=null){
				bw.write(header);
				bw.newLine();
			}
			for (Iterator<T> iterator = c.iterator(); iterator.hasNext();) {
				
				bw.write(iterator.next().toString());
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static Coord getCoordCentroid(Set<Coord> coords){
		double x=0;
		double y=0;
		for (Coord c : coords){
			x+=c.getX();
			y+=c.getY();
		}
		x = x/coords.size();
		y = y/coords.size();
		return new Coord(x,y);
		
	}
	
	

}
