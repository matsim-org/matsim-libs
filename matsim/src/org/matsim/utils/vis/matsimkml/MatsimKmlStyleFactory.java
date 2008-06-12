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

import org.matsim.gbl.MatsimResource;
import org.matsim.utils.vis.kml.ColorStyle;
import org.matsim.utils.vis.kml.Document;
import org.matsim.utils.vis.kml.Icon;
import org.matsim.utils.vis.kml.IconStyle;
import org.matsim.utils.vis.kml.KMZWriter;
import org.matsim.utils.vis.kml.LineStyle;
import org.matsim.utils.vis.kml.Style;
import org.matsim.utils.vis.kml.fields.Color;


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
	public static final Color MATSIMRED = new Color("ff","0f","0f","be");
	/**
	 * some colors frequently used in matsim
	 */
	public static final Color MATSIMBLUE = new Color(190, 10, 80, 190);
	/**
	 * some colors frequently used in matsim
	 */
	public static final Color MATSIMGREY = new Color(210, 50, 50, 70);
	/**
	 * some colors frequently used in matsim
	 */
	public static final Color MATSIMWHITE = new Color(230, 230, 230, 230);
	/**
	 * the kmz writer
	 */
	private KMZWriter writer = null;

	private Document document;

	private Style defaultnetworknodestyle;

	private Style defaultnetworklinkstyle;

	public MatsimKmlStyleFactory(KMZWriter writer, Document document) {
		this.writer = writer;
		this.document = document;
	}

	public Style createDefaultNetworkNodeStyle() throws IOException {
		if (this.defaultnetworknodestyle == null) {
			this.defaultnetworknodestyle = new Style("defaultnetworknodestyle");
			Icon icon = new Icon(DEFAULTNODEICON);

			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTNODEICON);
			IconStyle iStyle = new IconStyle(icon, MATSIMRED, IconStyle.DEFAULT_COLOR_MODE, ICONSCALE);
			this.defaultnetworknodestyle.setIconStyle(iStyle);
			this.document.addStyle(this.defaultnetworknodestyle);
//			LineStyle lineStyle = new LineStyle(MATSIMGREY, ColorStyle.DEFAULT_COLOR_MODE, 12);
//			style.setLineStyle(lineStyle);
		}
		return this.defaultnetworknodestyle;
	}

	public Style createDefaultNetworkLinkStyle() throws IOException {
		if (this.defaultnetworklinkstyle == null) {
			this.defaultnetworklinkstyle = new Style("defaultnetworklinkstyle");
			Icon icon = new Icon(DEFAULTLINKICON);

			this.writer.addNonKMLFile(MatsimResource.getAsInputStream(DEFAULTNODEICONRESOURCE), DEFAULTLINKICON);
			IconStyle iStyle = new IconStyle(icon, MATSIMWHITE, IconStyle.DEFAULT_COLOR_MODE, ICONSCALE);
			this.defaultnetworklinkstyle.setIconStyle(iStyle);
			LineStyle lineStyle = new LineStyle(MATSIMGREY, ColorStyle.DEFAULT_COLOR_MODE, 12);
			this.defaultnetworklinkstyle.setLineStyle(lineStyle);
			this.document.addStyle(this.defaultnetworklinkstyle);
		}
		return this.defaultnetworklinkstyle;
	}

}
