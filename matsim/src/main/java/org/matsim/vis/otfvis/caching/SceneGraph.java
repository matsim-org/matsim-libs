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

package org.matsim.vis.otfvis.caching;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.data.OTFConnectionManager;
import org.matsim.vis.otfvis.data.OTFDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;
import org.matsim.vis.otfvis.opengl.layer.AgentPointDrawer;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;
import org.matsim.vis.otfvis.opengl.layer.OGLSimpleStaticNetLayer;



/**
 *
 * The LayerDrawingOrderComparator is used to order the layers in ascending order
 * by their getDrawOrder() method
 *
 * @author dstrippgen
 *
 */
class LayerDrawingOrderComparator implements Comparator<SceneLayer>, Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public int compare(SceneLayer o1, SceneLayer o2) {
		int diff = (int)Math.signum(o1.getDrawOrder() - o2.getDrawOrder());
		return diff;
	}

}


/**
 * The SceneGraph is responsible for holding all information necessary to draw a particular timestep.
 * Once a SceneGraph is constructed, the Reader/Writer and the QuadTree will not be asked for information any longer.
 * Instead the SceneGraph's draw() method will be called.
 *
 * @author dstrippgen
 *
 */
public class SceneGraph {

	private Rect rect;
	
	private OGLAgentPointLayer agentLayer = new OGLAgentPointLayer();
	private OGLSimpleStaticNetLayer networkLayer = new OGLSimpleStaticNetLayer();
	private SimpleSceneLayer miscellaneousLayer = new SimpleSceneLayer();
	
	private final OTFOGLDrawer drawer;
	private final double time;

	private ArrayList<SceneLayer> drawingLayers;

	/**
	 * @return the time
	 */
	public double getTime() {
		return time;
	}

	public SceneGraph(Rect rect, double time, OTFConnectionManager connect, OTFOGLDrawer drawer) {
		this.rect = rect;
		this.drawer = drawer;
		this.time = time;
		this.drawingLayers = new ArrayList<SceneLayer>();
		this.drawingLayers.add(miscellaneousLayer);
		this.drawingLayers.add(networkLayer);
		this.drawingLayers.add(agentLayer);	
		for (SceneLayer layer : drawingLayers) {
			layer.init(time == -1 ? true : false);
		}
	}

	public Rect getRect() {
		return this.rect;
	}

	public void setRect(Rect rec) {
		this.rect = rec;
	}

	public OTFOGLDrawer getDrawer() {
		return drawer;
	}

	public void addItem(OTFDrawable item) {
		miscellaneousLayer.addItem(item);
	}

	public void addStaticItem(OTFDrawable item) {
		networkLayer.addItem(item);
	}
	
	public void finish() {
		Collections.sort(drawingLayers, new LayerDrawingOrderComparator());
		for (SceneLayer layer : drawingLayers) {
			layer.finish();
		}
	}
	
	public OGLAgentPointLayer getAgentPointLayer() {
		return agentLayer;
	}

	public void draw() {
		for (SceneLayer layer : drawingLayers) {
			layer.draw();
		}
	}

	public void glInit() {
		for (SceneLayer layer : drawingLayers) {
			layer.glInit();
		}
	}

	public AgentPointDrawer getAgentPointDrawer() {
		return (AgentPointDrawer) agentLayer.newInstanceOf(AgentPointDrawer.class);
	}

}

