/******************************************************************************
 *project: org.matsim.*
 * BasicMapGUI.java
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


package playground.rost.controller.gui;

import java.awt.Container;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.VisModuleContainer;

public interface BasicMapGUI {
	public VisModuleContainer getVisModuleContainer();
	
	public BasicMap getMap();
	
	public Container getCustomContainer();
	
	public void buildUI();
	
	public void UIChange();
}
