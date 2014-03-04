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
package playground.dgrether.xvis.gui;

import java.awt.event.WindowEvent;

import org.apache.log4j.Logger;

import playground.dgrether.xvis.control.XVisControl;


/**
 * @author dgrether
 *
 */
public class MainFrameWindowListener implements java.awt.event.WindowListener {
	
	private static final Logger log = Logger.getLogger(MainFrameWindowListener.class);
	
	public MainFrameWindowListener() {	}

	public void windowOpened(WindowEvent e) {
		
	}

	public void windowClosing(WindowEvent e) {
		log.info("closing");
		XVisControl.getInstance().shutdownVisualizer();
	
	}

	public void windowClosed(WindowEvent e) {
		log.info("closed");
	}

	public void windowIconified(WindowEvent e) {
		
	}

	public void windowDeiconified(WindowEvent e) {
		
	}

	public void windowActivated(WindowEvent e) {
		
	}

	public void windowDeactivated(WindowEvent e) {
		
	}

}
