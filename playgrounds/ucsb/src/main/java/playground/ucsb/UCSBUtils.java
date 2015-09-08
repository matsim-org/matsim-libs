/* *********************************************************************** *
 * project: org.matsim.*
 * UCSBUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.ucsb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public abstract class UCSBUtils {

	private final static Logger log = Logger.getLogger(UCSBUtils.class);

	public final static Random r = MatsimRandom.getRandom();
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	public static final NumberFormat formatter = new DecimalFormat("#");
	
	public static final Point getRandomCoordinate(Envelope envelope) {
		double x = envelope.getMinX() + r.nextDouble() * envelope.getWidth();
		double y = envelope.getMinY() + r.nextDouble() * envelope.getHeight();
		return geometryFactory.createPoint(new Coordinate(x,y));
	}
	
	public static final Coord getRandomCoordinate(SimpleFeature feature) {
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();
		while (true) {
			Point point = getRandomCoordinate(envelope);
			if (point.within(geometry)) {
				return new Coord(point.getX(), point.getY());
			}
		}
	}
	
	public static final Map<String,SimpleFeature> getFeatureMap(String shapeFile, String idName) throws IOException {
		Map<String,SimpleFeature> features = new HashMap<String, SimpleFeature>();
		for (SimpleFeature f: ShapeFileReader.getAllFeatures(shapeFile)) {
			String id = f.getAttribute(idName).toString();
			if (features.put(id,f) != null) {
				throw new RuntimeException("idName="+idName+" is not a unique identifer (id="+id+" exists at least twice).");
			}
		}
		log.info(features.size()+" features stored.");
		return features;
	}
	
	public static final <T> Set<Id<T>> parseObjectIds(String idFile, Class<T> type) throws FileNotFoundException, IOException {
		Set<Id<T>> idSet = new HashSet<>();
		BufferedReader br = IOUtils.getBufferedReader(idFile);
		String curr_line;
		while ((curr_line = br.readLine()) != null) {
			Id<T> id = Id.create(curr_line.trim(), type);
			idSet.add(id);
		}
		return idSet;
	}
	
	public static final String getTimeStamp() {
        Calendar today = new GregorianCalendar();
        SimpleDateFormat df = new SimpleDateFormat();
        df.applyPattern("yyyyMMDD_HHMMss");
        return df.format(today.getTime());
	}
}
