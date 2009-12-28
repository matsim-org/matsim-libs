/******************************************************************************
 *project: org.matsim.*
 * PopulationDensity.java
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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import playground.rost.controller.gui.BasicMapGUI;
import playground.rost.controller.map.BasicMap;
import playground.rost.controller.map.MapPaintCallback;
 
public class PopulationDensity extends JPanel implements MapPaintCallback {

	protected JSlider slider;
	protected PopulationDensityVisSquare visSquare;
	
	protected static final int defaultDensity = 10000;
	
	protected BasicMapGUI mapGUI;
	
	protected int maxDensity;
	
	public PopulationDensity(BasicMapGUI mapGUI, int maxDensity)
	{
		realCtor(mapGUI, maxDensity);
	}
	
	public PopulationDensity(BasicMapGUI mapGUI)
	{
		realCtor(mapGUI, defaultDensity);
	}
	
	protected void realCtor(BasicMapGUI mapGUI, int maxDensity)
	{
		this.mapGUI = mapGUI;
		this.maxDensity = maxDensity;
				
		buildUI();
		

		this.addComponentListener(new ComponentAdapter() 
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				setSliderAttributes();
			}
		});

	}
	
	protected void buildUI()
	{
		slider = new JSlider();
		setSliderAttributes();
		
		visSquare = new PopulationDensityVisSquare(maxDensity, 50, 50);
		this.setLayout(new BorderLayout());
		
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
		sliderPanel.add(slider);
		PopulationDensityColorGradient cGradient = new PopulationDensityColorGradient(this.maxDensity, 26);
		sliderPanel.add(cGradient);
		
		this.add(sliderPanel, BorderLayout.CENTER);
		this.add(visSquare, BorderLayout.WEST);
		
		slider.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e) {
				if(!slider.getValueIsAdjusting())
					sliderChanged();
			}});
	}
	
	public void sliderChanged()
	{
		visSquare.setPplPerKM2(slider.getValue());
		mapGUI.UIChange();
	}
	
	protected void setSliderAttributes()
	{
		slider.setMaximum(maxDensity);
		int sliderWidth = slider.getWidth();
		if(sliderWidth == 0)
			return;
		slider.setMajorTickSpacing(1000);
		slider.setMinorTickSpacing(200);
		slider.setPaintTicks(true);
		slider.setPaintLabels(false);
	}
	
	public void paint(BasicMap map, Graphics g)
	{
		visSquare.paint(map, g);
	}
	
	public int getDensity()
	{
		return slider.getValue();
	}
}
