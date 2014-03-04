/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.dgrether.xvis.control.events;

import java.awt.Container;

import playground.dgrether.xvis.gui.PanelManager.Area;


/**
 * @author dgrether
 *
 */
public class ShowPanelEvent implements ControlEvent {

	private Area area;
	private Container panel;

	public ShowPanelEvent(Container panel, Area area) {
		this.panel = panel;
		this.area = area;
	}
	
	public Area getArea() {
		return area;
	}

	
	public Container getPanel() {
		return panel;
	}



}
