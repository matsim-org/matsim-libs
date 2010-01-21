/* *********************************************************************** *
 * project: org.matsim.*
 * OTFQueryHandler.java
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

package org.matsim.vis.otfvis.interfaces;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D.Double;

import org.matsim.vis.otfvis.opengl.queries.AbstractQuery;

/**
 * OTFQueryHandler is the interface to handle Queries through on the client side
 * It is implemented in the OTFQueryContrlbar.
 * 
 * @author dstrippgen
 *
 */
public interface OTFQueryHandler {
	
	public void removeQueries();
	
	public OTFQueryRemote doQuery(final AbstractQuery query);
	
	public void handleIdQuery(String id, String query);
	
	public void updateQueries();
	
	public void drawQueries(OTFDrawer drawer);
	
	public void handleClick(String id, Double point, int mouseButton);
	
	public void handleClick(String id, Rectangle2D.Double origRect, int button);

}
