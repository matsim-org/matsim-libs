/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.interfaces;

import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;


public interface OTFQueryResult {
	
	/**
	 * Everytime the display needs to be refreshed this 
	 * method is called for every active Query.
	 */
	public void draw(OTFOGLDrawer drawer);

	/**
	 * Remove is called when a query is removed, to give the query the option to
	 * cleanup things.
	 * 
	 */
	public void remove();
	
	/**
	 * As long as this returns true, the query will be called every time step.
	 * 
	 * @return boolean indicated if query needs updating
	 */
	public boolean isAlive();
	
}
