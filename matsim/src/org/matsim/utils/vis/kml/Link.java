/* *********************************************************************** *
 * project: org.matsim.*
 * Link.java
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

package org.matsim.utils.vis.kml;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#link">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#link</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public class Link extends Object {

	private String href;
	
	/**
	 * For documentation, refer to
	 * <a href="http://earth.google.com/kml/kml_tags_21.html#viewrefreshmode">
	 * http://earth.google.com/kml/kml_tags_21.html#viewrefreshmode</a>
	 */
	public enum ViewRefreshMode {
		
		NEVER ("never"),
		ON_REGION ("onRegion");
		
		private String viewRefreshModeString;
		
		private ViewRefreshMode(String viewRefreshModeString) {
			this.viewRefreshModeString = viewRefreshModeString;
		}

		public String getViewRefreshModeString() {
			return viewRefreshModeString;
		}
		
	}
	
	private ViewRefreshMode viewRefreshMode;
	
	public static final ViewRefreshMode DEFAULT_VIEW_REFRESH_MODE = ViewRefreshMode.NEVER;
	
	/**
	 * Constructs a link with the required href attribute.
	 * 
	 * @param href
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#href">
	 * href</a> property of the new link.
	 */
	public Link(String href) {
		super("");
		this.href = href;
		this.viewRefreshMode = Link.DEFAULT_VIEW_REFRESH_MODE;
	}

	/**
	 * Constructs a link with the required <code>href</code> and the optional 
	 * <code>viewRefreshMode</code> attribute.
	 * 
	 * @param href
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#href">
	 * href</a> property of the new link.
	 * @param viewRefreshMode
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#viewrefreshmode">
	 * viewrefreshmode</a> property of the new link.
	 */
	public Link(String href, ViewRefreshMode viewRefreshMode) {
		super("");
		this.href = href;
		this.viewRefreshMode = viewRefreshMode;
	}
	
	@Override
	protected void writeObject(BufferedWriter out, XMLNS version, int offset,
			String offsetString) throws IOException {

		out.write(Object.getOffset(offset, offsetString));
		out.write("<Link>");
		out.newLine();

		out.write(Object.getOffset(offset + 1, offsetString));
		out.write("<href>" + this.href + "</href>");
		out.newLine();		
		
		if (this.viewRefreshMode != Link.DEFAULT_VIEW_REFRESH_MODE) {
			out.write(Object.getOffset(offset + 1, offsetString));
			out.write("<viewRefreshMode>" + this.viewRefreshMode.getViewRefreshModeString() + "</viewRefreshMode>");
			out.newLine();
		}		
		out.write(Object.getOffset(offset, offsetString));
		out.write("</Link>");
		out.newLine();

	}

}
