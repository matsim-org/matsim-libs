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
package playground.gregor.sim2d_v2.scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.helper.EnvironmentDistanceVectorsGeneratorIII;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesReader;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class ScenarioLoader2DImpl  {

	private StaticEnvironmentDistancesField sff;

	private final Scenario scenarioData;


	private final Sim2DConfigGroup sim2DConfig;

	public ScenarioLoader2DImpl(Scenario scenario) {
		this.scenarioData = scenario;
		MyDataContainer c = new MyDataContainer();
		this.scenarioData.addScenarioElement(c);
		this.sim2DConfig = (Sim2DConfigGroup) this.scenarioData.getConfig().getModule("sim2d");
	}

	public void load2DScenario() {

		loadStaticEnvironmentDistancesField();
	}


	//	private void loadLsMp() {
	//		FeatureSource fs = ShapeFileReader.readDataFile(this.sim2DConfig.getLSShapeFile());
	//
	//		@SuppressWarnings("rawtypes")
	//		Iterator it = null;
	//		try {
	//			it = fs.getFeatures().iterator();
	//		} catch (IOException e) {
	//			throw new RuntimeException(e);
	//		}
	//
	//
	//		int idd = 0;
	//		while (it.hasNext()) {
	//			Feature ft = (Feature) it.next();
	//			Id id = new IdImpl(idd++);
	//			this.scenarioData.getScenarioElement(MyDataContainer.class).getLineStringMap().put(id, (LineString) ft.getDefaultGeometry().getGeometryN(0));
	//
	//		}
	//
	//	}




	private void loadStaticEnvironmentDistancesField() {

		if (this.sim2DConfig.getStaticEnvFieldFile() == null) {
			throw new RuntimeException("this mode is not longer supported!");
		} else  {
			loadStaticEnvironmentDistancesField(this.sim2DConfig.getStaticEnvFieldFile());
		}


		this.scenarioData.addScenarioElement(this.sff);

	}

	private void loadStaticEnvironmentDistancesField(String staticEnvFieldFile) {


		EnvironmentDistancesReader r = new EnvironmentDistancesReader();
		r.setValidating(false);
		r.parse(this.sim2DConfig.getStaticEnvFieldFile());
		this.sff = r.getEnvDistField();

	}

	//	private void generateStaticEnvironmentDistancesField() {
	//		EnvironmentDistanceVectorsGeneratorIII gen = new EnvironmentDistanceVectorsGeneratorIII(this.scenarioData.getConfig());
	//		gen.setResolution(.20);
	//		gen.setIncr(2*Math.PI/8);
	//		this.sff = gen.generate();
	//
	//	}


}
