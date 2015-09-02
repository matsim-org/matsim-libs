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

import com.jogamp.opengl.GL2;

import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.vis.otfvis.OTFClientControl;
import org.matsim.vis.otfvis.caching.SceneLayer;
import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;


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

	private final List<OTFGLAbstractDrawable> items = new ArrayList<OTFGLAbstractDrawable>();

	private static Rect cachedRect = null ;

	private static int netDisplList = -1;
	
	private static int nItems = 0;
	
	private static float cachedLinkWidth = 30.f;

	public void addItem(OTFGLAbstractDrawable item) {
		items.add(item);
	}

	public OGLSimpleStaticNetLayer() {
		// Empty constructor
	}

	@Override
	public void draw() {
		GL2 gl = OTFGLAbstractDrawable.getGl().getGL2();
		checkNetList(gl);
		Color netColor = OTFClientControl.getInstance().getOTFVisConfig().getNetworkColor();
		float[] components = netColor.getColorComponents(new float[4]);
		gl.glColor4d(components[0], components[1], components[2], netColor.getAlpha() / 255.0f);
		gl.glCallList(netDisplList);
	}

	@Override
	public void glInit() {
		GL2 gl = OTFGLAbstractDrawable.getGl().getGL2();
		checkNetList(gl);
	}

	@Override
	public int getDrawOrder() {
		return 1;
	}


	private void checkNetList(GL2 gl) {
		float currentLinkWidth = OTFClientControl.getInstance().getOTFVisConfig().getLinkWidth();
		Rect rect = OTFClientControl.getInstance().getMainOTFDrawer().getViewBoundsAsQuadTreeRect() ;
		if ( cachedLinkWidth != currentLinkWidth || items.size() > nItems || !rect.equals(cachedRect) ) {
			// If the line width has changed (reason for redrawing)
			// or if the number of visible links is bigger than last time
			// (i.e. the user has zoomed out) or the user has resized the window
			// we need to recreate the display list.
			gl.glDeleteLists(netDisplList, 1);
			netDisplList = -2;
		}
		if (netDisplList < 0) {
			cachedRect = rect ;
			cachedLinkWidth = currentLinkWidth;
			netDisplList = gl.glGenLists(1);
			gl.glNewList(netDisplList, GL2.GL_COMPILE);
			for (OTFGLAbstractDrawable item : items) {
				item.draw();
			}
			nItems = items.size();
			gl.glEndList();
		}
	}
	
}
