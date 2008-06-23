/* *********************************************************************** *
 * project: org.matsim.*
 * OGLSimpleBackgroundLayer.java
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

package org.matsim.utils.vis.otfvis.opengl.layer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.utils.vis.otfvis.caching.DefaultSceneLayer;
import org.matsim.utils.vis.otfvis.caching.SceneGraph;
import org.matsim.utils.vis.otfvis.data.OTFClientQuad;
import org.matsim.utils.vis.otfvis.data.OTFData.Receiver;
import org.matsim.utils.vis.otfvis.opengl.drawer.AbstractBackgroundDrawer;


public class OGLSimpleBackgroundLayer extends DefaultSceneLayer{

	private static double offsetEast;
	private static double offsetNorth;
	private final static List<AbstractBackgroundDrawer> items = new ArrayList<AbstractBackgroundDrawer>();

	@Override
	public void init(final SceneGraph graph) {
		if (graph.getDrawer() != null) {
			final OTFClientQuad quad = graph.getDrawer().getQuad();
			
			offsetEast = quad.offsetEast;
			offsetNorth = quad.offsetNorth;
		}
	}

	/* (non-Javadoc)
	 * @see playground.david.vis.data.PersistentSceneLayer#addItem(playground.david.vis.data.OTFData.Receiver)
	 */
	@Override
	public void addItem(final Receiver item) {
		final AbstractBackgroundDrawer drawer = (AbstractBackgroundDrawer)item;
		items.add(drawer);
	}
	
	public static void addPersistentItem(final AbstractBackgroundDrawer drawer) {
		items.add(drawer);
	}

	/* (non-Javadoc)
	 * @see playground.david.vis.data.DefaultSceneLayer#draw()
	 */
	@Override
	public void draw() {
		for(final AbstractBackgroundDrawer item : items) {
			item.setOffset(offsetEast, offsetNorth);
			item.draw();
		}
	}

	public int getDrawOrder() {
		return 0;
	}
}
