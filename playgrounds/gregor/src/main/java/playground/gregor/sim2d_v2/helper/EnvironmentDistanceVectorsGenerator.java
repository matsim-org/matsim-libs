/* *********************************************************************** *
 * project: org.matsim.*
 * EnvironmentDistanceVectorsGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.gregor.sim2d_v2.helper;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesWriter;
import playground.gregor.sim2d_v2.simulation.floor.EnvironmentDistances;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

/**
 * @author laemmel
 * 
 */
@Deprecated //depcrecated use EnvironmentDistanceVectorsGeneratorIII instead
public class EnvironmentDistanceVectorsGenerator {

	private static final Logger log = Logger.getLogger(EnvironmentDistanceVectorsGenerator.class);

	private static final double incr = 2 * Math.PI / 16;

	private final MultiPolygon structure;
	private Envelope envelope;
	private QuadTree<EnvironmentDistances> distancesQuadTree;

	private final GeometryFactory geofac = new GeometryFactory();

	private final double res;

	private final double sens;



	public EnvironmentDistanceVectorsGenerator(MultiPolygon geo,
			double sensingRange, double res) {
		this.structure = (MultiPolygon) geo.buffer(0);
		this.res = res;
		this.sens = sensingRange;
	}

	public StaticEnvironmentDistancesField loadDistanceVectors() {
		intQuadTree();
		calculateDistanceVectors();
		return new StaticEnvironmentDistancesField(this.distancesQuadTree,this.res,this.sens);
	}

	/**
	 * 
	 */
	private void calculateDistanceVectors() {
		int loop = 0;
		int yloop = 0;
		for (double x = this.envelope.getMinX(); x <= this.envelope.getMaxX(); x += this.res) {
			log.info("xloop:" + ++loop + "  yloop:" + yloop);
			for (double y = this.envelope.getMinY(); y <= this.envelope.getMaxY(); y += this.res) {
				yloop++;
				Point point = this.geofac.createPoint(new Coordinate(x, y));
				if (!this.structure.covers(point) && this.structure.distance(point) > 0.05) {
					EnvironmentDistances ed = scanEnvironment(x, y);
					if (ed != null) {
						try {
							this.distancesQuadTree.put(x, y, ed);
						} catch (Exception e) {
							e.printStackTrace();
							throw new RuntimeException(e);
						}

					}

				}
			}

		}

	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	private EnvironmentDistances scanEnvironment(double x, double y) {
		Coordinate location = new Coordinate(x, y, 0);
		EnvironmentDistances ed = new EnvironmentDistances(location);
		double alpha = 0;
		for (; alpha < 2 * Math.PI;) {
			Coordinate[] coords = new Coordinate[4];
			coords[0] = location;

			double cos = Math.cos(alpha);
			double sin = Math.sin(alpha);

			double x1 = x + cos * this.sens;
			double y1 = y + sin * this.sens;
			Coordinate c1 = new Coordinate(x1, y1);
			coords[1] = c1;

			alpha += incr;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x2 = x + cos * this.sens;
			double y2 = y + sin * this.sens;
			Coordinate c2 = new Coordinate(x2, y2);
			coords[2] = c2;
			coords[3] = location;

			calcAndAddSectorObject(ed, coords);


		}

		return ed;
	}

	/**
	 * @param ed
	 * @param coords
	 * @return void
	 */
	private void calcAndAddSectorObject(EnvironmentDistances ed, Coordinate[] coords) {
		Polygon p = this.geofac.createPolygon(this.geofac.createLinearRing(coords), null);
		Geometry g = this.structure.intersection(p);
		if ((g instanceof MultiPolygon) || !(g instanceof GeometryCollection)) {
			DistanceOp op = new DistanceOp(g, this.geofac.createPoint(coords[0]));
			Coordinate[] tmp = op.closestPoints();
			double fX = tmp[1].x - tmp[0].x;
			double fY = tmp[1].y - tmp[0].y;
			double dist = Math.sqrt(Math.pow(fX, 2) + Math.pow(fY, 2));
			if (dist > this.sens) {
				throw new RuntimeException("this should not happen!!");
			} else if (dist <= 0.01) {
				return;
			}
			ed.addEnvironmentDistanceLocation(tmp[0]);
		}

	}

	private void intQuadTree() {
		Geometry geo = this.structure.getEnvelope();
		this.envelope = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			this.envelope.expandToInclude(c);
		}
		this.distancesQuadTree = new QuadTree<EnvironmentDistances>(this.envelope.getMinX() - 1000, this.envelope.getMinY() - 1000, this.envelope.getMaxX() + 1000, this.envelope.getMaxY() + 1000);
	}

	public static void main(String[] args) throws IOException {

		String cf = args[0];
		Config c = ConfigUtils.loadConfig(cf);
		Module module = c.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		c.getModules().put("sim2d", s);


		//		String shape = "test/input/playground/gregor/sim2d_v2/Controller2DTest/testController2D/90grad.shp";
		@SuppressWarnings("unchecked")
		Iterator<Feature> fs = ShapeFileReader.readDataFile(s.getFloorShapeFile()).getFeatures().iterator();



		double sensingRange = 5;
		double res = 0.05;
		while (fs.hasNext()) {
			Feature ft = fs.next();
			Geometry geo = ft.getDefaultGeometry();
			StaticEnvironmentDistancesField fl = new EnvironmentDistanceVectorsGenerator((MultiPolygon) geo,sensingRange,res).loadDistanceVectors();
			//			System.out.println(tree.size());
			new EnvironmentDistancesWriter().write(s.getStaticEnvFieldFile(), fl);
		}
	}

}
