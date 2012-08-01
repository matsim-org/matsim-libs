/* *********************************************************************** *
 * project: org.matsim.*
 * GuiDebuggerFrame.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.sim2d_v3.helper.guidebugger;

import java.awt.BorderLayout;

import javax.swing.JFrame;

public class GuiDebuggerFrame extends JFrame {
	
	
	public GuiDebuggerFrame() {
		setSize(1000, 1000);
		GuiDebugger dbg = new GuiDebugger();
		add(dbg,BorderLayout.CENTER);
		dbg.init();
	}

}
