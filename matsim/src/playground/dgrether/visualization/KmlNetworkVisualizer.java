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

import org.matsim.gbl.Gbl;
import org.matsim.network.KmlNetworkWriter;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkLayerBuilder;
import org.matsim.utils.geometry.transformations.GK4toWGS84;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KML;
import org.matsim.utils.vis.kml.KMLWriter;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.matsimkml.MatsimKMLLogo;

/**
 * @author dgrether
 * 
 */
public class KmlNetworkVisualizer {

	private NetworkLayer networkLayer;

	private KML mainKml;

	private Document mainDoc;

	private Folder mainFolder;
	
	private KMZWriter writer;
	public KmlNetworkVisualizer(final String networkFile, final String outputPath) {
		Gbl.createConfig(null);
		this.networkLayer = loadNetwork(networkFile);
		this.write(outputPath);
		
	}

	private void write(final String filename) {
		// init kml
		this.mainKml = new KML();
		this.mainDoc = new Document(filename);
		this.mainKml.setFeature(this.mainDoc);
		// create a folder
		this.mainFolder = new Folder("2dnetworklinksfolder");
		this.mainFolder.setName("Matsim Data");
		this.mainDoc.addFeature(this.mainFolder);
		// the writer
		this.writer = new KMZWriter(filename, KMLWriter.DEFAULT_XMLNS);
		try {
			// add the matsim logo to the kml
			MatsimKMLLogo logo = new MatsimKMLLogo(this.writer);
			this.mainFolder.addFeature(logo);
		} catch (IOException e) {
			Gbl.errorMsg("Cannot create legend or logo cause: " + e.getMessage());
			e.printStackTrace();
		}
		KmlNetworkWriter netWriter = new KmlNetworkWriter(this.networkLayer,
				new GK4toWGS84());
		Folder networkFolder = netWriter.getNetworkFolder();
		this.mainFolder.addFeature(networkFolder);
		this.writer.writeMainKml(this.mainKml);
		this.writer.close();
	}

	/**
	 * load the network
	 * 
	 * @return the network layer
	 */
	protected NetworkLayer loadNetwork(final String networkFile) {
		// - read network: which buildertype??
		printNote("", "  creating network layer... ");
		NetworkLayerBuilder
				.setNetworkLayerType(NetworkLayerBuilder.NETWORK_SIMULATION);
		NetworkLayer network = (NetworkLayer) Gbl.getWorld().createLayer(
				NetworkLayer.LAYER_TYPE, null);
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
			new KmlNetworkVisualizer(args[0], args[1]);
		}
	}

	public static void printHelp() {
		System.out
				.println("This tool has to be started with the following parameters:");
		System.out.println("  1. the name (path) of the network file");
		System.out.println("  2. the name (path) of the output kml file");

	}

}
