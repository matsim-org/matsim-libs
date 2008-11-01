/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;

import org.matsim.gbl.MatsimResource;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.transformations.AtlantisToWGS84;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.MatsimKmlStyleFactory;

/**
 * @author mrieser
 */
public class KmlNetworkWriterTest extends MatsimTestCase {

	public void testWrite() throws IOException {
		final String kmzFilename = getOutputDirectory() + "network.kmz";
		
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile("test/scenarios/equil/network.xml");
		
		ObjectFactory kmlObjectFactory = new ObjectFactory();
		KMZWriter kmzWriter = new KMZWriter(kmzFilename);
		
		KmlType mainKml = kmlObjectFactory.createKmlType();
		DocumentType mainDoc = kmlObjectFactory.createDocumentType();
		mainKml.setAbstractFeatureGroup(kmlObjectFactory.createDocument(mainDoc));
		
		KmlNetworkWriter kmlNetworkWriter = new KmlNetworkWriter(network, new AtlantisToWGS84(), kmzWriter, mainDoc);
		
		mainDoc.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(kmlNetworkWriter.getNetworkFolder()));
		
		kmzWriter.writeMainKml(mainKml);
		kmzWriter.close();
		
		// now do the tests
		File kmzFile = new File(kmzFilename);
		assertTrue("kmz-file does not exist.", kmzFile.exists());
		
		ZipFile zipFile = new ZipFile(kmzFile);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		ArrayList<ZipEntry> allEntries = new ArrayList<ZipEntry>(5);
		while (entries.hasMoreElements()) {
			allEntries.add(entries.nextElement());
		}
		assertEquals("there should be 4 entries in the zip-file.", 4, allEntries.size());
		ZipEntry docKmlEntry = null;
		ZipEntry linkIconEntry = null;
		ZipEntry nodeIconEntry = null;
		ZipEntry mainKmlEntry = null;
		for (ZipEntry entry : allEntries) {
			String name = entry.getName();
			if ("doc.kml".equals(name)) {
				docKmlEntry = entry;
			} else if (MatsimKmlStyleFactory.DEFAULTLINKICON.equals(name)) {
				linkIconEntry = entry;
			} else if (MatsimKmlStyleFactory.DEFAULTNODEICON.equals(name)) {
				nodeIconEntry = entry;
			} else if ("main.kml".equals(name)) {
				mainKmlEntry = entry;
			} else {
				fail("unrecognized entry name: " + name);
			}
		}

		long iconFileSize = new File(MatsimResource.getAsURL(MatsimKmlStyleFactory.DEFAULTNODEICONRESOURCE).getFile()).length();
		assertEquals("uncompressed size of icon is wrong.", linkIconEntry.getSize(), iconFileSize);
		assertEquals("uncompressed size of icon is wrong.", nodeIconEntry.getSize(), iconFileSize);
		assertEquals("uncompressed size of doc.kml is wrong.", 303, docKmlEntry.getSize());
		assertEquals("uncompressed size of main.kml is wrong.", 45428, mainKmlEntry.getSize());
	}
	
}
