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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.helper.EnvironmentDistanceVectorsGeneratorII;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesReader;
import playground.gregor.sim2d_v2.network.NetworkFromLsFile;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

@Deprecated //should be a stand-alone class and not inherited from ScenarioLoaderImpl
public class ScenarioLoader2DImpl extends ScenarioLoaderImpl {

	//
	//
	private StaticEnvironmentDistancesField sff;
	//
	//	private HashMap<Id, LineString> lsmp;

	private final Scenario scenarioData;


	private final Sim2DConfigGroup sim2DConfig;

	public ScenarioLoader2DImpl(Scenario scenarioData) {
		super(scenarioData);
		this.scenarioData = scenarioData;
		MyDataContainer c = new MyDataContainer();
		this.scenarioData.addScenarioElement(c);
		this.sim2DConfig = (Sim2DConfigGroup) scenarioData.getConfig().getModule("sim2d");
	}

	@Override
	public void loadNetwork() {
		//		if (Sim2DConfig.NETWORK_LOADER_LS) {
		loadLsMp();
		NetworkFromLsFile loader = new NetworkFromLsFile(getScenario(), this.scenarioData.getScenarioElement(MyDataContainer.class).getLineStringMap());
		loader.loadNetwork();
		loadMps();
		loadStaticEnvironmentDistancesField();
	}

	private void loadLsMp() {
		FeatureSource fs = ShapeFileReader.readDataFile(this.sim2DConfig.getLSShapeFile());

		@SuppressWarnings("rawtypes")
		Iterator it = null;
		try {
			it = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


		int idd = 0;
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Id id = new IdImpl(idd++);
			this.scenarioData.getScenarioElement(MyDataContainer.class).getLineStringMap().put(id, (LineString) ft.getDefaultGeometry().getGeometryN(0));

		}

	}

	private void loadMps() {
		FeatureSource fs = ShapeFileReader.readDataFile(this.sim2DConfig.getFloorShapeFile());

		@SuppressWarnings("rawtypes")
		Iterator it = null;
		try {
			it = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		Feature ft = (Feature) it.next();
		if (it.hasNext()) {
			throw new RuntimeException("multiple floors are not supported yet");
		}
		Geometry geo = ft.getDefaultGeometry();
		if (!(geo instanceof MultiPolygon)) {
			throw new RuntimeException("MultiPolygon expected but got:" + geo);
		}
		List<Link> links = new ArrayList<Link>(super.getScenario().getNetwork().getLinks().values());
		this.scenarioData.getScenarioElement(MyDataContainer.class).getMps().put((MultiPolygon) geo, links);
	}



	private void loadStaticEnvironmentDistancesField() {
		if (this.sim2DConfig.getStaticEnvFieldFile() == null) {
			generateStaticEnvironmentDistancesField();
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

	private void generateStaticEnvironmentDistancesField() {
		EnvironmentDistanceVectorsGeneratorII gen = new EnvironmentDistanceVectorsGeneratorII(this.scenarioData.getConfig());
		gen.setResolution(.20);
		gen.setIncr(2*Math.PI/8);
		this.sff = gen.generate();

	}


}
