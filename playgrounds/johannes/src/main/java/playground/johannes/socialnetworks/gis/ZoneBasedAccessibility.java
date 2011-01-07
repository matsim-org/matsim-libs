/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneBasedAccessibility.java
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
package playground.johannes.socialnetworks.gis;

import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class ZoneBasedAccessibility {

	private SpatialCostFunction costFunction;

	private final GeometryFactory geoFactory = new GeometryFactory();

	private static final Logger logger = Logger.getLogger(ZoneBasedAccessibility.class);

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		args[1] = 
		
		ZoneBasedAccessibility access = new ZoneBasedAccessibility();
		access.costFunction = new GravityCostFunction(-1.6, 0, new CartesianDistanceCalculator());
		Set<Point> points = access.loadPoints("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.005.xml");

		Envelope env = PointUtils.envelope(points);
		SpatialGridKMLWriter writer = new SpatialGridKMLWriter();

		ZoneLayer targetLayer = ZoneLayerSHP.read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1G08.shp");
		targetLayer.overwriteCRS(CRSUtils.getCRS(21781));
		
		logger.info("Calculating accessibility...");
		SpatialGrid<Double> accessGridMacro = new SpatialGrid<Double>(env.getMinX(), env.getMinY(), env.getMaxX(), env
				.getMaxY(), 1000.0);

		access.fillTargetLayer(points, targetLayer);
		access.accessGrid(accessGridMacro, targetLayer);
		writer.write(accessGridMacro, CRSUtils.getCRS(21781), "/Users/jillenberger/Work/socialnets/spatialchoice/output/access.macro.zones.kmz");

	}

	private Set<Point> loadPoints(String file) {
		SpatialGraph g = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(file);
		Set<Point> points = new HashSet<Point>();
		for (SpatialVertex vertex : g.getVertices())
			points.add(vertex.getPoint());

		return points;
	}

	private ZoneLayer fillTargetLayer(Set<Point> points, ZoneLayer layer) {
		TObjectIntHashMap<Zone> counts = new TObjectIntHashMap<Zone>();
		for (Point point : points) {
			Zone zone = layer.getZone(point);
			if (zone != null)
				counts.adjustOrPutValue(zone, 1, 1);
		}

		for (Zone zone : layer.getZones()) {
			zone.setAttribute((double)counts.get(zone));
		}

		return layer;
	}

	private void accessGrid(SpatialGrid<Double> sourceGird, ZoneLayer targetLayer) {
		Map<Zone, Point> centroids = centroids(targetLayer);
		
		for (int i = 0; i < sourceGird.getNumRows(); i++) {
			for (int j = 0; j < sourceGird.getNumCols(i); j++) {
				double x = sourceGird.getXmin() + j * sourceGird.getResolution();
				double y = sourceGird.getYmin() + i * sourceGird.getResolution();
				Point source = geoFactory.createPoint(new Coordinate(x, y));
				sourceGird.setValue(i, j, cellAccessibility(source, targetLayer, centroids));
			}
		}
	}

	private double cellAccessibility(Point source, ZoneLayer targetLayer, Map<Zone, Point> centroids) {
		double sum = 0;

		for (Zone<Double> zone : targetLayer.getZones()) {
			double X_ij = zone.getAttribute();
			sum += X_ij * Math.exp(costFunction.costs(source, centroids.get(zone)));
		}

		return Math.log(sum);
	}
	
	private Map<Zone, Point> centroids(ZoneLayer targetLayer) {
		Map<Zone, Point> controids = new HashMap<Zone, Point>();
		for(Zone target : targetLayer.getZones()) {
			controids.put(target, target.getGeometry().getCentroid());
		}
		return controids;
	}
}
