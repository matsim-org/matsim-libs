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


public interface OTFGLDrawable extends OTFDrawable , OTFDataReceiver{
	public void onDraw(GL gl);
}

