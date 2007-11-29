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

package org.matsim.utils.vis.matsimkml;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.Node;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.CoordinateTransformationI;
import org.matsim.utils.vis.kml.Feature;
import org.matsim.utils.vis.kml.Folder;
import org.matsim.utils.vis.kml.LineString;
import org.matsim.utils.vis.kml.Placemark;
import org.matsim.utils.vis.kml.Point;
import org.matsim.utils.vis.kml.Style;

/**
 * @author dgrether
 *
 */
public class NetworkFeatureFactory {

	private static final Logger log = Logger.getLogger(NetworkFeatureFactory.class);

	private CoordinateTransformationI coordTransform;
	/**
	 * constant for the link description
	 */
	public static final String ENDP = "</p>";
	/**
	 * constant for the link description
	 */
	public static final String STARTP = "<p>";
	/**
	 * constant for the link description
	 */
	public static final String ENDH3 = "</h3>";
	/**
	 * constant for the link description
	 */
	public static final String STARTH3 = "<h3>";
	/**
	 * constant for the link description
	 */
	public static final String ENDH2 = "</h2>";
	/**
	 * constant for the link description
	 */
	public static final String ENDCDATA = "]]>";
	/**
	 * constant for the link description
	 */
	public static final String STARTH2 = "<h2>";
	/**
	 * constant for the link description
	 */
	public static final String STARTCDATA = "<![CDATA[";
	/**
	 * constant for the link description
	 */
	public static final String STARTUL = "<ul>";
	/**
	 * constant for the link description
	 */
	public static final String ENDUL = "</ul>";
	/**
	 * constant for the link description
	 */
	public static final String STARTLI = "<li>";
	/**
	 * constant for the link description
	 */
	public static final String ENDLI = "</li>";

	public NetworkFeatureFactory(CoordinateTransformationI coordTransform) {
		this.coordTransform = coordTransform;
	}



	public Feature createLinkFeature(final Link l, Style networkStyle) {
		String description = createLinkDescription(l);
		Folder folder = new Folder(l.getId().toString());
		folder.setName(l.getId().toString());
		Placemark p = new Placemark("link" + l.getId().toString());
		p.setName(l.getId().toString());
		CoordI fromCoord = this.coordTransform.transform(l.getFromNode().getCoord());
		CoordI toCoord = this.coordTransform.transform(l.getToNode().getCoord());
		LineString line = new LineString(new Point(fromCoord.getX(), fromCoord.getY(), 0.0), new Point(toCoord.getX(), toCoord.getY(), 0.0));
		p.setGeometry(line);
		p.setStyleUrl(networkStyle.getStyleUrl());
		p.setDescription(description);

		Placemark pointPlacemark = new Placemark("linkCenter" + l.getId());
		CoordI centerCoord = this.coordTransform.transform(l.getCenter());
		Point point = new Point(centerCoord.getX(), centerCoord.getY(), 0.0);
		pointPlacemark.setGeometry(point);
		pointPlacemark.setStyleUrl(networkStyle.getStyleUrl());
		pointPlacemark.setDescription(description);
//		return pointPlacemark;
		folder.addFeature(pointPlacemark);
		folder.addFeature(p);
		return folder;
	}


	public Feature createNodeFeature(final Node n, Style networkStyle) {
		Placemark p = new Placemark("node" + n.getId().toString());
		p.setName(n.getId().toString());
		CoordI coord = this.coordTransform.transform(n.getCoord());
		Point point = new Point(coord.getX(), coord.getY(), 0.0);
		p.setGeometry(point);
		p.setStyleUrl(networkStyle.getStyleUrl());
		p.setDescription(createNodeDescription(n));
		return p;
	}

	private String createLinkDescription(Link l) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NetworkFeatureFactory.STARTCDATA);
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append("Link: " );
		buffer.append(l.getId());
		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append("From Node: "+ l.getFromNode().getId());
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append("To Node: " + l.getToNode().getId());
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(NetworkFeatureFactory.ENDP);

		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append("Attributes: ");
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(STARTUL);
		buffer.append(STARTLI);
		buffer.append("Freespeed: ");
		buffer.append(l.getFreespeed());
		buffer.append(ENDLI);
		buffer.append(STARTLI);
		buffer.append("Capacity: ");
		buffer.append(l.getCapacity());
		buffer.append(ENDLI);
		buffer.append(STARTLI);
		buffer.append("Number of Lanes: ");
		buffer.append(l.getLanes());
		buffer.append(ENDLI);
		buffer.append(STARTLI);
		buffer.append("Length: ");
		buffer.append(l.getLength());
		buffer.append(ENDLI);
		buffer.append(ENDUL);
		buffer.append(NetworkFeatureFactory.ENDP);

		buffer.append(NetworkFeatureFactory.ENDCDATA);

		return buffer.toString();

	}


	private String createNodeDescription(Node n) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(NetworkFeatureFactory.STARTCDATA);
		buffer.append(NetworkFeatureFactory.STARTH2);
		buffer.append("Node: " );
		buffer.append(n.getId());
		buffer.append(NetworkFeatureFactory.ENDH2);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append("Inlinks");
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(STARTUL);
		for (Link l : n.getInLinks().values()) {
			buffer.append(STARTLI);
			buffer.append("Link: " );
			buffer.append(l.getId());
			buffer.append(" from Node: " );
			buffer.append(l.getFromNode().getId());
			buffer.append(ENDLI);
		}
		buffer.append(ENDUL);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.STARTH3);
		buffer.append("Outlinks");
		buffer.append(NetworkFeatureFactory.ENDH3);
		buffer.append(NetworkFeatureFactory.STARTP);
		buffer.append(STARTUL);
		for (Link l : n.getOutLinks().values()) {
			buffer.append(STARTLI);
			buffer.append("Link: " );
			buffer.append(l.getId());
			buffer.append(" to Node: ");
			buffer.append(l.getToNode().getId());
			buffer.append(ENDLI);
		}
		buffer.append(ENDUL);
		buffer.append(NetworkFeatureFactory.ENDP);
		buffer.append(NetworkFeatureFactory.ENDCDATA);
		return buffer.toString();

	}
}
