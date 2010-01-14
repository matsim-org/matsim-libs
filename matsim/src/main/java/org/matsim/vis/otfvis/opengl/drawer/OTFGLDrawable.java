/* *********************************************************************** *
 * project: org.matsim.*
 * OTFGLDrawable.java
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

package org.matsim.vis.otfvis.opengl.drawer;

import javax.media.opengl.GL;

import org.matsim.vis.otfvis.data.OTFDataReceiver;
import org.matsim.vis.otfvis.gui.OTFDrawable;


/**
 * OTFGLDrawable is the Drawable interface for the OpenGL implementation.
 * Each element that is drawable on screen must implement this interface.
 * @author dstrippgen
 *
 */
public interface OTFGLDrawable extends OTFDrawable {

	/**Presumably, the machinery calls "draw".  For the OpenGL implementation, 
	 * "draw" always calls "onDraw" and then exits.  So for the OpenGL
	 * implementation, "onDraw" needs to be implemented in order to obtain
	 * "draw" functionality. ???
	 * 
	 * @param gl
	 */
	public void onDraw(GL gl);
}

