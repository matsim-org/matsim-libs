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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.xml.sax.SAXException;

import playground.gregor.sim2d_v2.config.Sim2DConfigGroup;
import playground.gregor.sim2d_v2.helper.EnvironmentDistanceVectorsGeneratorII;
import playground.gregor.sim2d_v2.io.EnvironmentDistancesReader;
import playground.gregor.sim2d_v2.network.NetworkFromLsFile;
import playground.gregor.sim2d_v2.simulation.floor.StaticEnvironmentDistancesField;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

public class ScenarioLoader2DImpl extends ScenarioLoaderImpl {

	private Map<MultiPolygon, List<Link>> mps;

	private StaticEnvironmentDistancesField sff;

	private HashMap<Id, LineString> lsmp;

	private final Scenario2DImpl scenarioData;


	private final Sim2DConfigGroup sim2DConfig;

	public ScenarioLoader2DImpl(Scenario2DImpl scenarioData) {
		super(scenarioData);
		this.scenarioData = scenarioData;
		this.sim2DConfig = (Sim2DConfigGroup) scenarioData.getConfig().getModule("sim2d");
	}

	@Override
	public void loadNetwork() {
		//		if (Sim2DConfig.NETWORK_LOADER_LS) {
		loadLsMp();
		NetworkFromLsFile loader = new NetworkFromLsFile(getScenario(), this.lsmp);
		loader.loadNetwork();
		loadMps();
		loadStaticEnvironmentDistancesField();
	}

	private void loadLsMp() {
		FeatureSource fs = null;
		fs = ShapeFileReader.readDataFile(this.sim2DConfig.getLSShapeFile());

		@SuppressWarnings("rawtypes")
		Iterator it = null;
		try {
			it = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.lsmp = new HashMap<Id, LineString>();
		int idd = 0;
		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Id id = new IdImpl(idd++);
			this.lsmp.put(id, (LineString) ft.getDefaultGeometry().getGeometryN(0));

		}

		this.scenarioData.setLineStringMap(this.lsmp);

	}

	private void loadMps() {
		FeatureSource fs = null;
		fs = ShapeFileReader.readDataFile(this.sim2DConfig.getFloorShapeFile());

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
		this.mps = new HashMap<MultiPolygon, List<Link>>();
		this.mps.put((MultiPolygon) geo, links);
		this.scenarioData.setFloorLinkMapping(this.mps);

	}



	private void loadStaticEnvironmentDistancesField() {
		if (this.sim2DConfig.getStaticEnvFieldFile() == null) {
			generateStaticEnvironmentDistancesField();
		} else  {
			loadStaticEnvironmentDistancesField(this.sim2DConfig.getStaticEnvFieldFile());
		}


		this.scenarioData.setStaticForceField(this.sff);
	}

	private void loadStaticEnvironmentDistancesField(String staticEnvFieldFile) {


		EnvironmentDistancesReader r = new EnvironmentDistancesReader();
		try {
			r.setValidating(false);
			r.parse(this.sim2DConfig.getStaticEnvFieldFile());
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.sff = r.getEnvDistField();

	}

	private void generateStaticEnvironmentDistancesField() {
		EnvironmentDistanceVectorsGeneratorII gen = new EnvironmentDistanceVectorsGeneratorII(this.scenarioData.getConfig());
		gen.setResolution(.20);
		gen.setIncr(2*Math.PI/8);
		this.sff = gen.generate();

	}


}
