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

package playground.yu.utils;

import java.io.IOException;

import org.matsim.gbl.Gbl;
import org.matsim.network.KmlNetworkWriter;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;

public class KMLNetWriterTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "../schweiz-ivtch/network/ivtch-changed.xml";
//		final String netFilename = "./test/yu/ivtch/input/network.xml";
//		final String netFilename = "./test/yu/equil_test/equil_net.xml";
//		final String kmzFilename = "./test/yu/ivtch/output/testEquil.kmz";
		final String kmzFilename = "./test/yu/utils/ivtch-changed_r1.1.kmz";

		Gbl.createConfig(null);

		NetworkLayerBuilder
				.setNetworkLayerType(NetworkLayerBuilder.NETWORK_SIMULATION);
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(netFilename);

		Document d = new Document(kmzFilename);
		KML k = new KML();
		k.setFeature(d);

		Folder f = new Folder("testFolder");
		f.setName("testFolderName");
		d.addFeature(f);

		KMZWriter kw = new KMZWriter(kmzFilename, KMLWriter.DEFAULT_XMLNS);

		MatsimKMLLogo mkl = null;
		try {
			mkl = new MatsimKMLLogo(kw);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		f.addFeature(mkl);

		KmlNetworkWriter nw = new KmlNetworkWriter(network,
//				new AtlantisToWGS84()
				new CH1903LV03toWGS84()
		, kw, d);

		try {
			f.addFeature(nw.getNetworkFolder());
		} catch (IOException e) {
			e.printStackTrace();
		}

		kw.writeMainKml(k);
		kw.close();
	}

}
