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
		//		return;
		//		}

		//		if (Sim2DConfig.LOAD_NETWORK_FROM_XML_FILE) {
		//			super.loadNetwork();
		//			loadMps();
		//			loadLsMp();
		//		} else if (!Sim2DConfig.NETWORK_LOADERII) {
		//			NetworkLoader loader = new NetworkLoaderImpl(getScenario().getNetwork(), getScenario().getConfig().planCalcScore());
		//			this.mps = loader.getFloors();
		//			if (this.mps.size() > 1) {
		//				throw new RuntimeException("multiple floors are not supported yet");
		//			}
		//			FeatureType ft = initFeatureType();
		//			Collection<Feature> fts = new ArrayList<Feature>();
		//			int num = 0;
		//			for (MultiPolygon mp : this.mps.keySet()) {
		//				try {
		//					fts.add(ft.create(new Object[] { mp, num++ }));
		//				} catch (IllegalAttributeException e) {
		//					throw new RuntimeException(e);
		//				}
		//			}
		//			try {
		//				ShapeFileWriter.writeGeometries(fts, this.sim2DConfig.getFloorShapeFile());
		//			} catch (IOException e) {
		//				throw new RuntimeException(e);
		//			}
		//			new NetworkWriter(getScenario().getNetwork()).write(getScenario().getConfig().network().getInputFile());
		//		} else {
		//			NetworkLoader loader = new NetworkLoaderImplII(getScenario().getNetwork());
		//			loader.loadNetwork();
		//			new NetworkWriter(getScenario().getNetwork()).write(getScenario().getConfig().network().getInputFile());
		//			loadMps();
		//			loadLsMp();
		//		}
		//		loadStaticForceField();
	}

	private void loadLsMp() {
		FeatureSource fs = null;
		try {
			fs = ShapeFileReader.readDataFile(this.sim2DConfig.getLSShapeFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
		try {
			fs = ShapeFileReader.readDataFile(this.sim2DConfig.getFloorShapeFile());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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
		this.scenarioData.setStaticForceField(this.sff);
	}


}
