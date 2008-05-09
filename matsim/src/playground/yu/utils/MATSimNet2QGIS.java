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
package playground.yu.utils;

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
import org.geotools.referencing.CRS;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * This class founds on many codes of Gregor Laemmel. man should for this "run"
 * install com.sun.media.jai and javax.media.jai from http://jai.dev.java.net
 * 
 * @author ychen
 * 
 */
public class MATSimNet2QGIS {
	/**
	 * this class is only a copy of
	 * <class>playground.gregor.shapeFileToMATSim.ShapeFileWriter</class>
	 * Gregor Laemmel's
	 * 
	 * @author ychen
	 * 
	 */
	public static class ShapeFileWriter2 {
		public static void writeGeometries(Collection<Feature> features,
				String filename) throws IOException, FactoryException,
				SchemaException {
			ShapefileDataStore datastore = new ShapefileDataStore((new File(
					filename)).toURI().toURL());
			FeatureType ft = (features.iterator().next()).getFeatureType();
			datastore.createSchema(ft);
			((FeatureStore) (datastore.getFeatureSource(ft.getTypeName())))
					.addFeatures(DataUtilities.reader(features));
		}
	}

	private NetworkLayer network;
	private CoordinateReferenceSystem crs = null;
	public static String ch1903 = "PROJCS[\"CH1903_LV03\",GEOGCS[\"GCS_CH1903\",DATUM[\"D_CH1903\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"False_Easting\",600000],PARAMETER[\"False_Northing\",200000],PARAMETER[\"Scale_Factor\",1],PARAMETER[\"Azimuth\",90],PARAMETER[\"Longitude_Of_Center\",7.439583333333333],PARAMETER[\"Latitude_Of_Center\",46.95240555555556],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"21781\"]]";
	private Network2PolygonGraph n2g;

	public void readNetwork(String netFilename) {
		Config config = Gbl.createConfig(null);
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(netFilename);
	}

	/**
	 * @param crs
	 *            the crs to set
	 */
	public void setCrs(String wkt) {
		try {
			this.crs = CRS.parseWKT(wkt);
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		this.n2g = new Network2PolygonGraph(this.network, this.crs);
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	public void writeShapeFile(String ShapeFilename) {
		try {
			ShapeFileWriter2.writeGeometries(this.n2g.getFeatures(),
					ShapeFilename);
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
	public void addParameter(String paraName, Class<?> clazz,
			Map<Id, ?> parameters) {
		this.n2g.addParameter(paraName, clazz, parameters);
	}

	// /////////////////////////////
	/**
	 * @return the network
	 */
	public NetworkLayer getNetwork() {
		return this.network;
	}

	public void readEvents(String eventsFilename, EventHandlerI handler) {
		Events events = new Events();
		events.addHandler(handler);
		new MatsimEventsReader(events).readFile(eventsFilename);
	}
}
