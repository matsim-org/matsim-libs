/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package org.matsim.core.network.algorithms.intersectionSimplifier;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

/** 
 * Converts a {@link Geometry}, specifically a {@link Polygon} to a sequence
 * of coordinates, each representing a boundary point of the polygon. This is 
 * used to write a concave hull's characteristics to an {@link ObjectAttributes}
 * file for facilities, or simply as a {@link Facility}'s {@link Attributes}. 
 *
 * @author jwjoubert
 */
public class HullConverter implements AttributeConverter<Geometry> {
	private final Logger log = Logger.getLogger(HullConverter.class);

	@Override
	public Geometry convert(String value) {
		GeometryFactory gf = new GeometryFactory();
		Geometry g;

		List<Coordinate> list = new ArrayList<Coordinate>();
		String[] sa = value.split(",");
		for(String s : sa){
			String[] sa2 = s.substring(1, s.length()-1).split(";");
			double x = Double.parseDouble(sa2[0]);
			double y = Double.parseDouble(sa2[1]);
			list.add(new Coordinate(x, y));
		}

		Coordinate[] ca = new Coordinate[list.size()];
		
		/* Distinguish between points, lines and polygons. */
		if(ca.length == 1){
			ca[0] = list.get(0);
			g = gf.createPoint(ca[0]);
		} else if(ca.length == 2){
			ca[0] = list.get(0);
			ca[1] = list.get(1);
			g = gf.createLineString(ca);
		} else{
			for(int i = 0; i < list.size(); i++){
				ca[i] = list.get(i);
			}
			g = gf.createPolygon(gf.createLinearRing(ca), null);
		}
		
		return g;
	}

	@Override
	public String convertToString(Object o) {
		if(!(o instanceof Geometry)){
			log.error("Could not convert the geometry: it is not of type Geometry. Returning empty string.");
			return "";
		}
		
		/* Convert to the format: (x1;y1),(x2;y2),...,(xn;yn) */
		Coordinate[] ca = ((Geometry)o).getCoordinates();
		String s = new String();
		for(int i = 0; i < ca.length-1; i++){
			s += "(";
			s += ca[i].x;
			s += ";";
			s += ca[i].y;
			s += "),";
		}
		s += "(";
		s += ca[ca.length-1].x;
		s += ";";
		s += ca[ca.length-1].y;
		s += ")";
		
		return s;
	}

}
