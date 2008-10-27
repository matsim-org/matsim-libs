/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimKmlStyleFactory.java
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

import java.io.IOException;

import net.opengis.kml._2.DocumentType;
import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LineStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.matsim.gbl.MatsimResource;
import org.matsim.utils.vis.kml.KMZWriter;


/**
 * @author dgrether
 *
 */
public class MatsimKmlStyleFactory {
	public static final String DEFAULTLINKICON ="icon181.png";

	public static final String DEFAULTNODEICON ="icon18.png";
	/**
	 * the resource to be used as icon
	 */
	public static final String DEFAULTNODEICONRESOURCE = "icon18.png";
	/**
	 * the scale for the icons
	 */
	private static final double ICONSCALE = 0.5;
	/**
	 * some colors frequently used in matsim: bgr: 15,15,190
	 */
	public static final byte[] MATSIMRED = new byte[]{(byte) 0xFF, (byte) 0x0F, (byte) 0x0F, (byte) 0xBE};
	/**
	 * some colors frequently used in matsim
	 */
//	public static final Color MATSIMBLUE = new Color(190, 10, 80, 190);
	/**
	 * some colors frequently used in matsim
	 */
	public static final byte[] MATSIMGREY = new byte[]{(byte) 210, (byte) 50, (byte) 50, (byte) 70};
	/**
	 * some colors frequently used in matsim
	 */
	public static final byte[] MATSIMWHITE = new byte[]{(byte) 230, (byte) 230, (byte) 230, (byte) 230};
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
//			LineStyle lineStyle = new LineStyle(MATSIMGREY, ColorStyle.DEFAULT_COLOR_MODE, 12);
//			style.setLineStyle(lineStyle);
		}
		return this.defaultnetworknodestyle;
	}

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
			lineStyle.setWidth(12.0);
			this.defaultnetworklinkstyle.setLineStyle(lineStyle);
			this.document.getAbstractStyleSelectorGroup().add(kmlObjectFactory.createStyle(this.defaultnetworklinkstyle));
		}
		return this.defaultnetworklinkstyle;
	}

}
