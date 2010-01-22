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

package playground.mrieser;

import java.awt.FileDialog;
import java.io.File;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriterHandlerImplTLinks;
import org.matsim.core.network.NetworkWriterHandlerImplTNodes;

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

		ScenarioImpl scenario = new ScenarioImpl();
		Config config = scenario.getConfig();
		config.global().setLocalDtdBase("dtd/");

		System.out.println("reading network from " + this.networkFileName);
		NetworkLayer network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(this.networkFileName);

		if (network.getNodes().get(new IdImpl("0")) != null) {
			Logger.getLogger(CreateTransimsNetwork.class).error("The network contains a node with id 0. Transims is likely to have problems with that!");
		}
		if (network.getLinks().get(new IdImpl(0)) != null) {
			Logger.getLogger(CreateTransimsNetwork.class).error("The network contains a link with id 0. Transims is likely to have problems with that!");
			if (network.getLinks().get(new IdImpl(999999)) == null) {
				network.getLinks().get(new IdImpl(0)).setId(new IdImpl("999999"));
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
