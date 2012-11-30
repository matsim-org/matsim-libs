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
import java.util.LinkedList;
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
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
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
		
		if (this.data==null)
			return;
		
		LinkedList<Tuple<Id,Double>> clusters = this.data.getClusters(mode);
		int k = clusters.size();
		
		String [] classVal = new String[k];
		Color [] classColor = new Color[k];
		
		JLabel [] colorLabels = new JLabel[k];
		JLabel [] valueLabels = new JLabel[k];
		
		for (int i = 0; i < k; i++)
		{
			if (mode.equals(Mode.EVACUATION))
			{
				classColor[i] = this.data.getEvacuationTimeVisData().getAttribute((IdImpl)clusters.get(i).getFirst());
				classVal[i] = ""+clusters.get(i).getSecond();
			}
			else if (mode.equals(Mode.CLEARING))
			{
				classColor[i] = this.data.getClearingTimeVisData().getAttribute((IdImpl)clusters.get(i).getFirst());
				classVal[i] = ""+clusters.get(i).getSecond();
			}
			else 
			{
				classColor[i] = this.data.getLinkUtilizationVisData().getAttribute((IdImpl)clusters.get(i).getFirst()).getSecond();
				classVal[i] = ""+clusters.get(i).getSecond();
			}
			
			System.out.println("val:");
			System.out.println(clusters.get(i).getFirst() + ":" + clusters.get(i).getSecond());
			
		}
		
		JPanel keyPanel = new JPanel(new GridBagLayout());
		
		keyPanel.setSize(this.width, this.height);
		keyPanel.setPreferredSize(new Dimension(this.width,this.height));
		
		GridBagConstraints c = new GridBagConstraints();
		
		for (int i = 0; i < k; i++)
		{
			colorLabels[i] = new JLabel(" "); colorLabels[i].setOpaque(true); colorLabels[i].setBackground(classColor[i]); colorLabels[i].setBorder(BorderFactory.createLineBorder(Color.black, 1));
			valueLabels[i] = new JLabel(classVal[i]); valueLabels[i].setOpaque(true); valueLabels[i].setBackground(Color.WHITE); valueLabels[i].setBorder(BorderFactory.createLineBorder(Color.black, 1));
			
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0.3; c.gridx = 0; c.gridy = i;
			keyPanel.add(colorLabels[i], c);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 0.7; c.gridwidth = 2; c.gridx = 1; c.gridy = i;
			keyPanel.add(valueLabels[i], c);
		}
		
		this.add(keyPanel);
		this.validate();
		this.setSize(this.width, this.height);
		this.setPreferredSize(new Dimension(this.width,this.height));

	}


	public void setMode(Mode mode)
	{
		this.mode = mode;
		drawDataPanel();
	}

}
