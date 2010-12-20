/* *********************************************************************** *
 * project: org.matsim.*
 * StaticForceFieldGenerator.java
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
package playground.gregor.sim2d_v2.simulation.floor;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class StaticForceFieldGenerator {

	private static final Logger log = Logger.getLogger(StaticForceFieldGenerator.class);

	private static final double incr = 2 * Math.PI / 32;

	private final MultiPolygon structure;

	private final GeometryFactory geofac = new GeometryFactory();

	private StaticForceField ret;

	boolean loaded = false;

	private QuadTree<ForceLocation> forcesQuadTree;

	private Envelope envelope;

	public StaticForceFieldGenerator(MultiPolygon structure) {
		this.structure = (MultiPolygon) structure.buffer(0);
	}

	public StaticForceField loadStaticForceField() {
		if (this.loaded) {
			return this.ret;
		}
		intQuadTree();
		calculateForces();
		return new StaticForceField(this.forcesQuadTree);
	}

	private void calculateForces() {
		int loop = 0;
		int yloop = 0;
		for (double x = this.envelope.getMinX(); x <= this.envelope.getMaxX(); x += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
			log.info("xloop:" + ++loop + "  yloop:" + yloop);
			for (double y = this.envelope.getMinY(); y <= this.envelope.getMaxY(); y += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
				yloop++;
				Point point = this.geofac.createPoint(new Coordinate(x, y));
				if (!this.structure.covers(point) && this.structure.distance(point) > 0.1) {
					ForceLocation f = calculateForce(x, y);
					if (f != null) {
						try {

							this.forcesQuadTree.put(x, y, f);
						} catch (Exception e) {
							e.printStackTrace();
							GisDebugger.dump("/home/laemmel/devel/dfg/tmp/staticForcesDbg.shp");
							throw new RuntimeException(e);
						}

					}

				}
			}
		}
		GisDebugger.dump("/home/laemmel/devel/dfg/tmp/staticForcesDbg.shp");
	}

	private ForceLocation calculateForce(double x, double y) {

		Force f = new Force();
		ForceLocation ret = new ForceLocation(f, new Coordinate(x, y));

		Coordinate c0 = new Coordinate(x, y);
		double alpha = 0;
		double cos = 0;
		double sin = 0;
		int contr = 0;
		for (; alpha < 2 * Math.PI;) {
			Coordinate[] coords = new Coordinate[4];
			coords[0] = c0;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x1 = x + cos * Sim2DConfig.Bw;
			double y1 = y + sin * Sim2DConfig.Bw;
			Coordinate c1 = new Coordinate(x1, y1);
			coords[1] = c1;

			alpha += incr;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x2 = x + cos * Sim2DConfig.Bw;
			double y2 = y + sin * Sim2DConfig.Bw;
			Coordinate c2 = new Coordinate(x2, y2);
			coords[2] = c2;
			coords[3] = c0;

			if (calcAndAddSectorForce(f, coords)) {
				contr++;
			}
		}
		if (contr == 0) {
			return null;
		}

		f.setXComponent(f.getXComponent() / contr);
		f.setYComponent(f.getYComponent() / contr);

		Coordinate[] cooo = new Coordinate[] { new Coordinate(x, y), new Coordinate(x + 0.01, y + 0.01), new Coordinate(x + f.getXComponent(), y + f.getYComponent()), new Coordinate(x, y) };
		LinearRing lr = this.geofac.createLinearRing(cooo);
		Polygon ppp = this.geofac.createPolygon(lr, null);
		GisDebugger.addGeometry(ppp);

		return ret;
	}

	private boolean calcAndAddSectorForce(Force f, Coordinate[] coords) {
		Polygon p = this.geofac.createPolygon(this.geofac.createLinearRing(coords), null);
		Geometry g = this.structure.intersection(p);
		if (!(g instanceof GeometryCollection)) {
			DistanceOp op = new DistanceOp(g, this.geofac.createPoint(coords[0]));
			Coordinate[] tmp = op.closestPoints();
			double fX = tmp[1].x - tmp[0].x;
			double fY = tmp[1].y - tmp[0].y;
			double dist = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
			if (dist > Sim2DConfig.Bw) {
				throw new RuntimeException("this should not happen!!");
			} else if (dist <= 0.01) {
				return false;
			}

			// //DEBUG
			// GisDebugger.addGeometry(p);
			// Coordinate[] tmp1 = new Coordinate[]{tmp[0],new
			// Coordinate(tmp[0].x+0.01,tmp[0].y+0.01),tmp[1],tmp[0]};
			// LinearRing lr = this.geofac.createLinearRing(tmp1);
			// Polygon tmpP = this.geofac.createPolygon(lr, null);
			// GisDebugger.addGeometry(tmpP);

			double exp = Math.exp(Sim2DConfig.Bw / dist);
			fX *= exp / dist;
			fY *= exp / dist;
			// fX /= dist;
			// fY /= dist;

			// double l2 = Math.sqrt(Math.pow(fX, 2)+Math.pow(fY, 2));
			// System.out.println(dist + " " + l2);

			f.incrementX(fX);
			f.incrementY(fY);

			return true;
		}

		return false;

	}

	private void intQuadTree() {
		Geometry geo = this.structure.getEnvelope();
		this.envelope = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			this.envelope.expandToInclude(c);
		}
		this.forcesQuadTree = new QuadTree<ForceLocation>(this.envelope.getMinX() - 1000, this.envelope.getMinY() - 1000, this.envelope.getMaxX() + 1000, this.envelope.getMaxY() + 1000);
	}

}
