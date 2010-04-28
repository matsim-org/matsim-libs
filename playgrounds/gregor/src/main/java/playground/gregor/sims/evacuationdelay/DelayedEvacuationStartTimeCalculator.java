/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.gregor.sims.evacuationdelay;

import java.io.IOException;
import java.util.Iterator;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.evacuation.base.EvacuationStartTimeCalculator;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class DelayedEvacuationStartTimeCalculator implements EvacuationStartTimeCalculator {

	private final double baseTime;
	private QuadTree<Feature> coordQuadTree;
	private final Network network;

	public DelayedEvacuationStartTimeCalculator(double earliestEvacTime, String evacDecsionZonesFile, Network network) {
		this.baseTime = earliestEvacTime;
		this.network = network;
		try {
			loadShapeFile(evacDecsionZonesFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void loadShapeFile(String evacDecsionZonesFile) throws IOException {
		FeatureSource fs = ShapeFileReader.readDataFile(evacDecsionZonesFile);
		Envelope e = fs.getBounds();
		this.coordQuadTree = new QuadTree<Feature>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());
		Iterator it = fs.getFeatures().iterator();
		while (it.hasNext()) {
			Feature f = (Feature) it.next();
			Coord c = MGC.coordinate2Coord(f.getDefaultGeometry().getCentroid().getCoordinate());
			this.coordQuadTree.put(c.getX(), c.getY(), f);
		}

	}

	@Override
	public double getEvacuationStartTime(Activity act) {
		Coord c = act.getCoord();
		if (c == null) {
			c = this.network.getLinks().get(act.getLinkId()).getCoord();
		}
		Feature ft = this.coordQuadTree.get(c.getX(), c.getY());

		Geometry geo = ft.getDefaultGeometry();
		Point p = MGC.coord2Point(c);

		try {
			if (!geo.contains(p)) {
				ft = getFt(p);
			}
			return this.baseTime + getOffset(ft);
		} catch (Exception e) {
			return this.baseTime;
		}


	}


	private Feature getFt(Point p) {
		for (Feature ft : this.coordQuadTree.values()) {
			if (ft.getDefaultGeometry().contains(p)) {
				return ft;
			}
		}
		return null;
	}

	private double getOffset(Feature ft) {
		double immed = ((Double) ft.getAttribute("EVACIMMED")) /100;
		double delay = ((Double) ft.getAttribute("EVACDELAY")) /100;
//		double noevac = ((Double) ft.getAttribute("NOEVAC")) /100;
		double rand = MatsimRandom.getRandom().nextDouble();
		if (rand <= immed) {
			return 0;
		}
		rand -= immed;
		if (rand < delay) {
			return 60 * 20;
		}
		return 60 * 28;
	}
//	private double getOffset(double dist) {
//		if (dist <= 3000) {
//			double rnd = MatsimRandom.getRandom().nextDouble();
//			if (rnd <= .214) {
//				return 25 * 60;
//			} else if (rnd <= .631 ) {
//				return 15 * 60;
//			} else {
//				return .0;
//			}
//		} else {
//			double rnd = MatsimRandom.getRandom().nextDouble();
//			if (rnd <= .357) {
//				return 25 * 60;
//			} else if (rnd <= .596 ) {
//				return 15 * 60;
//			} else {
//				return .0;
//			}
//		}
//	}

}
