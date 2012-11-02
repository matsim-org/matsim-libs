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

import org.matsim.vis.otfvis.caching.SceneGraph;


/**
 * OTFGLDrawableImpl is a basic implementation of the OTFOGLDrawable interface with an
 * additional simple invalidation mechanism.
 *
 * @author dstrippgen
 *
 * <p>
 * there was an "isValid" but it was not used.  kai, jan'10/feb'11
 * </p>
 *
 */
public abstract class OTFGLAbstractDrawableReceiver extends OTFGLAbstractDrawable {

	public abstract void addToSceneGraph(SceneGraph graph);

}