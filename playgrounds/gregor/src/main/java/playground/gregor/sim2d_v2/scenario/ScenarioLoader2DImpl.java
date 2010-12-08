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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import org.geotools.data.FeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.AttributeType;
import org.geotools.feature.AttributeTypeFactory;
import org.geotools.feature.DefaultAttributeTypeFactory;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.FeatureTypeFactory;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.network.NetworkFromLsFile;
import playground.gregor.sim2d_v2.network.NetworkLoader;
import playground.gregor.sim2d_v2.network.NetworkLoaderImpl;
import playground.gregor.sim2d_v2.network.NetworkLoaderImplII;
import playground.gregor.sim2d_v2.simulation.floor.StaticForceField;
import playground.gregor.sim2d_v2.simulation.floor.StaticForceFieldGenerator;
import playground.gregor.sim2d_v2.simulation.floor.StaticForceFieldReader;
import playground.gregor.sim2d_v2.simulation.floor.StaticForceFieldWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;

public class ScenarioLoader2DImpl extends ScenarioLoaderImpl {

	private Map<MultiPolygon, List<Link>> mps;

	private StaticForceField sff;

	private HashMap<Id, LineString> lsmp;

	private final Scenario2DImpl scenarioData;

	private Queue<Event> phantomPopulation;

	public ScenarioLoader2DImpl(Scenario2DImpl scenarioData) {
		super(scenarioData);
		this.scenarioData = scenarioData;
	}

	@Override
	public void loadNetwork() {
		if (Sim2DConfig.NETWORK_LOADER_LS) {
			loadLsMp();
			NetworkFromLsFile loader = new NetworkFromLsFile(getScenario(), this.lsmp);
			loader.loadNetwork();
			loadMps();
			loadStaticForceField();
			return;
		}

		if (Sim2DConfig.LOAD_NETWORK_FROM_XML_FILE) {
			super.loadNetwork();
			loadMps();
			loadLsMp();
		} else if (!Sim2DConfig.NETWORK_LOADERII) {
			NetworkLoader loader = new NetworkLoaderImpl(getScenario().getNetwork(), getScenario().getConfig().charyparNagelScoring());
			this.mps = loader.getFloors();
			if (this.mps.size() > 1) {
				throw new RuntimeException("multiple floors are not supported yet");
			}
			FeatureType ft = initFeatureType();
			Collection<Feature> fts = new ArrayList<Feature>();
			int num = 0;
			for (MultiPolygon mp : this.mps.keySet()) {
				try {
					fts.add(ft.create(new Object[] { mp, num++ }));
				} catch (IllegalAttributeException e) {
					throw new RuntimeException(e);
				}
			}
			try {
				ShapeFileWriter.writeGeometries(fts, Sim2DConfig.FLOOR_SHAPE_FILE);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			new NetworkWriter(getScenario().getNetwork()).write(getScenario().getConfig().network().getInputFile());
		} else {
			NetworkLoader loader = new NetworkLoaderImplII(getScenario().getNetwork());
			loader.loadNetwork();
			new NetworkWriter(getScenario().getNetwork()).write(getScenario().getConfig().network().getInputFile());
			loadMps();
			loadLsMp();
		}
		loadStaticForceField();
	}

	private void loadLsMp() {
		FeatureSource fs = null;
		try {
			fs = ShapeFileReader.readDataFile(Sim2DConfig.LS_SHAPE_FILE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Iterator it = null;
		try {
			it = fs.getFeatures().iterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.lsmp = new HashMap<Id, LineString>();

		while (it.hasNext()) {
			Feature ft = (Feature) it.next();
			Id id = new IdImpl((Long) ft.getAttribute("ID"));
			this.lsmp.put(id, (LineString) ft.getDefaultGeometry().getGeometryN(0));

		}

		this.scenarioData.setLineStringMap(this.lsmp);

	}

	private void loadMps() {
		FeatureSource fs = null;
		try {
			fs = ShapeFileReader.readDataFile(Sim2DConfig.FLOOR_SHAPE_FILE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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

	private FeatureType initFeatureType() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(TransformationFactory.WGS84_UTM33N);
		AttributeType p = DefaultAttributeTypeFactory.newAttributeType("MultiPolygon", MultiPolygon.class, true, null, null, targetCRS);
		AttributeType num = AttributeTypeFactory.newAttributeType("floor_nr", Integer.class);

		Exception ex;
		try {
			return FeatureTypeFactory.newFeatureType(new AttributeType[] { p, num }, "MultiPolygon");
		} catch (FactoryRegistryException e) {
			ex = e;
		} catch (SchemaException e) {
			ex = e;
		}
		throw new RuntimeException(ex);

	}

	private void loadStaticForceFieldII(Map<Id, List<Id>> linkSubLinkMapping) {
		// this.ssff = new
		// StaticForceFieldGeneratorIII(linkSubLinkMapping).getStaticForceField();
		// this.scenarioData.setSegmentedStaticForceField(this.ssff);

	}

	private void loadStaticForceField() {
		if (Sim2DConfig.LOAD_STATIC_FORCE_FIELD_FROM_FILE) {
			this.sff = new StaticForceFieldReader(Sim2DConfig.STATIC_FORCE_FIELD_FILE).getStaticForceField();
		} else {
			// this.sff = new
			// StaticForceFieldGenerator(this.mps.keySet().iterator().next()).loadStaticForceField();
			this.sff = new StaticForceFieldGenerator(this.mps.keySet().iterator().next()).loadStaticForceField();
			new StaticForceFieldWriter().write(Sim2DConfig.STATIC_FORCE_FIELD_FILE, this.sff);
		}
		this.scenarioData.setStaticForceField(this.sff);
		// new StaticForceFieldToShape(this.sff).createShp();
	}

	public void setPhantomPopulationEventsFile(String file) {
		this.phantomPopulation = new PhantomPopulationLoader(file).getPhantomPopulation();
		this.scenarioData.setPhantomPopulation(this.phantomPopulation);
	}

}
