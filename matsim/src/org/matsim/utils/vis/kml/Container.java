/* *********************************************************************** *
 * project: org.matsim.*
 * Container.java
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.gbl.Gbl;
import org.matsim.utils.vis.kml.KMLWriter.XMLNS;

/**
 * For documentation, refer to
 * <a href="http://code.google.com/apis/kml/documentation/kmlreference.html#container">
 * http://code.google.com/apis/kml/documentation/kmlreference.html#container</a>
 * 
 * @author dgrether, meisterk, mrieser
 * @deprecated For working with KML files, please use the library kml-2.2-jaxb-2.1.7.jar. 
 * See ch.ethz.ivt.KMLDemo in that library for examples of usage.
 *
 */
public abstract class Container extends Feature {

	private final Map<String, Feature> features;

	/**
	 * Constructs a container with default values
	 * of its {@link Feature} attributes.
	 * @param id the id of this kml element
	 */
	public Container(final String id) {

		super(
				id,
				Feature.DEFAULT_NAME,
				Feature.DEFAULT_DESCRIPTION,
				Feature.DEFAULT_ADDRESS,
				Feature.DEFAULT_LOOK_AT,
				Feature.DEFAULT_STYLE_URL,
				Feature.DEFAULT_VISIBILITY,
				Feature.DEFAULT_REGION,
				Feature.DEFAULT_TIME_PRIMITIVE
				);
		this.features = new HashMap<String, Feature>();

	}

	/**
	 * Constructs a container with user-defined values
	 * of its {@link Feature} attributes.
	 *
	 * @param id The id allows unique identification of a KML element.
	 * @param name
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#name">
	 * name</a> of the new container.
	 * @param description
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#description">
	 * description</a> of the new container.
	 * @param address
	 * @param lookAt
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#lookat">
	 * lookAt</a> property of the new container.
	 * @param styleUrl
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#styleurl">
	 * style URL</a> of the new container.
	 * @param visibility
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#visibility">
	 * visibility</a> of the new container.
	 * @param region
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#region">
	 * region</a> of the new container.
	 * @param timePrimitive
	 * the <a href="http://earth.google.com/kml/kml_tags_21.html#timeprimitive">
	 * time primitive</a> of the new container.
	 */
	public Container(
			final String id,
			final String name,
			final String description,
			final String address,
			final LookAt lookAt,
			final String styleUrl,
			final boolean visibility,
			final Region region,
			final TimePrimitive timePrimitive) {

		super(
				id,
				name,
				description,
				address,
				lookAt,
				styleUrl,
				visibility,
				region,
				timePrimitive);
		this.features = new HashMap<String, Feature>();

	}

	/**
	 * Adds a feature ({@link Placemark}, {@link Folder} etc.)
	 * to this container.
	 *
	 * @param feature the new feature
	 */
	public void addFeature(final Feature feature) {

		if (this.features.containsKey(feature.getId())) {
			Gbl.errorMsg("A feature with the id \"" + feature.getId() + "\" already exists.");
		}

		this.features.put(feature.getId(), feature);

	}


	@Override
	protected void writeObject(
			final BufferedWriter out,
			final XMLNS version,
			final int offset,
			final String offsetString)
	throws IOException {

		super.writeObject(out, version, offset, offsetString);

		Iterator<Feature> featureIterator = this.features.values().iterator();
		while (featureIterator.hasNext()) {
			Feature feature = featureIterator.next();
			feature.writeObject(out, version, offset, offsetString);
		}

	}

	public boolean containsFeature(final String id) {

		return this.features.containsKey(id);

	}

}
