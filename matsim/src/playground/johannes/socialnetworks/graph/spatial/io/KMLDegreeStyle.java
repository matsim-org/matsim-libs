/* *********************************************************************** *
 * project: org.matsim.*
 * SNKMLDegreeStyle.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.spatial.io;

import gnu.trove.TIntObjectHashMap;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.apache.commons.math.stat.StatUtils;

import playground.johannes.socialnetworks.graph.Graph;
import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Vertex;
import playground.johannes.socialnetworks.graph.social.util.ColorUtils;

/**
 * @author illenberger
 *
 */
public class KMLDegreeStyle implements KMLObjectStyle<Graph, Vertex> {

	private static final String VERTEX_STYLE_PREFIX = "vertex.style.";
	
	private ObjectFactory objectFactory = new ObjectFactory();
	
	private TIntObjectHashMap<String> styleIdMappings;
	
	private LinkType vertexIconLink;
	
	private boolean logscale;

	public KMLDegreeStyle(LinkType vertexIconLink) {
		this.vertexIconLink = vertexIconLink;
	}
	
	public void setLogscale(boolean logscale) {
		this.logscale = logscale;
	}
	
	public List<StyleType> getObjectStyle(Graph graph) {
		double[] degrees = GraphStatistics.degreeDistribution(graph).absoluteDistribution().keys();
		double k_min = StatUtils.min(degrees);
		double k_max = StatUtils.max(degrees);
		
		List<StyleType> styleTypes = new ArrayList<StyleType>(degrees.length);
		styleIdMappings = new TIntObjectHashMap<String>();
		
		
		for(double k : degrees) {
			StyleType styleType = objectFactory.createStyleType();
			styleType.setId(getVertexStyleId((int)k));
			
			IconStyleType iconStyle = objectFactory.createIconStyleType();
			iconStyle.setIcon(vertexIconLink);
			iconStyle.setScale(0.5);
			
			double color = -1;
			if(logscale) {
				double min = Math.log(k_min + 1);
				double max = Math.log(k_max + 1);
				color = (Math.log(k + 1) - min) / (max - min);
			} else {
				color = (k - k_min) / (k_max - k_min);
			}
			
			Color c = ColorUtils.getHeatmapColor(color);
			iconStyle.setColor(new byte[]{(byte)c.getAlpha(), (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			
			styleType.setIconStyle(iconStyle);
			
			styleTypes.add(styleType);
			styleIdMappings.put((int)k, styleType.getId());
		}
		
		return styleTypes;
	}

	public String getObjectSytleId(Vertex object) {
		return styleIdMappings.get(object.getEdges().size());
	}
	
	private String getVertexStyleId(int k) {
		return VERTEX_STYLE_PREFIX + Integer.toString(k);
	}
}
