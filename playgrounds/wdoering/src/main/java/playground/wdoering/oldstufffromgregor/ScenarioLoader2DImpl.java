/* *********************************************************************** *
 * project: org.matsim.*
 * ScenarioLoader2DImpl.java
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

package playground.wdoering.oldstufffromgregor;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

public class ScenarioLoader2DImpl  {

	private final Scenario scenarioData;

	private Sim2DConfigGroup sim2DConfig;

	private final MyDataContainer c;

	private final Config config;

	public ScenarioLoader2DImpl(Scenario scenario) {
		this.scenarioData = scenario;
		this.config = scenario.getConfig();
		this.c = new MyDataContainer();
		this.scenarioData.addScenarioElement(MyDataContainer.ELEMENT_NAME, this.c);
	}

	public void load2DScenario() {
		initSim2DConfigGroup();
		loadFloorShape();
	}

	private void initSim2DConfigGroup() {
		ConfigGroup module = this.config.getModule("sim2d");
		Sim2DConfigGroup s = null;
		if (module == null) {
			s = new Sim2DConfigGroup();
		} else {
			s = new Sim2DConfigGroup(module);
		}
		this.config.getModules().put("sim2d", s);
		this.sim2DConfig = (Sim2DConfigGroup) this.scenarioData.getConfig().getModule("sim2d");
	}

	private void loadFloorShape() {
		String file = this.sim2DConfig.getFloorShapeFile();
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(file);

		// Uh??? 8-S
		this.scenarioData.addScenarioElement( ShapeFileReader.class.getName(), reader);
		generateDenseCoords(reader);

		SegmentsFromGeometries segs = new SegmentsFromGeometries(reader);
		
		this.c.setFloatSegQuad(segs.getFloatSegQuadTree());
	}


	private void generateDenseCoords(ShapeFileReader reader) {
		Envelope e = reader.getBounds();
		QuadTree<Coordinate> quad = new QuadTree<Coordinate>(e.getMinX(),e.getMinY(),e.getMaxX(),e.getMaxY());

		List<Geometry> geos = new ArrayList<Geometry>();
		for (SimpleFeature ft : reader.getFeatureSet()) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			geos.add(geo);
		}
		DenseMultiPointFromGeometries dmp = new DenseMultiPointFromGeometries();
		MultiPoint mp = dmp.getDenseMultiPointFromGeometryCollection(geos);
		for (int i = 0; i < mp.getNumPoints(); i++) {
			Point p = (Point) mp.getGeometryN(i);
			quad.put(p.getX(), p.getY(), p.getCoordinate());
		}
		this.c.setDenseCoordsQuadTree(quad);
	}
}