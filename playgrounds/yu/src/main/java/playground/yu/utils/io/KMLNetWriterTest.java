/* *********************************************************************** *
 * project: org.matsim.*
 * KMLNetWriterTest.java
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

package playground.yu.utils.io;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

public class KMLNetWriterTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch-changed.xml";
		// final String netFilename = "./test/yu/ivtch/input/network.xml";
		// final String netFilename = "./test/yu/equil_test/equil_net.xml";
		// final String kmzFilename = "./test/yu/ivtch/output/testEquil.kmz";
		final String kmzFilename = "./test/yu/ivtch/output/testZrh.kmz";

		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(netFilename);

		ObjectFactory kmlObjectFactory = new ObjectFactory();

		DocumentType d = kmlObjectFactory.createDocumentType();
		d.setId(kmzFilename);
		KmlType k = kmlObjectFactory.createKmlType();
		k.setAbstractFeatureGroup(kmlObjectFactory.createDocument(d));

		FolderType f = kmlObjectFactory.createFolderType();
		f.setId("testFolder");
		f.setName("testFolderName");
		d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(f));

		KMZWriter kw = new KMZWriter(kmzFilename);

		ScreenOverlayType mkl = null;
		try {
			mkl = MatsimKMLLogo.writeMatsimKMLLogo(kw);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		f.getAbstractFeatureGroup().add(
				kmlObjectFactory.createScreenOverlay(mkl));

		KmlNetworkWriter nw = new KmlNetworkWriter(network,
		// new AtlantisToWGS84()
				new CH1903LV03toWGS84(), kw, d);

		try {
			f.getAbstractFeatureGroup().add(
					kmlObjectFactory.createFolder(nw.getNetworkFolder()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		kw.writeMainKml(k);
		kw.close();
	}

}
