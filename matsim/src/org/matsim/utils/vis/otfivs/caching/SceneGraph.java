/* *********************************************************************** *
 * project: org.matsim.*
 * SceneGraph.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.utils.vis.otfivs.caching;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.utils.collections.QuadTree.Rect;
import org.matsim.utils.vis.otfivs.data.OTFConnectionManager;
import org.matsim.utils.vis.otfivs.data.OTFData;
import org.matsim.utils.vis.otfivs.interfaces.OTFDrawer;



class LayerDrawingOrderComparator implements Comparator<SceneLayer>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int compare(SceneLayer o1, SceneLayer o2) {
		int diff = (int)Math.signum(o1.getDrawOrder() - o2.getDrawOrder());

		return diff;
	}
	
}


public class SceneGraph {
	private Rect rect;
	private final Map<Class, SceneLayer> layers = new LinkedHashMap<Class, SceneLayer>();
	private final List<SceneLayer> drawingLayers = new LinkedList<SceneLayer>();

	private final OTFDrawer drawer;
	private final double time;
	
	/**
	 * @return the time
	 */
	public double getTime() {
		return time;
	}

	public SceneGraph(Rect rect, double time, OTFConnectionManager connect, OTFDrawer drawer) {
		this.rect = rect;
		this.drawer = drawer;
		this.time = time;
		
		// default layer, might be overridden from connect!
		layers.put(Object.class, new SimpleSceneLayer());
		
		connect.fillLayerMap(layers);

		// do initialising action if necessary
		for (SceneLayer layer : layers.values()) {
			layer.init(this);
			drawingLayers.add(layer);
		}
		
	}
	
	public Rect getRect() {
		return this.rect;
	}
	
	public void setRect(Rect rec) {
		this.rect = rec;
	}

	public OTFDrawer getDrawer() {
		return drawer;
	}
	
	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
		SceneLayer layer = layers.get(clazz);
		if (layer == null)layer = layers.get(Object.class); //DS must exist: default handling
		return layer.newInstance(clazz);
	}

	public void addItem(OTFData.Receiver item) {
		SceneLayer layer = layers.get(item.getClass());
		if (layer == null)layer = layers.get(Object.class); //DS must exist: default handling
		
		layer.addItem(item);
	}
	
	public void finish() {
		Collections.sort(drawingLayers, new LayerDrawingOrderComparator());
		// do finishing action if necessary
		for (SceneLayer layer : drawingLayers) layer.finish();
	}
	
	public SceneLayer getLayer(Class clazz) {
		SceneLayer layer = layers.get(clazz);
		if (layer == null)layer = layers.get(Object.class); //DS must exist: default handling
		return layer;
	}
	
	public void draw() {
		// do initialising action if necessary
		for (SceneLayer layer : drawingLayers) layer.draw();
	}
}

