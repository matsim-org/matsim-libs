/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleSceneLayer.java
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.vis.otfvis.opengl.drawer.OTFGLAbstractDrawable;


/**
 *  * The SimpleSceneLayer is the one SceneLayer that is guaranteed to be present in the OTFVis.
 * Every element that is not mapped to a specific layer is added to this layer.

 * @author dstrippgen
 *
 */
public class SimpleSceneLayer implements SceneLayer {
	
	private final List<OTFGLAbstractDrawable> items = new ArrayList<OTFGLAbstractDrawable>();

	public void addItem(OTFGLAbstractDrawable item) {
		items.add(item);
	}
	
	@Override
	public void glInit() {
		// Nothing to do
	}

	@Override
	public void draw() {
		for(OTFGLAbstractDrawable item : items) {
			item.draw();
		}
	}
	
	@Override
	public int getDrawOrder() {
		return 100;
	}
	
}