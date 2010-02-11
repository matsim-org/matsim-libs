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

package org.matsim.vis.otfvis.caching;

import org.matsim.vis.otfvis.data.OTFDataReceiver;

/**
 * The interface SceneLayer has to be implemented from each class the will be added to the SceneGraph as a Layer.
 * The SceneLayer can take OTFData.Recevier elements. It Is also responsible for creating the Receivers associated with this Layer.
 * 
 * @author dstrippgen
 *
 */
public interface SceneLayer {
	
	public void init(SceneGraph graph, boolean initConstData);
	
	public void finish();
	
	public void addItem(OTFDataReceiver item);
	
	public void draw();
	
	public OTFDataReceiver newInstance(Class<? extends OTFDataReceiver> clazz) throws InstantiationException, IllegalAccessException;
	
	public int getDrawOrder();
	
}

