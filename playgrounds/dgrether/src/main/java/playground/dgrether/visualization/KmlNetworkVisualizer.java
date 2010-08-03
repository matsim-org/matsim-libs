/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkVisualizer.java
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

package playground.dgrether.visualization;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

import playground.dgrether.DgPaths;

/**
 * @author dgrether
 *
 */
public class KmlNetworkVisualizer {

	private static final Logger log = Logger.getLogger(KmlNetworkVisualizer.class);

	private Network network;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private KmlType mainKml;

	private DocumentType mainDoc;

	private FolderType mainFolder;

	private KMZWriter writer;

	public KmlNetworkVisualizer(final Network network) {
		this.network = network;
	}
	
	private void write(final String filename) {
			this.write(filename, new GK4toWGS84());
	}	

	public void write(final String filename, CoordinateTransformation transform) {
		// init kml
		this.mainKml = this.kmlObjectFactory.createKmlType();
		this.mainDoc = this.kmlObjectFactory.createDocumentType();
		this.mainKml.setAbstractFeatureGroup(this.kmlObjectFactory.createDocument(mainDoc));
		// create a folder
		this.mainFolder = this.kmlObjectFactory.createFolderType();
		this.mainFolder.setName("Matsim Data");
		this.mainDoc.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(this.mainFolder));
		// the writer
		this.writer = new KMZWriter(filename);
		try {
			// add the matsim logo to the kml
			ScreenOverlayType logo = MatsimKMLLogo.writeMatsimKMLLogo(writer);
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createScreenOverlay(logo));
			KmlNetworkWriter netWriter = new KmlNetworkWriter(this.network,
					transform, this.writer, this.mainDoc);
			FolderType networkFolder = netWriter.getNetworkFolder();
			this.mainFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder(networkFolder));
		} catch (IOException e) {
			Gbl.errorMsg("Cannot create kmz or logo cause: " + e.getMessage());
			e.printStackTrace();
		}
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
		log.info("Network written to kmz!");
	}

	protected static Network loadNetwork(final String networkFile) {
		ScenarioImpl scenario = new ScenarioImpl();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		return scenario.getNetwork();
	}


	public static void main(final String[] args) {
		if (false && args.length != 2) {
			printHelp();
		}
		else {
			//			Network net = loadNetwork("../../cvsRep/vsp-cvs/studies/berlin-wip/network/wip_net.xml");
			Network net = loadNetwork(DgPaths.BERLIN_NET);
//			new KmlNetworkVisualizer("./examples/equil/network.xml", "./output/equil.kmz");
			new KmlNetworkVisualizer(net).write(DgPaths.BERLIN_SCENARIO + "net/miv_big/m44_344_big.kmz");

		}
	}

	public static void printHelp() {
		System.out
				.println("This tool has to be started with the following parameters:");
		System.out.println("  1. the name (path) of the network file");
		System.out.println("  2. the name (path) of the output kml file");

	}

}
