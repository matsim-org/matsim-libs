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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * this class founds on many codes of Gregor Laemmel
 *
 * @author ychen
 *
 */
public class MATSimNet2Shape {
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
			FeatureType ft = (features.iterator().next())
					.getFeatureType();
			datastore.createSchema(ft);
			((FeatureStore) (datastore.getFeatureSource(ft.getTypeName())))
					.addFeatures(DataUtilities.reader(features));
		}
	}

	private NetworkLayer network;
	private CoordinateReferenceSystem crs = null;
	public static String ch1903 = "PROJCS[\"CH1903_LV03\",GEOGCS[\"GCS_CH1903\",DATUM[\"D_CH1903\",SPHEROID[\"Bessel_1841\",6377397.155,299.1528128]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Hotine_Oblique_Mercator_Azimuth_Center\"],PARAMETER[\"False_Easting\",600000],PARAMETER[\"False_Northing\",200000],PARAMETER[\"Scale_Factor\",1],PARAMETER[\"Azimuth\",90],PARAMETER[\"Longitude_Of_Center\",7.439583333333333],PARAMETER[\"Latitude_Of_Center\",46.95240555555556],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"21781\"]]";
	private NetworkToGraph2 n2g;

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
		this.n2g = new NetworkToGraph2(this.network, this.crs);
	}

	/**
	 * @param ShapeFilename
	 *            where the shapefile will be saved
	 */
	public void writeShapeFile(String ShapeFilename) {
		try {
			ShapeFileWriter2.writeGeometries(this.n2g.getFeatures(), ShapeFilename);
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

	public void addParameter(String paraName, Class clazz,
			Map<String, ?> parameters) {
		this.n2g.addParameter(paraName, clazz, parameters);
	}

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


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MATSimNet2Shape mn2s = new MATSimNet2Shape();
		// /////////////////////////////////////////////////////
		// write MATSim-network to Shp-file
		// /////////////////////////////////////////////////////
		// mn2s.readNetwork("test/yu/utils/ivtch-osm.1.2.xml");
		// mn2s.setCrs(ch1903);
		// mn2s.writeShapeFile("test/yu/utils/0.shp");
		// /////////////////////////////////////////////////////
		mn2s.readNetwork("../schweiz-ivtch/network/ivtch-osm.xml");
		mn2s.setCrs(ch1903);
		VolumesAnalyzer va = new VolumesAnalyzer(3600, 24 * 3600 - 1,
				mn2s.network);
		mn2s.readEvents("../runs/run439/100.events.txt.gz", va);

		// Map<String, Integer> vol7s = new HashMap<String, Integer>();
		// Map<String, Integer> vol8s = new HashMap<String, Integer>();
		// for (QueueLink ql : ((Map<IdI, QueueLink>) mn2s.network.getLinks())
		// .values()) {
		// int[] v = va.getVolumesForLink(ql.getId().toString());
		// vol7s.put(ql.getId().toString(), ((v != null) ? v[7] : 0) * 10);
		// vol8s.put(ql.getId().toString(), ((v != null) ? v[8] : 0) * 10);
		// }
		// //add new parameters or properties for links in Shp-file
		// mn2s.addParameter("volume(7-8h)", Integer.class, vol7s);
		// mn2s.addParameter("volume(8-9h)", Integer.class, vol7s);

		List<Map<String, Integer>> vols = new ArrayList<Map<String, Integer>>(
				24);
		for (int i = 0; i < 24; i++) {
			vols.add(i, null);
		}
		for (Link ql : (mn2s.network.getLinks())
				.values()) {
			String qlId = ql.getId().toString();
			int[] v = va.getVolumesForLink(qlId);
			for (int i = 0; i < 24; i++) {
				Map<String, Integer> m = vols.get(i);
				if (m != null) {
					m.put(qlId, ((v != null) ? v[i] : 0) * 10);
				} else if (m == null) {
					m = new HashMap<String, Integer>();
					m.put(qlId, ((v != null) ? v[i] : 0) * 10);
					vols.add(i, m);
				}
			}
		}
		for (int i = 0; i < 24; i++) {
			mn2s.addParameter("vol" + i + "-" + (i + 1) + "h", Integer.class,
					vols.get(i));
		}
		mn2s.writeShapeFile("test/yu/utils/test.shp");
	}
}
