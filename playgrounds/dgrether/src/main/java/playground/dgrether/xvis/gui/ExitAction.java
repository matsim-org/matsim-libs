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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import playground.dgrether.xvis.control.XVisControl;


public class ExitAction extends AbstractAction {
	
	public ExitAction(){
		super("Exit");
		KeyStroke stroke = KeyStroke.getKeyStroke(
				KeyEvent.VK_X, ActionEvent.ALT_MASK);
		putValue(MNEMONIC_KEY, stroke.getKeyCode());
	
	}
	
	public void actionPerformed(ActionEvent e) {
		XVisControl.getInstance().shutdownVisualizer();
	}
}
