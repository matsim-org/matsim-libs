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

import org.matsim.vis.otfvis.gui.OTFDrawable;


/**
 * History:<ul>
 * <li> There used to be an "OTFGLDrawableImpl implements OTFOGLDrawable".
 * This class here is replacing both the Impl (which was abstract anyway) and the interface (which was only used for this
 * one abstract implementation.
 * <li> An advantage of making this abstract instead of an interface is that onDraw can remain protected, only exposing
 * draw() as the true public interfaces (as it should be as far as I can currently see).
 * <li> There used to be an "isValid" but as far as I could tell it did not do anything.
 * </ul>
 * kai, feb'11
 * 
 * @author dstrippgen
 *
 */
public abstract class OTFGLAbstractDrawable implements OTFDrawable {

	//	private boolean isValid = true; // setting this from "default" to "private" curiously does not seem to make a difference.  kai, jan'10

	private static GL gl; // setting this to private seems to work.  kai, jan'10
	// yyyyyy michaz writes that the SceneLayers are reinstantiated in every time step.  In consequence, display speed critically 
	// depends on the fact that the infrastructure behind the SceneLayers is NOT taken down in every time step.  This seems
	// to be achieved by static variables such as this one.  kai, jan'11

	public final void draw() {
		// Make sure onDraw is called only once per object	// this comment may have had something to do with the isValid, which 
															// did not work (I think).  kai, feb'11
		onDraw(gl);
//		isValid = true;
	}
	
	abstract protected void onDraw( GL gl ) ;

	/**<p>
	 * This setter is, if I see it correctly, only called once, in "drawNetList" inside "OTFOGLDrawer".
	 * OTFOGLDrawer has its own (private, non-static) gl variable.  In that call, the content of that non-static
	 * variable is pushed to the static variable here. --???  kai, jan'10
	 * </p>
	 *
	 * @param gl
	 */
	static void setGl(GL gl) {
		// package-private seems to be sufficient--?  kai, jan'11
		OTFGLAbstractDrawable.gl = gl;
	}
	protected final static GL getGl() {
		return gl;
	}

}