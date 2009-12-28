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
import java.util.Date;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.KmlType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.ScreenOverlayType;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.KmlNetworkWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.transformations.GK4toWGS84;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKMLLogo;

/**
 * @author dgrether
 *
 */
public class KmlNetworkVisualizer {

	private static final Logger log = Logger.getLogger(KmlNetworkVisualizer.class);

	private NetworkLayer networkLayer;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private KmlType mainKml;

	private DocumentType mainDoc;

	private FolderType mainFolder;

	private KMZWriter writer;
	public KmlNetworkVisualizer(final String networkFile, final String outputPath) {
		Gbl.createConfig(null);
		this.networkLayer = loadNetwork(networkFile);
		this.write(outputPath);
	}

	public KmlNetworkVisualizer(final NetworkLayer network, final String outputPath) {
		this.networkLayer = network;
		this.write(outputPath);
	}

	private void write(final String filename) {
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
			KmlNetworkWriter netWriter = new KmlNetworkWriter(this.networkLayer,
					new GK4toWGS84(), this.writer, this.mainDoc);
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

	/**
	 * load the network
	 *
	 * @return the network layer
	 */
	protected NetworkLayer loadNetwork(final String networkFile) {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayer network = new NetworkLayer(); //(NetworkLayer) Gbl.getWorld().createLayer(
//				NetworkLayer.LAYER_TYPE, null);
		printNote("", "  done");

		printNote("", "  reading network xml file... ");
		new MatsimNetworkReader(network).readFile(networkFile);
		printNote("", "  done");

		return network;
	}

	/**
	 * an internal routine to generated some (nicely?) formatted output. This
	 * helps that status output looks about the same every time output is written.
	 *
	 * @param header
	 *          the header to print, e.g. a module-name or similar. If empty
	 *          <code>""</code>, no header will be printed at all
	 * @param action
	 *          the status message, will be printed together with a timestamp
	 */
	private final void printNote(final String header, final String action) {
		if (header != "") {
			System.out.println();
			System.out
					.println("===============================================================");
			System.out.println("== " + header);
			System.out
					.println("===============================================================");
		}
		if (action != "") {
			System.out.println("== " + action + " at " + (new Date()));
		}
		if (header != "") {
			System.out.println();
		}
	}

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		if (args.length != 2) {
			printHelp();
		}
		else {
//			new KmlNetworkVisualizer(args[0], args[1]);
//			new KmlNetworkVisualizer("./examples/equil/network.xml", "./output/equil.kmz");
			new KmlNetworkVisualizer("../../cvsRep/vsp-cvs/studies/berlin-wip/network/wip_net.xml", "./output/wipNet.kmz");

		}
	}

	public static void printHelp() {
		System.out
				.println("This tool has to be started with the following parameters:");
		System.out.println("  1. the name (path) of the network file");
		System.out.println("  2. the name (path) of the output kml file");

	}

}
