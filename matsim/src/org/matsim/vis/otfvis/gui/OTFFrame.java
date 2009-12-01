/* *********************************************************************** *
 * project: org.matsim.*
 * OTFFrame
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.otfvis.gui;

import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;

import org.matsim.vis.otfvis.opengl.OTFClientFile;


/**
 * @author dgrether
 *
 */
public class OTFFrame extends JFrame {

	private JSplitPane pane;

	public OTFFrame(String title, boolean isMac) {
		super(title);
		this.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ); 		
		JFrame.setDefaultLookAndFeelDecorated(true);
		if (isMac){
			this.getRootPane().putClientProperty("apple.awt.brushMetalLook", Boolean.TRUE);
		}
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		pane.setContinuousLayout(true);
		pane.setOneTouchExpandable(true);
		this.getContentPane().add(pane);
		this.pane = pane;
		//Make sure menus appear above JOGL Layer
		JPopupMenu.setDefaultLightWeightPopupEnabled(false); 

	}
	
	public  JSplitPane getSplitPane(){
		return this.pane;
	}
	
	@Override
	protected void processWindowEvent(WindowEvent e) {

	        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
	        	OTFClientFile.endProgram(0);
	        } else {
		        super.processWindowEvent(e);
	        }
	    }

	
}
