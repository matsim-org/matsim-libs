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

import java.util.Set;

import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.LineString;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.matsimkml.MatsimKmlStyleFactory;


/**
 * @author dgrether
 *
 */
public class KmlNetworkWriter {

	private CoordinateTransformationI coordTransform = null;
	
	private NetworkLayer network;

	private MatsimKmlStyleFactory factory;
	
	private Style networkStyle;
	
	public KmlNetworkWriter(final NetworkLayer network, final CoordinateTransformationI coordTransform) {
		this.network = network;
		this.coordTransform = coordTransform;
		this.factory = new MatsimKmlStyleFactory();
	}
	
	
	public Folder getNetworkFolder() {
		Folder folder = new Folder(this.network.getName());
		folder.setName("MATSIM Network: " + this.network.getName());
		this.networkStyle = this.factory.createDefaultNetworkStyle();
		folder.addStyle(this.networkStyle);
		Folder nodeFolder = new Folder(this.network.getName() + "nodes");
		nodeFolder.setName("Nodes");
		folder.addFeature(nodeFolder);
		Folder linkFolder = new Folder(this.network.getName() + "links");
		linkFolder.setName("Links");
		folder.addFeature(linkFolder);
		
		Set<Node> networkNodes = this.network.getNodes();
		for (Node n : networkNodes) {
			nodeFolder.addFeature(createNodeFeature(n));
		}
		Set<Link> networkLinks = this.network.getLinks();
		for (Link l : networkLinks) {
			linkFolder.addFeature(createLinkFeature(l));
		}
		
		return folder;
	}


	private Feature createLinkFeature(final Link l) {
		Placemark p = new Placemark(l.getId().toString());
		p.setName(l.getId().toString());
		CoordI fromCoord = this.coordTransform.transform(l.getFromNode().getCoord());
		CoordI toCoord = this.coordTransform.transform(l.getToNode().getCoord());
		LineString line = new LineString(new Point(fromCoord.getX(), fromCoord.getY(), 0.0), new Point(toCoord.getX(), toCoord.getY(), 0.0));
		p.setGeometry(line);
		p.setStyleUrl(this.networkStyle.getStyleUrl());
		return p;
	}


	private Feature createNodeFeature(final Node n) {
		Placemark p = new Placemark(n.getId().toString());
		p.setName(n.getId().toString());
		CoordI coord = this.coordTransform.transform(n.getCoord());
		Point point = new Point(coord.getX(), coord.getY(), 0.0);
		p.setGeometry(point);
		p.setStyleUrl(this.networkStyle.getStyleUrl());
		return p;
	}
	
}
