/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultSceneLayer.java
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
 * The DefaultSceneLayer is an empty SceneLayer that implements all interface methods as empty methods.
 * It is there solely for the programmer's convenience.
 * 
 * @author dstrippgen
 *
 */
public abstract class DefaultSceneLayer implements SceneLayer {

	public void addItem(OTFDataReceiver item) {
	}

	public void draw() {
	}

	public void finish() {
	}

	public void init(SceneGraph graph, boolean initConstData) {
	}

	public Object newInstance(Class clazz) throws InstantiationException, IllegalAccessException {
		return clazz.newInstance();
	}
}
