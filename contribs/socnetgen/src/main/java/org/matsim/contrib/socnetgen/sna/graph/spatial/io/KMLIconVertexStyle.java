/* *********************************************************************** *
 * project: org.matsim.*
 * KMLDotVertexStyle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.graph.spatial.io;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.matsim.contrib.socnetgen.sna.graph.Graph;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.vis.kml.KMZWriter;

import net.opengis.kml.v_2_2_0.IconStyleType;
import net.opengis.kml.v_2_2_0.LinkType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.StyleType;

/**
 * Implementation of {@link KMLObjectStyle} that displays an icon at the
 * position of the placemark representing a vertex. Per default vertices will be
 * drawn as a dot ("icon18.png" in the main matsim package) colored by
 * {@link VertexDegreeColorizer}. This class is also a {@link KMZWriterListener}
 * and thus requires registering at the {@link SpatialGraphKMLWriter}.
 * 
 * @author jillenberger
 * 
 */
public class KMLIconVertexStyle implements KMLObjectStyle, KMZWriterListener {

//	private static final String ICON_HREF = "vertex.png";
	private static final String ICON_HREF = "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png";

	private final ObjectFactory kmlFactory = new ObjectFactory();

	private Colorizable vertexColorizer;

	private Map<Vertex, StyleType> vertexStyles;

	private String iconName = "icon18.png";

	private final Graph graph;

	/**
	 * Creates icon style object that draws vertices as a dot colored by degree.
	 * 
	 * @param graph
	 *            a graph
	 */
	public KMLIconVertexStyle(Graph graph) {
		this.graph = graph;
		setVertexColorizer(new VertexDegreeColorizer(graph));
	}

	/**
	 * Returns the colorizer used to color vertices. The default colorizer is
	 * {@link VertexDegreeColorizer}.
	 * 
	 * @return the colorizer used to color vertices.
	 */
	public Colorizable getVertexColorizer() {
		return vertexColorizer;
	}

	/**
	 * Sets the colorizer to color vertices.
	 * 
	 * @param vertexColorizer
	 *            a colorizer.
	 */
	public void setVertexColorizer(Colorizable vertexColorizer) {
		this.vertexColorizer = vertexColorizer;
	}

	/**
	 * Returns the name of the image resource used to draw vertices (resource
	 * are in the res-directory o the main matsim package).
	 * 
	 * @return the name of the image resource used to draw vertices.
	 */
	public String getIconName() {
		return iconName;
	}

	/**
	 * Sets the image resource to draw vertices. The resource has to be located
	 * in the res-directory of the main package).
	 * 
	 * @param iconName
	 *            the image resource to draw vertices.
	 */
	public void setIconName(String iconName) {
		this.iconName = iconName;
	}

	private Map<Vertex, StyleType> initVertexStyles(Graph graph) {
		/*
		 * Get the color for all vertices.
		 */
		Map<Vertex, Color> vertexColorMapping = new HashMap<Vertex, Color>();
		Set<Color> colors = new HashSet<Color>();
		for (Vertex v : graph.getVertices()) {
			Color c = vertexColorizer.getColor(v);
			colors.add(c);
			vertexColorMapping.put(v, c);
		}
		/*
		 * Create s style type for all unique colors.
		 */
		LinkType kmlIconLink = kmlFactory.createLinkType();
		kmlIconLink.setHref(ICON_HREF);
		Map<Color, StyleType> colorStyles = new HashMap<Color, StyleType>();
		for (Color c : colors) {
			IconStyleType kmlIconStyle = kmlFactory.createIconStyleType();
			kmlIconStyle.setIcon(kmlIconLink);
			kmlIconStyle.setScale(0.75);
			kmlIconStyle.setColor(new byte[] { (byte) c.getAlpha(), (byte) c.getBlue(), (byte) c.getGreen(),
					(byte) c.getRed() });

			StyleType kmlStyle = kmlFactory.createStyleType();
			kmlStyle.setId(String.format("vertex%1$s", Integer.toHexString(c.getRGB())));
			kmlStyle.setIconStyle(kmlIconStyle);

			colorStyles.put(c, kmlStyle);
		}
		/*
		 * Assign each vertex its style type.
		 */
		Map<Vertex, StyleType> vertexStyleMapping = new HashMap<Vertex, StyleType>();
		for (Entry<Vertex, Color> entry : vertexColorMapping.entrySet()) {
			vertexStyleMapping.put(entry.getKey(), colorStyles.get(entry.getValue()));
		}

		return vertexStyleMapping;
	}

	/**
	 * @see {@link KMLObjectStyle#getStyle(Object)}
	 */
	@Override
	public StyleType getStyle(Object object) {
		if (vertexStyles == null)
			vertexStyles = initVertexStyles(graph);

		return vertexStyles.get(object);
	}

	/**
	 * Does nothing.
	 * 
	 * @see {@link KMZWriterListener#closeWriter(KMZWriter)}
	 */
	@Override
	public void closeWriter(KMZWriter writer) {
	}

	/**
	 * Adds the image resource which is used to draw vertices to the KMZ
	 * archive.
	 * 
	 * @see {@link KMZWriterListener#openWriter(KMZWriter)}
	 */
	@Override
	public void openWriter(KMZWriter writer) {
//		try {
////			writer.addNonKMLFile(MatsimResource.getAsInputStream(iconName), ICON_HREF);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

	}
}
