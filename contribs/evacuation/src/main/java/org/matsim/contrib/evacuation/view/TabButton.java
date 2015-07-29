/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package org.matsim.contrib.evacuation.view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.matsim.contrib.evacuation.model.Constants.ModuleType;

public class TabButton extends JButton
{
	private static final long serialVersionUID = 1L;
	private ModuleType moduleType;
	private JPanel backgroundPanel;
	
	
	private Color color = Color.white;  
	private Color hoverColor = Color.white;  

	public TabButton(ModuleType moduleType, JPanel backgroundPanel, int width, int height)
	{
		super("");
		
		this.moduleType = moduleType;
		this.backgroundPanel = backgroundPanel;
		
		this.setForeground(Color.BLACK);
		this.setBackground(Color.WHITE);
		this.setBorder(null);

		this.setPreferredSize(new Dimension(width, height));
		
	}
	
	public Color getHoverColor()
	{
		return hoverColor;
	}
	
	public void setHoverColor(Color hoverColor)
	{
		this.hoverColor = hoverColor;
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
		this.getBackgroundPanel().setBackground(color);
	}
	
	public JPanel getBackgroundPanel()
	{
		return backgroundPanel;
	}
	
	public ModuleType getModuleType()
	{
		return moduleType;
	}

	public void hover(boolean toggle)
	{
		if (this.isEnabled())
		{
			if (toggle)
				this.getBackgroundPanel().setBackground(hoverColor);
			else
				this.getBackgroundPanel().setBackground(color);
		}
		
	}
	
	@Override
	public void setEnabled(boolean b)
	{
		super.setEnabled(b);
		
		if (!b)
			this.getBackgroundPanel().setBackground(Color.gray);
	}

}
