/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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

package org.matsim.network;

import java.io.IOException;

import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.matsimkml.MatsimKmlStyleFactory;
import org.matsim.utils.vis.matsimkml.NetworkFeatureFactory;

/**
 * @author dgrether
 */
public class KmlNetworkWriter {

	private NetworkLayer network;

	private MatsimKmlStyleFactory styleFactory;

	private Style networkLinkStyle;

	private NetworkFeatureFactory networkFeatureFactory;

	private Style networkNodeStyle;

	public KmlNetworkWriter(final NetworkLayer network, final CoordinateTransformationI coordTransform, KMZWriter writer, Document doc) {
		this.network = network;
		this.styleFactory = new MatsimKmlStyleFactory(writer, doc);
		this.networkFeatureFactory = new NetworkFeatureFactory(coordTransform);
	}


	public Folder getNetworkFolder() throws IOException {
		Folder folder = new Folder(this.network.getName());
		folder.setName("MATSIM Network: " + this.network.getName());
		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();
		Folder nodeFolder = new Folder(this.network.getName() + "nodes");
		nodeFolder.setName("Nodes");
//		nodeFolder.addStyle(this.networkNodeStyle);
		folder.addFeature(nodeFolder);
		Folder linkFolder = new Folder(this.network.getName() + "links");
		linkFolder.setName("Links");
//		linkFolder.addStyle(this.networkLinkStyle);
		folder.addFeature(linkFolder);

		for (Node n : this.network.getNodes().values()) {
			nodeFolder.addFeature(this.networkFeatureFactory.createNodeFeature(n, this.networkNodeStyle));
		}
		for (Link l : this.network.getLinks().values()) {
			linkFolder.addFeature(this.networkFeatureFactory.createLinkFeature(l, this.networkLinkStyle));
		}

		return folder;
	}






}
