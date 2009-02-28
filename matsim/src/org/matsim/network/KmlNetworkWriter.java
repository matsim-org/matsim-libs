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

import net.opengis.kml._2.AbstractFeatureType;
import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.FolderType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.PlacemarkType;
import net.opengis.kml._2.StyleType;

import org.apache.log4j.Logger;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Network;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.utils.geometry.CoordinateTransformation;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.MatsimKmlStyleFactory;
import org.matsim.utils.vis.kml.NetworkFeatureFactory;

/**
 * @author dgrether
 */
public class KmlNetworkWriter {

	private static final Logger log = Logger.getLogger(KmlNetworkWriter.class);

	private Network network;

	private MatsimKmlStyleFactory styleFactory;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();
	
	private StyleType networkLinkStyle;

	private NetworkFeatureFactory networkFeatureFactory;

	private StyleType networkNodeStyle;

	public KmlNetworkWriter(final Network network, final CoordinateTransformation coordTransform, KMZWriter writer, DocumentType doc) {
		this.network = network;
		this.styleFactory = new MatsimKmlStyleFactory(writer, doc);
		this.networkFeatureFactory = new NetworkFeatureFactory(coordTransform);
	}

	public FolderType getNetworkFolder() throws IOException {
		
		FolderType folder = this.kmlObjectFactory.createFolderType();
		
		folder.setName("MATSIM Network: " + this.network.getName());
		this.networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
		this.networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();
		
		FolderType nodeFolder = kmlObjectFactory.createFolderType();
		nodeFolder.setName("Nodes");
//		linkFolder.addStyle(this.networkNodeStyle);
		for (Node n : this.network.getNodes().values()) {
			
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createNodeFeature(n, this.networkNodeStyle);
			if (abstractFeature.getClass().equals(PlacemarkType.class)) {
				nodeFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
			} else if (abstractFeature.getClass().equals(FolderType.class)) {
				nodeFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
			} else {
				log.warn("Not yet implemented: Adding node KML features of type " + abstractFeature.getClass());
			}

		}
		folder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(nodeFolder));

		FolderType linkFolder = kmlObjectFactory.createFolderType();
		linkFolder.setName("Links");
//		linkFolder.addStyle(this.networkLinkStyle);
		for (Link l : this.network.getLinks().values()) {
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createLinkFeature(l, this.networkLinkStyle);
			if (abstractFeature.getClass().equals(PlacemarkType.class)) {
				linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createPlacemark((PlacemarkType) abstractFeature));
			} else if (abstractFeature.getClass().equals(FolderType.class)) {
				linkFolder.getAbstractFeatureGroup().add(this.kmlObjectFactory.createFolder((FolderType) abstractFeature));
			} else {
				log.warn("Not yet implemented: Adding node KML features of type " + abstractFeature.getClass());
			}
		}
		folder.getAbstractFeatureGroup().add(kmlObjectFactory.createFolder(linkFolder));
		
		return folder;
		
	}

}
