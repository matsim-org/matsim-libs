/* *********************************************************************** *
 * project: org.matsim.*
 * RoadClosuresEditor.java
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

package playground.wdoering.grips.evacuationanalysis.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;
import playground.wdoering.grips.evacuationanalysis.data.AttributeData;

public class KeyPanel extends AbstractDataPanel {
	
	private ChartPanel chartPanel;
	private Mode mode;


	//inherited field:
	//protected EventData data
	
	//TODO: GRAPH graph;
	
	public KeyPanel(Mode mode, int width, int height)
	{
		this.setPanelSize(width, height);
		this.mode = mode;
		
		drawDataPanel();
	}

	
	@Override
	public void drawDataPanel()
	{
		
		if (this.data!=null)
			return;
		
		
		String class1Val = "", class2Val = "", class3Val = "", class4Val = "", class5Val = "";
		Color class1Color, class2Color, class3Color, class4Color, class5Color;
		class1Color = class2Color = class3Color = class4Color = class5Color = Color.white;
		
		for (int i = 1; i < 6; i++)
		{
			if (mode.equals(Mode.EVACUATION))
			{
				
				
			}
		}
		
		JPanel keyPanel = new JPanel(new GridBagLayout());
		
		keyPanel.setSize(this.width, this.height);
		keyPanel.setPreferredSize(new Dimension(this.width,this.height));
		
		JLabel class1Label = new JLabel(" "); class1Label.setOpaque(true); class1Label.setBackground(class1Color); class1Label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class2Label = new JLabel(" "); class2Label.setOpaque(true); class2Label.setBackground(class2Color); class2Label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class3Label = new JLabel(" "); class3Label.setOpaque(true); class3Label.setBackground(class3Color); class3Label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class4Label = new JLabel(" "); class4Label.setOpaque(true); class4Label.setBackground(class4Color); class4Label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class5Label = new JLabel(" "); class5Label.setOpaque(true); class5Label.setBackground(class5Color); class5Label.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		
		JLabel class1 = new JLabel(class1Val); class1.setOpaque(true);class1.setBackground(Color.WHITE); class1.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class2 = new JLabel(class2Val); class2.setOpaque(true); class2.setBackground(Color.WHITE); class2.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class3 = new JLabel(class3Val); class3.setOpaque(true); class3.setBackground(Color.WHITE); class3.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class4 = new JLabel(class4Val); class4.setOpaque(true);  class4.setBackground(Color.WHITE); class4.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JLabel class5 = new JLabel(class5Val); class5.setOpaque(true); class5.setBackground(Color.WHITE); class5.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.3; c.gridx = 0; c.gridy = 0;
		keyPanel.add(class1Label, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.7; c.gridwidth = 2; c.gridx = 1; c.gridy = 0;
		keyPanel.add(class1, c);
		
		//2. klasse
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.3; c.gridx = 0; c.gridy = 1;
		keyPanel.add(class2Label, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.7; c.gridwidth = 2; c.gridx = 1; c.gridy = 1;
		keyPanel.add(class2, c);
		
		//3. klasse
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.3; c.gridx = 0; c.gridy = 2;
		keyPanel.add(class3Label, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.7; c.gridwidth = 2; c.gridx = 1; c.gridy = 2;
		keyPanel.add(class3, c);
		
		//4. klasse
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.3; c.gridx = 0; c.gridy = 3;
		keyPanel.add(class4Label, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.7; c.gridwidth = 2; c.gridx = 1; c.gridy = 3;
		keyPanel.add(class4, c);
		
		//5. klasse
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.3; c.gridx = 0; c.gridy = 4;
		keyPanel.add(class5Label, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.7; c.gridwidth = 2; c.gridx = 1; c.gridy = 4;
		keyPanel.add(class5, c);
		
		this.add(keyPanel);
		this.validate();
		this.setSize(this.width, this.height);
		this.setPreferredSize(new Dimension(this.width,this.height));

	}

}
