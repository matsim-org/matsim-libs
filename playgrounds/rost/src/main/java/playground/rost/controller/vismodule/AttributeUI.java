/******************************************************************************
 *project: org.matsim.*
 * AttributeUI.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.vismodule;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import playground.rost.controller.vismodule.VisModule.MoveLayerDirection;

public class AttributeUI {
	public JPanel mainPanel;
	public JLabel label;
	public JTextField text;
	protected VisModule vM;
	public final String key;
	
	protected void moveLayer(MoveLayerDirection direction)
	{
		vM.requestMoveLayer(direction);
	}
	
	public AttributeUI(VisModule vM, String key)
	{
		this.vM = vM;
		this.key = key;
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridLayout(0,2));
		label = new JLabel(key);
		mainPanel.add(label);
		text = new JTextField(vM.getAtrributes().get(key));
		mainPanel.add(text);		
	}
	
	public JPanel getPanel()
	{
		return mainPanel;
	}
}
