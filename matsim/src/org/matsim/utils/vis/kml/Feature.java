/* *********************************************************************** *
 * project: org.matsim.*
 * Feature.java
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
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://earth.google.com/kml/kml_tags_21.html#feature">
 * http://earth.google.com/kml/kml_tags_21.html#feature</a>
 */
public abstract class Feature extends Object {

	private String name;
	private String description;
	private String address;
	private LookAt lookAt;
	private String styleUrl;
	private boolean visibility;
	private final Region region;
	private TimePrimitive timePrimitive;
	private final TreeMap<String, Style> styles;

	public static final String DEFAULT_NAME = null;
	public static final String DEFAULT_DESCRIPTION = null;
	public static final String DEFAULT_ADDRESS = null;
	public static final LookAt DEFAULT_LOOK_AT = null;
	public static final String DEFAULT_STYLE_URL = null;
	public static final boolean DEFAULT_VISIBILITY = true;
	public static final Region DEFAULT_REGION = null;
	public static final TimePrimitive DEFAULT_TIME_PRIMITIVE = null;
	
	private final static Logger log = Logger.getLogger(Feature.class);

	/**
	 * Constructs the feature with default values of its attributes.
	 *
	 * @param id The id allows unique identification of a KML element.
	 */
	public Feature(final String id) {

		super(id);
		this.name = Feature.DEFAULT_NAME;
		this.description = Feature.DEFAULT_DESCRIPTION;
		this.address = Feature.DEFAULT_ADDRESS;
		this.lookAt = Feature.DEFAULT_LOOK_AT;
		this.styleUrl = Feature.DEFAULT_STYLE_URL;
		this.visibility = Feature.DEFAULT_VISIBILITY;
		this.region = Feature.DEFAULT_REGION;
		this.timePrimitive = Feature.DEFAULT_TIME_PRIMITIVE;
		this.styles = new TreeMap<String, Style>();

	}

	/**
	 * Constructs the feature with user-defined values of its attributes.
	 *
	 * @param id The id allows unique identification of a KML element.
	 * @param name
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#name">
	 * name</a> of the new feature.
	 * @param description
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#description">
	 * description</a> of the new feature.
	 * @param address
	 * @param lookAt
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#lookat">
	 * lookAt</a> property of the new feature.
	 * @param styleUrl
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#styleurl">
	 * style URL</a> of the new feature.
	 * @param visibility
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#visibility">
	 * visibility</a> of the new feature.
	 * @param region
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#region">
	 * region</a> of the new feature.
	 * @param timePrimitive
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#timeprimitive">
	 * time primitive</a> of the new feature.
	 */
	public Feature(
			final String id,
			final String name,
			final String description,
			final String address,
			final LookAt lookAt,
			final String styleUrl,
			final boolean visibility,
			final Region region,
			final TimePrimitive timePrimitive) {

		super(id);
		this.name = name;
		this.description = description;
		this.address = address;
		this.lookAt = lookAt;
		this.styleUrl = styleUrl;
		this.visibility = visibility;
		this.region = region;
		this.timePrimitive = timePrimitive;
		this.styles = new TreeMap<String, Style>();

	}

	@Override
	protected void writeObject(
			final BufferedWriter out,
			final XMLNS version,
			final int offset,
			final String offsetString)
	throws IOException {

		if (!Feature.DEFAULT_NAME.equals(this.name)) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<name>");
			out.write(this.name);
			out.write("</name>");
			out.newLine();
		}

		if (!Feature.DEFAULT_DESCRIPTION.equals(this.description)) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<description>");
			out.write(this.description);
			out.write("</description>");
			out.newLine();
		}

		if (!Feature.DEFAULT_ADDRESS.equals(this.address)) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<address>");
			out.write(this.address);
			out.write("</address>");
			out.newLine();
		}

		if (!Feature.DEFAULT_LOOK_AT.equals(this.lookAt)) {
			this.lookAt.writeObject(out, version, offset, offsetString);
		}

		if (!Feature.DEFAULT_STYLE_URL.equals(this.styleUrl)) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<styleUrl>");
			out.write(this.styleUrl);
			out.write("</styleUrl>");
			out.newLine();
		}

		if (this.visibility != Feature.DEFAULT_VISIBILITY) {
			out.write(Object.getOffset(offset, offsetString));
			out.write("<visibility>");
			out.write("0");
			out.write("</visibility>");
			out.newLine();
		}

		// regions don't exist in KML version 2.0
		if (XMLNS.V_21.equals(version)) {
			if (this.region != Feature.DEFAULT_REGION) {
				this.region.writeObject(out, version, offset, offsetString);
			}
		}

		if (this.timePrimitive != Feature.DEFAULT_TIME_PRIMITIVE) {
			this.timePrimitive.writeObject(out, version, offset, offsetString);
		}

		Iterator<Style> style_it = this.styles.values().iterator();
		while (style_it.hasNext()) {
			Object style = style_it.next();
			style.writeObject(out, version, offset, offsetString);
		}

	}

	/**
	 * Adds a style to this feature.
	 *
	 * @param style the style to be added
	 */
	public void addStyle(final Style style) {

		if (this.styles.containsKey(style.getId())) {
			log.warn("A style with the id \"" + style.getId() + "\" already exists. It is replaced by the new one.");
		}
		this.styles.put(style.getId(), style);

	}

	public boolean containsStyle(final String id) {
		return this.styles.containsKey(id);
	}

	/**
	 * @param id
	 * @return the style object with id id
	 */
	public Style getStyle(final String id) {

		if (!(this.styles.containsKey(id))) {
			Gbl.errorMsg("A style with the id \"" + id + "\" doesn't exist.");
		}

		return this.styles.get(id);
	}

	/**
	 * Sets the LookAt property of this feature.
	 *
	 * @param lookAt
	 */
	public void setLookAt(final LookAt lookAt) {
		this.lookAt = lookAt;
	}

	/**
	 * Sets the timePrimitive property of this feature
	 *
	 * @param timePrimitive
	 */
	public void setTimePrimitive(final TimePrimitive timePrimitive) {
		this.timePrimitive = timePrimitive;
	}

	/**
	 * Sets the style to display this feature.
	 *
	 * @param styleUrl
	 */
	public void setStyleUrl(final String styleUrl) {
		this.styleUrl = styleUrl;
	}
	/**
	 * Sets the name of this feature.
	 *
	 * @param name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Sets the description of this feature.
	 *
	 * @param description
	 */
	public void setDescription(final String description) {
		this.description = description;
	}
	/**
	 * If this feature should not initially be drawn in
	 * the 3d viewer the visibility should be set to false.
	 * @param visible
	 */
	public void setVisibility(final boolean visible) {
		this.visibility = visible;
	}
}
