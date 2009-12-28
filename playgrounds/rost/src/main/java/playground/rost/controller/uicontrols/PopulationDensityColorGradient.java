/******************************************************************************
 *project: org.matsim.*
 * PopulationDensityColorGradient.java
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


package playground.rost.controller.uicontrols;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class PopulationDensityColorGradient extends JPanel {

	protected int max;
	protected int height;
	protected static final int border = 7;
	
	public PopulationDensityColorGradient(int max, int height)
	{
		this.max = max;
		this.height = height;
	}
	
	@Override
	public void paintComponent( Graphics g )
	{
		int width = this.getWidth() - border;;
		for(int x = border; x < width; ++x)
		{
			//calc ppl
			int ppl = (int)(((double)x/(double)width)*max);
			Color c = getColor(ppl, max);
			g.setColor(c);
			g.fillRect(x, 0, 1, height);
		}
	}
	
	public void setPreferredSize(int width, int height)
	{
		this.setPreferredSize(width, height);
	}
	
	public static Color getColor(int value, int max)
	{
		if(value > max)
			throw new RuntimeException("Color out of bounds!");
		value = (int)((767)*((double)value / (double)max));
		if(value < 256)
		{
			return new Color(value % 256, 255,0);
		}
		else if(value < 512)
		{
			return new Color(255,255-value%256, 0);
		}
		else return new Color(255-value%256,0,0);
	}
}
