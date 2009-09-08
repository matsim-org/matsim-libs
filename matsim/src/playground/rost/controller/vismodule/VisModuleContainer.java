/******************************************************************************
 *project: org.matsim.*
 * VisModuleContainer.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.vismodule;

import java.awt.Container;
import java.awt.Graphics;
import java.util.List;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.map.MapPaintCallback;
import playground.rost.controller.vismodule.VisModule.MoveLayerDirection;

public interface VisModuleContainer extends MapPaintCallback {
	
	public void addVisModule(VisModule vM);
	
	public void removeVisModule(VisModule vM);
	
	public List<VisModule> getVisModuleOrder();
	
	public void setVisModuleOrder(List<VisModule> newVMOrder);
	
	public void paintMap(BasicMap map, Graphics g);
	
	public Container getContainer();
	
	public void requestMoveLayer(VisModule vM, MoveLayerDirection direction);
}
