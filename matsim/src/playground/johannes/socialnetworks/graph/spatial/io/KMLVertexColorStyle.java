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

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TIntObjectHashMap;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.opengis.kml._2.IconStyleType;
import net.opengis.kml._2.LinkType;
import net.opengis.kml._2.ObjectFactory;
import net.opengis.kml._2.StyleType;

import org.apache.commons.math.stat.StatUtils;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;

import playground.johannes.socialnetworks.graph.social.util.ColorUtils;

/**
 * @author illenberger
 *
 */
public abstract class KMLVertexColorStyle<G extends Graph, V extends Vertex> implements KMLObjectStyle<G, V> {

//	private static final String VERTEX_STYLE_PREFIX = "vertex.style.";
	
	private ObjectFactory objectFactory = new ObjectFactory();
	
	private TDoubleObjectHashMap<String> styleIdMappings;
	
	private LinkType vertexIconLink;
	
	private boolean logscale;

	public KMLVertexColorStyle(LinkType vertexIconLink) {
		this.vertexIconLink = vertexIconLink;
	}
	
	public void setLogscale(boolean logscale) {
		this.logscale = logscale;
	}
	
	public List<StyleType> getObjectStyle(G graph) {
		TDoubleObjectHashMap<String> values = getValues(graph);
		double[] keys = values.keys();
		double min = StatUtils.min(keys);
		double max = StatUtils.max(keys);
		
		List<StyleType> styleTypes = new ArrayList<StyleType>(keys.length);
		styleIdMappings = new TDoubleObjectHashMap<String>();
		
		
		for(double val : keys) {
			StyleType styleType = objectFactory.createStyleType();
			styleType.setId(values.get(val));
			
			IconStyleType iconStyle = objectFactory.createIconStyleType();
			iconStyle.setIcon(vertexIconLink);
			iconStyle.setScale(0.5);
//			iconStyle.setScale(1.0);
			
			Color c = colorForValue(val, min, max);
			iconStyle.setColor(new byte[]{(byte)c.getAlpha(), (byte)c.getBlue(), (byte)c.getGreen(), (byte)c.getRed()});
			
			styleType.setIconStyle(iconStyle);
			
			styleTypes.add(styleType);
			styleIdMappings.put(val, styleType.getId());
		}
		
		return styleTypes;
	}

	protected abstract TDoubleObjectHashMap<String> getValues(G graph);
	
	protected abstract double getValue(V vertex);
	
	protected Color colorForValue(double val, double min, double max) {
		double color = -1;
		if(logscale) {
			double min2 = Math.log(min + 1);
			double max2 = Math.log(max + 1);
			color = (Math.log(val + 1) - min2) / (max2 - min2);
		} else {
			color = (val - min) / (max - min);
		}
		
		return ColorUtils.getHeatmapColor(color);
	}
	
	public String getObjectSytleId(V object) {
		return styleIdMappings.get(getValue(object));
	}
	
//	private String getVertexStyleId(double k) {
//		return VERTEX_STYLE_PREFIX + Double.toString(k);
//	}
}
