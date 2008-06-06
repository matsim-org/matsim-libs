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

package org.matsim.utils.vis.otfvis.caching;

import java.util.ArrayList;
import java.util.List;

import org.matsim.utils.vis.otfvis.data.OTFData.Receiver;
import org.matsim.utils.vis.otfvis.gui.OTFDrawable;


public class SimpleSceneLayer extends DefaultSceneLayer {
	protected final List<OTFDrawable> items = new ArrayList<OTFDrawable>();

	@Override
	public void addItem(Receiver item) {
		items.add((OTFDrawable)item);
	}

	@Override
	public void draw() {
		for(OTFDrawable item : items) item.draw();
	}
	
	@Override
	public void finish() {
	}

	@Override
	public void init(SceneGraph graph) {
	}

	public int getDrawOrder() {
		return 100;
	}

	
	
}