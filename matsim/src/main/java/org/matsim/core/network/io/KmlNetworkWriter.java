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

package org.matsim.core.network.io;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.vis.kml.KMZWriter;
import org.matsim.vis.kml.MatsimKmlStyleFactory;
import org.matsim.vis.kml.NetworkFeatureFactory;
import org.matsim.vis.kml.NetworkKmlStyleFactory;

import net.opengis.kml.v_2_2_0.AbstractFeatureType;
import net.opengis.kml.v_2_2_0.DocumentType;
import net.opengis.kml.v_2_2_0.FolderType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import net.opengis.kml.v_2_2_0.StyleType;

/**
 * @author dgrether
 */
public final class KmlNetworkWriter implements MatsimSomeWriter {
	// yyyyyy move network io into separate package

	private static final Logger log = Logger.getLogger(KmlNetworkWriter.class);

	private final Network network;

	private NetworkKmlStyleFactory styleFactory;

	private final ObjectFactory kmlObjectFactory = new ObjectFactory();

    private final NetworkFeatureFactory networkFeatureFactory;

    public KmlNetworkWriter(final Network network, final CoordinateTransformation coordTransform, KMZWriter writer, DocumentType doc) {
		this.network = network;
		this.styleFactory = new MatsimKmlStyleFactory(writer, doc);
		this.networkFeatureFactory = new NetworkFeatureFactory(coordTransform, network);
	}
	
	public void setNetworkKmlStyleFactory(NetworkKmlStyleFactory styleFac){
		this.styleFactory = styleFac;
	}

	public FolderType getNetworkFolder() throws IOException {
		
		FolderType folder = this.kmlObjectFactory.createFolderType();
		
		folder.setName("MATSIM Network");
        StyleType networkLinkStyle = this.styleFactory.createDefaultNetworkLinkStyle();
        StyleType networkNodeStyle = this.styleFactory.createDefaultNetworkNodeStyle();
		
		FolderType nodeFolder = kmlObjectFactory.createFolderType();
		nodeFolder.setName("Nodes");
//		linkFolder.addStyle(this.networkNodeStyle);
		for (Node n : this.network.getNodes().values()) {
			
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createNodeFeature(n, networkNodeStyle);
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
			AbstractFeatureType abstractFeature = this.networkFeatureFactory.createLinkFeature(l, networkLinkStyle);
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
