/******************************************************************************
 *project: org.matsim.*
 * PopulationDensityVisSquare.java
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
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.map.MapPaintCallback;
import playground.rost.graph.GraphAlgorithms;

public class PopulationDensityVisSquare extends JPanel implements MapPaintCallback {
	
	protected int drawWidth;
	protected int drawHeight;
		
	public static int borderSize = 4;
	public static int infoHeight = 20;
	
	protected int pplPerKM2;
	
	protected BasicMap map;
	
	protected int maxPpl;
	
	public PopulationDensityVisSquare(int maxPpl, int drawWidth, int drawHeight)
	{
		this.drawWidth = drawWidth;
		this.drawHeight = drawHeight;
		this.maxPpl = maxPpl;
		this.setPreferredSize(new Dimension(drawWidth + borderSize, drawHeight+borderSize + infoHeight ));
	}
	
	@Override
	public void paintComponent( Graphics g )
	{
		if(map == null)
			return;
		this.setPreferredSize(new Dimension(drawWidth + borderSize , drawHeight+borderSize + infoHeight));
		g.clearRect(0, 0, this.getWidth(), this.getHeight());
		//get area of sample square
		int width = drawWidth;
		int height = drawHeight;
		
		double dX = map.getX(width) - map.getX(0);
		double dY = map.getX(height) - map.getX(0);
		dX *= GraphAlgorithms.dX;
		dY *= GraphAlgorithms.dY;
		
		double Area = dX*dY;
		int pplInSquare = (int) (Area * pplPerKM2);
		Color c = PopulationDensityColorGradient.getColor(pplPerKM2, maxPpl);
		
		g.setColor(c);
		g.fillRect(borderSize/2, borderSize / 2, drawWidth, drawHeight);
		
		g.setColor(Color.black);
		g.drawString("" + pplInSquare, borderSize/2 + 5, drawHeight /2);
		g.setColor(Color.black);
		g.drawString("ppl / km�:" + pplPerKM2, borderSize/2 + 5, drawHeight + borderSize + infoHeight / 2);

	}
	
	public void paint(BasicMap map, Graphics g)
	{
		this.map = map;
		this.repaint();
	}
	
	
	public void setPplPerKM2(int pplPerKM2)
	{
		this.pplPerKM2 = pplPerKM2;
		this.repaint();
	}
}
