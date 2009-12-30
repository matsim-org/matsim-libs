/* *********************************************************************** *
 * project: org.matsim.*
 * OTFDrawer.java
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

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.rmi.RemoteException;

import org.matsim.vis.otfvis.data.OTFClientQuad;


/**
 * OTFDrawer is the base interface for the two implementations of the actual client of the OTFVis.
 * OTFOGLDrawer and JNetComponent( as the Swing variant) rely on this interface. Other drawer classes might follow.
 * 
 * @author dstrippgen
 *
 */
public interface OTFDrawer {
	public void invalidate(int time) throws RemoteException;
	public void redraw();
	public void handleClick(Point2D.Double point, int mouseButton, MouseEvent e);
	public OTFClientQuad getQuad();
	public Component getComponent();
	public void clearCache();
	public void handleClick(Rectangle currentRect, int button);
}
