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
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.Feature;
import org.geotools.feature.FeatureType;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class founds on many codes of Gregor Laemmel. man should for this "run"
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
	protected ScenarioImpl scenario = new ScenarioImpl();
	protected CoordinateReferenceSystem crs = null;
	protected X2GraphImpl n2g = null;

	public MATSimNet2QGIS(String netFilename, String coordRefSys) {
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFilename);
		crs = MGC.getCRS(coordRefSys);
		n2g = new Network2PolygonGraph(scenario.getNetwork(), crs);
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
	public NetworkLayer getNetwork() {
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
		String netFilename = "../berlin-bvg09/net/miv_small/m44_344_small_ba.xml.gz";

		MATSimNet2QGIS mn2q = new MATSimNet2QGIS(netFilename, gk4);
		mn2q.writeShapeFile("../berlin-bvg09/net/miv_small/m44_344_small_ba.shp");
	}
}
