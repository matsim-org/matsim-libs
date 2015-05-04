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

package others.sergioo.confidenceEllipses2014.gui;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.JFrame;

import others.sergioo.confidenceEllipses2014.kernel.ConfidenceEllipsesCalculator.PersonEllipse;
import others.sergioo.visUtils.PointLines;

public class WindowEllipse extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//Constants
	public static int MAX_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
	public static int MAX_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
	public static int FRAMESIZE = 100;
	//Attributes
	public static int width;
	public static int height;
	private PanelPointLines panel;
	//Methods
	public WindowEllipse(PointLines pointLines) {
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		panel=new PanelPointLines(pointLines, ((PersonEllipse)pointLines).getEllipses());
		this.setSize(width, height);
		this.add(panel, BorderLayout.CENTER);
	}
	public WindowEllipse(PointLines pointLines, String title) {
		setTitle(title);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setLocation(0,0);
		this.setLayout(new BorderLayout());
		panel=new PanelPointLines(pointLines, ((PersonEllipse)pointLines).getEllipses(), title);
		this.setSize(width, height);
		this.add(panel, BorderLayout.CENTER);
	}
}
