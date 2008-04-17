/* *********************************************************************** *
 * project: org.matsim.*
 * CreateTransimsNetwork.java
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

package playground.marcel;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.config.Config;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.NetworkWriterHandlerImplTLinks;
import org.matsim.network.NetworkWriterHandlerImplTNodes;

public class CreateTransimsNetwork {

	private String networkFileName;

	public void run(final String[] args) {
		if (args.length == 0) {
			if (!chooseFile()) {
				return; // no file selected
			}
		} else {
			this.networkFileName = args[0];
		}

		File networkFile = new File(this.networkFileName);
		if (!networkFile.exists()) {
			System.err.println("File " + this.networkFileName + " not found.");
			return;
		}
		String linksFileName = networkFile.getParent() + "/T.links";
		String nodesFileName = networkFile.getParent() + "/T.nodes";
		if (new File(linksFileName).exists()) {
			System.err.println("Output-File " + linksFileName + " exists already.");
			return;
		}
		if (new File(nodesFileName).exists()) {
			System.err.println("Output-File " + nodesFileName + " exists already.");
			return;
		}

		Config config = Gbl.createConfig(null);
		config.addCoreModules();
		config.global().setLocalDtdBase("dtd/");

		System.out.println("reading network from " + this.networkFileName);
		NetworkLayer network = null;
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(this.networkFileName);

		if (network.getNode("0") != null) {
			Logger.getLogger(CreateTransimsNetwork.class).error("The network contains a node with id 0. Transims is likely to have problems with that!");
		}
		if (network.getLink(new Id(0)) != null) {
			Logger.getLogger(CreateTransimsNetwork.class).error("The network contains a link with id 0. Transims is likely to have problems with that!");
			if (network.getLink(new Id(999999)) == null) {
				network.getLink(new Id(0)).setId(new Id("999999"));
				Logger.getLogger(CreateTransimsNetwork.class).error("Changed link 0 to link 999999.");
			}
		}

		System.out.println("writing links to " + linksFileName);
		new NetworkWriterHandlerImplTLinks(network).writeFile(linksFileName);
		System.out.println("writing nodes to " + nodesFileName);
		new NetworkWriterHandlerImplTNodes(network).writeFile(nodesFileName);
	}

	private boolean chooseFile() {
		String filename = "";

		JFrame tmpFrame = new JFrame("");

		FileDialog dialog = new FileDialog(tmpFrame, "Choose a network file", FileDialog.LOAD);
		dialog.setVisible(true);

		filename = dialog.getFile();
		tmpFrame.dispose();
		if (filename == null) {
			return false;
		}
		this.networkFileName = dialog.getDirectory() + "/" + filename;
		return true;
	}

	public static void main(final String[] args) {
		new CreateTransimsNetwork().run(args);
		System.exit(0);
	}

}
