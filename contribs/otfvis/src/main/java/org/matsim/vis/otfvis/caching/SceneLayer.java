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


/**
 * The interface SceneLayer has to be implemented from each class the will be added to the SceneGraph as a Layer.
 * The SceneLayer can take OTFData.Recevier elements. It Is also responsible for creating the Receivers associated with this Layer.
 * 
 * @author dstrippgen
 *
 */
public interface SceneLayer {
	
	
	/**
	 * 
	 * This is the OpenGL init command passed down. OpenGL may call init any time, for example when switching
	 * displays, re-creating buffers etc., so layers need to be notified of this.
	 * Particularly, the OGLSimpleStaticNetLayer needs this message to recreate its OpenGL display lists
	 * when the OpenGL context changes.
	 *
	 * Swing implementations don't need this, and OpenGL implementations need to get the OpenGL context
	 * from a static variable somewhere. Sorry. I think it is not really possible to put drawables for
	 * two graphics frameworks with completely different protocols behind a common interface.
	 * 
	 */
	void glInit();

	void draw();
	
	int getDrawOrder();
	
}

