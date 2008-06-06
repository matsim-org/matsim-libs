/* *********************************************************************** *
 * project: org.matsim.*
 * SceneLayer.java
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

import org.matsim.utils.vis.otfvis.data.OTFData;

public interface SceneLayer {
	public void init(SceneGraph graph);
	public void finish();
	public void addItem(OTFData.Receiver item);
	public void draw();
	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException;
	public int getDrawOrder();
	
}

