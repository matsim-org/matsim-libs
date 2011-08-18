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

	// We need to statically cache the GL context here. The reason is that OTFDrawable tries to 
	// be a common interface for things which are drawable by Swing and things which are drawable by OpenGL.
	// So the context is not passed in with every call to draw (as would usually be the case).
	// So we have to cache it. In the OpenGL community, it is generally
	// recommended *not* to store GL contexts anywhere, because the driver may create and pass
	// a new one at any time.  michaz
	private static GL gl; 

	@Override
	public final void draw() {
		onDraw(gl);
	}
	
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
	public final void glInit() {
		onInit(gl);
	}
	
	protected void onInit(GL gl) {
		// To override.
	}
	
	abstract protected void onDraw( GL gl ) ;

	static void setGl(GL gl) {
		OTFGLAbstractDrawable.gl = gl;
	}
	
	public final static GL getGl() {
		return gl;
	}

}