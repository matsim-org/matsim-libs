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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public abstract class UCSBUtils {

	private final static Logger log = Logger.getLogger(UCSBUtils.class);

	private final static Random r = MatsimRandom.getRandom();
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	public static final NumberFormat formatter = new DecimalFormat("#");
	
	public static final Point getRandomCoordinate(Envelope envelope) {
		double x = envelope.getMinX() + r.nextDouble() * envelope.getWidth();
		double y = envelope.getMinY() + r.nextDouble() * envelope.getHeight();
		return geometryFactory.createPoint(new Coordinate(x,y));
	}
	
	public static final Coord getRandomCoordinate(Feature feature) {
		Geometry geometry = feature.getDefaultGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();
		while (true) {
			Point point = getRandomCoordinate(envelope);
			if (point.within(geometry)) {
				return new CoordImpl(point.getX(),point.getY());
			}
		}
	}
	
	public static final Map<String,Feature> getFeatureMap(String shapeFile, String idName) throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(shapeFile);
		Map<String,Feature> features = new HashMap<String, Feature>(fs.getFeatures().size());
		for (Object o: fs.getFeatures()) {
			Feature f = (Feature)o;
			String id = f.getAttribute(idName).toString();
			if (features.put(id,f) != null) {
				Gbl.errorMsg("idName="+idName+" is not a unique identifer (id="+id+" exists at least twice).");
			}
		}
		log.info(features.size()+" features stored.");
		return features;
	}
	
	public static final Set<Id> parseObjectIds(String idFile) throws FileNotFoundException, IOException {
		Set<Id> idSet = new HashSet<Id>();
		BufferedReader br = IOUtils.getBufferedReader(idFile);
		String curr_line;
		while ((curr_line = br.readLine()) != null) {
			Id id = new IdImpl(curr_line.trim());
			idSet.add(id);
		}
		return idSet;
	}
}
