/* *********************************************************************** *
 * project: org.matsim.*
 * OGLAgentPointLayer.java
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

package org.matsim.vis.otfvis.opengl.layer;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.nio.FloatBuffer;
import java.util.Arrays;

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.data.OTFDataReceiver;


/**
 * OGLAgentPointLayer is responsible for drawing the agents/vehicles as point sprites.
 * It is a very fast way to draw massive (100ks) of agents in realtime.
 * It does not run too well on ATI cards, though.
 *
 * @author dstrippgen
 *
 */
public class OGLAgentPointLayer implements SceneLayer {

	final static int BUFFERSIZE = 10000;

	private final AgentArrayDrawer drawer = new AgentArrayDrawer();
	
	private final AgentPointDrawer pointdrawer = new AgentPointDrawer(this);

	public OGLAgentPointLayer() {
		// Empty constructor.
	}

	@Override
	public void draw() {
		this.drawer.draw();
	}

	@Override
	public void finish() {
	}

	@Override
	public void init(SceneGraph graph, boolean initConstData) {
	}

	@Override
	public OTFDataReceiver newInstanceOf(Class<? extends OTFDataReceiver> clazz) {
		return this.pointdrawer;
	}

	@Override
	public int getDrawOrder() {
		return 100;
	}

	public Point2D.Double getAgentCoords(char [] id) {
		int idNr = Arrays.hashCode(id);
		Integer i = this.drawer.getId2coord().get(idNr);
		if (i != null) {
			FloatBuffer vertex = this.drawer.getPosBuffers().get(i / BUFFERSIZE);
			int innerIdx = i % BUFFERSIZE;
			float x = vertex.get(innerIdx*2);
			float y = vertex.get(innerIdx*2+1);
			return new Point2D.Double(x,y);
		}
		return null;
	}

	@Override
	public void addItem(OTFDataReceiver item) {
		
	}

	public void addAgent(char[] id, float startX, float startY, Color mycolor, boolean saveId) {
		drawer.addAgent(id, startX, startY, mycolor, saveId);
	}


}
