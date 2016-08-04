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
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;
import org.matsim.vis.otfvis.opengl.layer.OGLAgentPointLayer;



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
		return (int)Math.signum(o1.getDrawOrder() - o2.getDrawOrder());
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
	private SimpleSceneLayer miscellaneousLayer = new SimpleSceneLayer();

	private ArrayList<SceneLayer> drawingLayers;

	public SceneGraph(Rect rect) {
		this.rect = rect;
		this.drawingLayers = new ArrayList<>();
		this.drawingLayers.add(miscellaneousLayer);
		this.drawingLayers.add(agentLayer);
	}

	public Rect getRect() {
		return this.rect;
	}

	public void setRect(Rect rec) {
		this.rect = rec;
	}

	public void addItem(OTFGLAbstractDrawable item) {
		miscellaneousLayer.addItem(item);
	}

	public void finish() {
		Collections.sort(drawingLayers, new LayerDrawingOrderComparator());
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

}

