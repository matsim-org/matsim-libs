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
package playground.dgrether.xvis.control;

import playground.dgrether.xvis.gui.DrawingPreferences;
import playground.dgrether.xvis.gui.SwingLayoutPreferences;


/**
 * @author dgrether
 *
 */
public class XVisControl {

	private static XVisControl instance;
	
	private SwingLayoutPreferences layoutPreferences = new SwingLayoutPreferences();
	
	private DrawingPreferences drawingPreferences = new DrawingPreferences();
	
	private ControlEventsManager eventsManager = new ControlEventsManager();
	
	private XVisControl() {
	}
	
	public static void init(){
		instance = new XVisControl();
	}
	
	public static synchronized XVisControl getInstance(){
		if (instance == null){
			init();
		}
		return instance;
	}

	public void shutdownVisualizer() {
		System.exit(0);
	}

	
	public SwingLayoutPreferences getLayoutPreferences() {
		return layoutPreferences;
	}

	
	public DrawingPreferences getDrawingPreferences() {
		return drawingPreferences;
	}

	public ControlEventsManager getControlEventsManager(){
		return this.eventsManager;
	}
	
}
