/* *********************************************************************** *
 * project: org.matsim.*
 * GUI.java
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

package playground.christoph.oldenburg;

import javax.swing.JFrame;

public class GUI extends JFrame {

	private static final long serialVersionUID = 1L;

	public GUI() {
		super();
		init();
	}
	
	private void init() {
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Sioux Falls Evacuation - Summer School Oldenburg");
		this.setContentPane(new ConfigPanel());
		this.setVisible(true);
		this.setSize(800, 600);
		
//	    try {
//	        UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//	        UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
//	        UIManager.setLookAndFeel("javax.swing.plaf.mac.MacLookAndFeel");
//	        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");  
//	        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
//	    } catch (Exception e) { }
	}
	
	public static void main(String args[]) {
		if (args.length == 0) System.exit(0);
		
		DemoConfig.configFile = args[0];
		new GUI();
	}
}
