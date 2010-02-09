/* *********************************************************************** *
 * project: org.matsim.*
 * OTFGLDrawableImpl.java
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

import org.matsim.vis.otfvis.caching.SceneGraph;
import org.matsim.vis.otfvis.data.OTFDataReceiver;


/**
 * OTFGLDrawableImpl is a basic implementation of the OTFOGLDrawable interface with an
 * additional simple invalidation mechanism.
 *
 * @author dstrippgen
 *
 * <p>
 * isValid is set to true or false, but I can't figure out where this is ever used.
 * kai, jan'10
 * </p>
 *
 */
public abstract class OTFGLDrawableImpl implements OTFGLDrawable, OTFDataReceiver {
//	private boolean isValid = true; // setting this from "default" to "private" curiously does not seem to make a difference.  kai, jan'10
	private static GL gl; // setting this from "public" to "protected" seems to work.  kai, jan'10

	public final void draw() {
		// Make sure onDraw is called only once per object
		onDraw(getGl());
//		isValid = true;
	}
	public void invalidate(SceneGraph graph) {
//		isValid = false;
		graph.addItem(this);
	}
	/**<p>
	 * This setter is, if I see it correctly, only called once, in "drawNetList" inside "OTFOGLDrawer".
	 * OTFOGLDrawer has its own (private, non-static) gl variable.  In that call, the content of that non-static
	 * variable is pushed to the static variable here. --???  kai, jan'10
	 * </p>
	 *
	 * @param gl
	 */
	protected static void setGl(GL gl) {
		OTFGLDrawableImpl.gl = gl;
	}
	protected static GL getGl() {
		return gl;
	}

}