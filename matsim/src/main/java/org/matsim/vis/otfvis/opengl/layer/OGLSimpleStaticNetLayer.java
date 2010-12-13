/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleStaticNetLayer.java
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
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.gui.OTFDrawable;
import org.matsim.vis.otfvis.opengl.drawer.OGLProvider;


/**
 * SimpleStaticNetLayer draws the Network.
 * 
 * It uses an OpenGL "Display List" to speed up drawing. The Display List essentially collects all draw commands from the link drawers ("items")
 * and caches them for extremely fast redrawing.
 * 
 * There is no invalidation, except when the link width in the configuration is changed. So, this layer is really "static".
 * 
 * WARNING: Contrary to what one might think, a new instance of every layer is created in every timestep!!!
 * That the data is static is accomplished by.. well.. static members.
 * 
 * (michaz 12-2010)
 *
 * @author dstrippgen
 *
 */
public class OGLSimpleStaticNetLayer implements SceneLayer {

	private final List<OTFDrawable> items = new ArrayList<OTFDrawable>();
	
	private OGLProvider myDrawer;
	
	private static int netDisplList = -1;
	
	public static float cellWidth_m = 30.f;

	@Override
	public void addItem(OTFDataReceiver item) {
		items.add((OTFDrawable)item);
	}

	public OGLSimpleStaticNetLayer() {
		// Empty constructor
	}

	@Override
	public void draw() {
		GL gl = myDrawer.getGL();
		checkNetList(gl);
		Color netColor = OTFClientControl.getInstance().getOTFVisConfig().getNetworkColor();
		float[] components = netColor.getColorComponents(new float[4]);
		gl.glColor4d(components[0], components[1], components[2], netColor.getAlpha() / 255.0f);
		gl.glCallList(netDisplList);
	}

	@Override
	public void init(SceneGraph graph, boolean initConstData) {
		myDrawer = (OGLProvider)graph.getDrawer();
	}

	@Override
	public int getDrawOrder() {
		return 1;
	}

	@Override
	public void finish() {
		
	}

	@Override
	public OTFDataReceiver newInstance(Class<? extends OTFDataReceiver> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void checkNetList(GL gl) {
		List<OTFDrawable> it = items;
		float cellWidthAct_m = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
		if (cellWidth_m != cellWidthAct_m){
			gl.glDeleteLists(netDisplList, 1);
			netDisplList = -2;
		}
		if (netDisplList < 0) {
			cellWidth_m = cellWidthAct_m;
			netDisplList = gl.glGenLists(1);
			gl.glNewList(netDisplList, GL.GL_COMPILE);
			for (OTFDrawable item : it) {
				item.draw();
			}
			gl.glEndList();
		}
	}

}
