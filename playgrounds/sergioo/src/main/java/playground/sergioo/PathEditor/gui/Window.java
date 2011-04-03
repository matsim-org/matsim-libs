/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.sergioo.PathEditor.gui;

import java.awt.BorderLayout;
import javax.swing.JFrame;

import org.matsim.api.core.v01.network.Network;

import playground.sergioo.GTFS.Trip;

public class Window extends JFrame implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Constants
	public static int WIDTH = 1000;
	public static int HEIGHT = 1000;
	public static int FRAMESIZE = 50;
	//Attributes
	private PanelPathEditor panel;
	//Methods
	public Window(Network network, Trip trip) {
		this.setLocation(0,0);
		this.setSize(WIDTH, HEIGHT);
		this.setLayout(new BorderLayout());
		panel=new PanelPathEditor();
		this.add(panel, BorderLayout.CENTER);
	}
	@Override
	public void run() {
		
	}
}
