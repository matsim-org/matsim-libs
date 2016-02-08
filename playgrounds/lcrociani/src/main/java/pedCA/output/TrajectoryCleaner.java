/* *********************************************************************** *
 * project: org.matsim.*
 * TrajectoryCleaner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package pedCA.output;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

//JPSReport can only handle trajectory points that are locate inside the environment geometry
//this class discards all points outside the geometry 
public class TrajectoryCleaner {
	private final String inFile;
	private final String outFile;
	private final String geometryFile;
	
	private static double EPSILON = 0.01;
	private final List<List<Coordinate>> obstacles = new ArrayList<>();
	private final List<Coordinate> room = new ArrayList<>();
	
	private ShapeFileReader r;

	public TrajectoryCleaner(String inFile, String outFile, String geometryFile) {
		this.inFile = inFile;
		this.outFile = outFile;
		this.geometryFile = geometryFile;
	}
	
	public void run() {
		this.r = new ShapeFileReader();
		this.r.readFileAndInitialize(this.geometryFile);
		
		createRoomAndObstacles();
		
		try {
			cleanTra();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void cleanTra() throws IOException {
		Polygon lr = getRing();
		lr = (Polygon) lr.buffer(-0.1);
		BufferedReader tBr = new BufferedReader(new FileReader(new File(this.inFile)));
		BufferedWriter oW = new BufferedWriter(new FileWriter(new File(this.outFile)));
		String l = tBr.readLine();
		while (l != null) {
			String[] expl = StringUtils.explode(l, '\t');
			if (expl.length == 5 && !l.startsWith("#")) {
				double x = Double.parseDouble(expl[2]);
				double y = Double.parseDouble(expl[3]);

				Point p = MGC.xy2Point(x, y);
				if (lr.contains(p)) {
					oW.append(l);
				} else {
					l = tBr.readLine();
					continue;
				}
			} else {
				oW.append(l);
			}
			oW.append('\n');
			l = tBr.readLine();
		}
		tBr.close();
		oW.close();
	}
	
	private Polygon getRing() {
		GeometryFactory geoFac = new GeometryFactory();
		Coordinate [] coords = new Coordinate[this.room.size()];
		int idx = 0;
		for (Coordinate c : this.room) {
			coords[idx] = c;
			idx++;
		}

		LinearRing lr = geoFac.createLinearRing(coords);
		Polygon p = geoFac.createPolygon(lr, null);
		return p;
	}
	
	private void createRoomAndObstacles() {
		//1. obstacles (obstacles are neglected)
		Iterator<SimpleFeature> it = this.r.getFeatureSet().iterator();
		while (it.hasNext()){
			SimpleFeature ft = it.next();
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			Coordinate[] cs = geo.getCoordinates();
			if (dist(cs[0],cs[cs.length-1]) < EPSILON) {
				List<Coordinate> obs = new ArrayList<>();
				this.obstacles.add(obs);
				for (Coordinate c: cs) {
					obs.add(c);
				}
				it.remove();
			} 
		}
		//2. boundary
		LinkedList<Coordinate> ring = new LinkedList<>();
		Iterator<SimpleFeature> it2 = this.r.getFeatureSet().iterator();
		SimpleFeature first = it2.next();
		it2.remove();
		Geometry firstGeo = (Geometry) first.getDefaultGeometry();
		for (Coordinate c : firstGeo.getCoordinates()) {
			ring.add(c);
		}

		while (dist(ring.getFirst(),ring.getLast()) > EPSILON) {
			while (it2.hasNext()) {
				SimpleFeature next = it2.next();
				Geometry nextGeo = (Geometry) next.getDefaultGeometry();
				if (dist(ring.getLast(),nextGeo.getCoordinates()[0]) < EPSILON) {
					addStraightAway(ring,nextGeo);
					it2.remove();
					break;
				} else if (dist(ring.getLast(),nextGeo.getCoordinates()[nextGeo.getCoordinates().length-1]) < EPSILON) {
					addReverse(ring,nextGeo);
					it2.remove();
					break;
				}

			}
			System.out.println(this.r.getFeatureSet().size());
			it2 = this.r.getFeatureSet().iterator();
		}
		ring.getLast().setCoordinate(ring.getFirst());
		this.room.addAll(ring);

	}
	
	private void addReverse(LinkedList<Coordinate> ring, Geometry nextGeo) {
		for (int i = nextGeo.getCoordinates().length-2; i >= 0; i--) {
			ring.add(nextGeo.getCoordinates()[i]);
		}

	}

	private void addStraightAway(LinkedList<Coordinate> ring, Geometry nextGeo) {
		for (int i = 1; i < nextGeo.getCoordinates().length; i++) {
			ring.add(nextGeo.getCoordinates()[i]);
		}

	}

	private double dist(Coordinate c1, Coordinate c2) {
		double dx = c2.x-c1.x;
		double dy = c2.y-c1.y;
		return Math.sqrt(dx*dx+dy*dy);
	}
}
