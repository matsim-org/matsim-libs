/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimNet2Shape.java
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

/**
 *
 */
package playground.yu.utils.qgis;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class founds on many codes of Gregor Laemmel. man should for this "playground.yu.integration.cadyts.demandCalibration.withCarCounts.run"
 * install com.sun.media.jai and javax.media.jai from http://jai.dev.java.net
 * 
 * @author ychen
 * 
 */
public class MATSimNet2QGIS implements X2QGIS {

	/**
	 * this class is only a copy of
	 * <class>playground.gregor.shapeFileToMATSim.ShapeFileWriter</class> Gregor
	 * Laemmel's
	 * 
	 * @author ychen
	 * 
	 */
	public static class ShapeFileWriter2 {

		@SuppressWarnings("deprecation")
		public static void writeGeometries(final Collection<Feature> features,
				final String filename) throws IOException, FactoryException,
				SchemaException {
			ShapefileDataStore datastore = new ShapefileDataStore(new File(
					filename).toURI().toURL());
			FeatureType ft = features.iterator().next().getFeatureType();
			datastore.createSchema(ft);
			((FeatureStore) datastore.getFeatureSource(ft.getTypeName()))
					.addFeatures(DataUtilities.reader(features));
		}
	}

	protected static double flowCapFactor = 0.1;
	protected Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
	protected CoordinateReferenceSystem crs = null;
	protected X2GraphImpl n2g = null;

	protected MATSimNet2QGIS() {
	}

	public MATSimNet2QGIS(String netFilename, String coordRefSys) {
		new MatsimNetworkReader(scenario).readFile(netFilename);
		crs = MGC.getCRS(coordRefSys);
		n2g = new Network2PolygonGraph(getNetwork(), crs);
	}

	public MATSimNet2QGIS(Scenario scenario, String coordRefSys) {
		this.scenario = scenario;
		crs = MGC.getCRS(coordRefSys);
		n2g = new Network2PolygonGraph(getNetwork(), crs);
	}

	public MATSimNet2QGIS(String netFilename, String coordRefSys,
			Set<Id> linkIds2paint) {
		this(netFilename, coordRefSys);
		if (linkIds2paint != null) {
			Set<Link> links2paint = new HashSet<Link>();
			Map<Id, Link> linkImpls = (Map<Id, Link>) getNetwork().getLinks();
			for (Id linkId : linkIds2paint) {
				links2paint.add(linkImpls.get(linkId));
			}
			((Network2PolygonGraph) this.n2g).setLinks2paint(links2paint);
		}
	}

	/**
	 * @param flowCapFactor
	 *            the flowCapFactor to set
	 */
	public static void setFlowCapFactor(final double flowCapacityFactor) {
		flowCapFactor = flowCapacityFactor;
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	public void writeShapeFile(final String ShapeFilename) {
		try {
			ShapeFileWriter2.writeGeometries(n2g.getFeatures(), ShapeFilename);
		} catch (FactoryRegistryException e) {
			e.printStackTrace();
		} catch (SchemaException e) {
			e.printStackTrace();
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (FactoryException e) {
			e.printStackTrace();
		}
	}

	// /////////////////////////////
	public void addParameter(final String paraName, final Class<?> clazz,
			final Map<Id, ?> parameters) {
		n2g.addParameter(paraName, clazz, parameters);
	}

	// /////////////////////////////
	/**
	 * @return the network
	 */
	public Network getNetwork() {
		return scenario.getNetwork();
	}

	public void readEvents(final String eventsFilename,
			final EventHandler[] handlers) {
		EventsManagerImpl events = new EventsManagerImpl();
		for (EventHandler handler : handlers)
			events.addHandler(handler);
		new MatsimEventsReader(events).readFile(eventsFilename);
	}

	public void readPlans(final String plansFilename,
			final AbstractPersonAlgorithm pa) {
		new MatsimPopulationReader(scenario).readFile(plansFilename);
		pa.run(scenario.getPopulation());
	}

	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	/**
	 * @param n2g
	 *            the n2g to set
	 */
	public void setN2g(final X2Graph n2g) {
		this.n2g = (X2GraphImpl) n2g;
	}

	public static void main(final String[] args) {
		String netFilename = "../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";

		Set<Id> linkIds2paint = null;
		// -------------------------------------------------------------
		linkIds2paint = new HashSet<Id>();
		// B-344 H
		linkIds2paint.add(new IdImpl("initial_792040"));
		linkIds2paint.add(new IdImpl("10292"));
		linkIds2paint.add(new IdImpl("10294R"));
		linkIds2paint.add(new IdImpl("3972"));
		linkIds2paint.add(new IdImpl("3974"));
		linkIds2paint.add(new IdImpl("3956R"));
		linkIds2paint.add(new IdImpl("3955"));
		linkIds2paint.add(new IdImpl("3959R"));
		linkIds2paint.add(new IdImpl("3960"));
		linkIds2paint.add(new IdImpl("3894R"));
		linkIds2paint.add(new IdImpl("3893R"));
		linkIds2paint.add(new IdImpl("3892R"));
		linkIds2paint.add(new IdImpl("3889"));
		linkIds2paint.add(new IdImpl("3891R"));
		linkIds2paint.add(new IdImpl("3891"));
		linkIds2paint.add(new IdImpl("3889R"));

		// B-344 R
		linkIds2paint.add(new IdImpl("initial_781015"));
		linkIds2paint.add(new IdImpl("3892"));
		linkIds2paint.add(new IdImpl("3893"));
		linkIds2paint.add(new IdImpl("3894"));
		linkIds2paint.add(new IdImpl("3960R"));
		linkIds2paint.add(new IdImpl("3959"));
		linkIds2paint.add(new IdImpl("3955R"));
		linkIds2paint.add(new IdImpl("3956"));
		linkIds2paint.add(new IdImpl("3974R"));
		linkIds2paint.add(new IdImpl("3972R"));
		linkIds2paint.add(new IdImpl("3964R"));
		linkIds2paint.add(new IdImpl("10289R"));

		// -------------------------------------------------------------
		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(netFilename, gk4,
				linkIds2paint);
		mn2q
				.writeShapeFile("../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/TEST_network_multimodal.shp");
	}
}
