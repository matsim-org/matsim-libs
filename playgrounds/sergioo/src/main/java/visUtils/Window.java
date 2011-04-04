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

package visUtils;

import java.awt.BorderLayout;
import javax.swing.JFrame;

public class Window extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Constants
	public static int MAX_WIDTH = 1920;
	public static int MAX_HEIGHT = 1080;
	public static int FRAMESIZE = 50;
	//Attributes
	public static int width;
	public static int height;
	private PanelPointLines panel;
	//Methods
	public Window(PointLines pointLines) {
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		panel=new PanelPointLines(pointLines);
		this.setSize(width, height);
		this.add(panel, BorderLayout.CENTER);
	}
}
