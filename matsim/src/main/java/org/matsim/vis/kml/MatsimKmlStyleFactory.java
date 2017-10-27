/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimKmlStyleFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.vis.kml;

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.matsim.core.gbl.MatsimResource;

/**
 * @author dgrether
 */
public class MatsimKmlStyleFactory implements NetworkKmlStyleFactory {
	public static final String DEFAULTLINKICON ="link.png";

	public static final String DEFAULTNODEICON ="node.png";
	/**
	 * the resource to be used as icon
	 */
	public static final String DEFAULTNODEICONRESOURCE = "icon18.png";

	private static final Double ICONSCALE = Double.valueOf(0.5);

	public static final byte[] MATSIMRED = new byte[]{(byte) 255, (byte) 15, (byte) 15, (byte) 190};
//	public static final Color MATSIMBLUE = new Color(190, 190, 80, 90);
	private static final byte[] MATSIMGREY = new byte[]{(byte) 210, (byte) 70, (byte) 50, (byte) 50};
	protected static final byte[] MATSIMWHITE = new byte[]{(byte) 230, (byte) 230, (byte) 230, (byte) 230};
	/**
	 * the kmz writer
	 */
	private KMZWriter writer = null;

	private ObjectFactory kmlObjectFactory = new ObjectFactory();

	private DocumentType document;

	private StyleType defaultnetworknodestyle;

	private StyleType defaultnetworklinkstyle;

	public MatsimKmlStyleFactory(KMZWriter writer, DocumentType document) {
		this.writer = writer;
		this.document = document;
	}
	
	@Override
	public StyleType createDefaultNetworkNodeStyle() throws IOException {
		if (this.defaultnetworknodestyle == null) {
			this.defaultnetworknodestyle = kmlObjectFactory.createStyleType();
			this.defaultnetworknodestyle.setId("defaultnetworknodestyle");

			LinkType iconLink = kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTNODEICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTNODEICON);
			IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMRED);
			iStyle.setScale(MatsimKmlStyleFactory.ICONSCALE);
			this.defaultnetworknodestyle.setIconStyle(iStyle);
			this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.defaultnetworknodestyle));
		}
		return this.defaultnetworknodestyle;
	}

	@Override
	public StyleType createDefaultNetworkLinkStyle() throws IOException {
		if (this.defaultnetworklinkstyle == null) {
			this.defaultnetworklinkstyle = kmlObjectFactory.createStyleType();
			this.defaultnetworklinkstyle.setId("defaultnetworklinkstyle");

			LinkType iconLink = kmlObjectFactory.createLinkType();
			iconLink.setHref(DEFAULTLINKICON);
			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyleType iStyle = kmlObjectFactory.createIconStyleType();
			iStyle.setIcon(iconLink);
			iStyle.setColor(MatsimKmlStyleFactory.MATSIMWHITE);
			iStyle.setScale(MatsimKmlStyleFactory.ICONSCALE);
			this.defaultnetworklinkstyle.setIconStyle(iStyle);
			LineStyleType lineStyle = kmlObjectFactory.createLineStyleType();
			lineStyle.setColor(MatsimKmlStyleFactory.MATSIMGREY);
			lineStyle.setWidth(Double.valueOf(12.0));
			this.defaultnetworklinkstyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.defaultnetworklinkstyle));
		}
		return this.defaultnetworklinkstyle;
	}

}
