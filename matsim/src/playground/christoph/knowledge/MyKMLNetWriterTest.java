/* *********************************************************************** *
 * project: org.matsim.*
 * MyKMLNetWriterTest.java
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

package playground.christoph.knowledge;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;

import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimResource;
import org.matsim.network.KmlNetworkWriter;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.transformations.CH1903LV03toWGS84;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.MatsimKMLLogo;
import org.matsim.utils.vis.kml.MatsimKmlStyleFactory;

public class MyKMLNetWriterTest {

	String netFileName;
	String kmzFileName;
	NetworkLayer network;
	
	public void setNetwork(NetworkLayer net)
	{
		network = net;
	}
	
	public void loadNetwork(String netFileName)
	{
		network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(netFileName);
	}
	
	public void setKmzFileName(String name)
	{
		kmzFileName = name;
	}

	public void createKmzFile()
	{		
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		
		DocumentType d = kmlObjectFactory.createDocumentType();
		d.setId(kmzFileName);
		KmlType k = kmlObjectFactory.createKmlType();

		k.setAbstractFeatureGroup(kmlObjectFactory.createDocument(d));

		FolderType f = kmlObjectFactory.createFolderType();
		f.setId("testFolder");
		f.setName("testFolderName");
		d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(f));

		FolderType mf = kmlObjectFactory.createFolderType();
		mf.setId("networklinksfolder");
		mf.setName("mainFolder");
		d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(mf));
		
		KMZWriter kw = new KMZWriter(kmzFileName);

		ScreenOverlayType mkl = null;
		try {
			mkl = MatsimKMLLogo.writeMatsimKMLLogo(kw);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		f.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(mkl));

		// add network
		KmlNetworkWriter nw = new KmlNetworkWriter(network, new CH1903LV03toWGS84(), kw, d);
		try {
			d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(nw.getNetworkFolder()));
		} catch (IOException e) {
			e.printStackTrace();
		}

		kw.writeMainKml(k);
		kw.close();		
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final String netFilename = "D:/Master_Thesis_HLI/Workspace/TestNetz/network.xml";
		final String kmzFilename = "D:/Master_Thesis_HLI/Workspace/TestNetz/test.kmz";

		Gbl.createConfig(null);

		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(netFilename);

		ObjectFactory kmlObjectFactory = new ObjectFactory();
		
		DocumentType d = kmlObjectFactory.createDocumentType();
		d.setId(kmzFilename);
		KmlType k = kmlObjectFactory.createKmlType();

		k.setAbstractFeatureGroup(kmlObjectFactory.createDocument(d));

		FolderType f = kmlObjectFactory.createFolderType();
		f.setId("testFolder");
		f.setName("testFolderName");
		d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(f));

		FolderType mf = kmlObjectFactory.createFolderType();
		mf.setId("networklinksfolder");
		mf.setName("mainFolder");
		d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(mf));
		
		KMZWriter kw = new KMZWriter(kmzFilename);

		ScreenOverlayType mkl = null;
		try {
			mkl = MatsimKMLLogo.writeMatsimKMLLogo(kw);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		f.getAbstractFeatureGroup().add(kmlObjectFactory.createScreenOverlay(mkl));

		// add network
		KmlNetworkWriter nw = new KmlNetworkWriter(network, new CH1903LV03toWGS84(), kw, d);

		try {
			d.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(nw.getNetworkFolder()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		kw.writeMainKml(k);
		kw.close();
	}

}

