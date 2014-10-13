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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis;
import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;
import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Unit;

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
		else
			this.removeAll();
		
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
				classColor[i] =this.data.getEvacuationTimeVisData().getAttribute(clusters.get(i).getFirst());
				
				classVal[i] =  EvacuationAnalysis.getReadableTime(clusters.get(i).getSecond(), Unit.TIME);
			}
			else if (mode.equals(Mode.CLEARING))
			{
				classColor[i] = this.data.getClearingTimeVisData().getAttribute(clusters.get(i).getFirst());
				classVal[i] = EvacuationAnalysis.getReadableTime(clusters.get(i).getSecond(), Unit.TIME);
			}
			else 
			{
				classColor[i] = this.data.getLinkUtilizationVisData().getAttribute(clusters.get(i).getFirst()).getSecond();
				classVal[i] = EvacuationAnalysis.getReadableTime(clusters.get(i).getSecond(), Unit.PEOPLE);
			}
			
//			System.out.println("val:");
//			System.out.println(clusters.get(i).getFirst() + ":" + clusters.get(i).getSecond());
			
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
		
		this.add(new JLabel(""+mode));
		this.add(keyPanel);
		this.validate();
		this.setSize(this.width, this.height);
		this.setPreferredSize(new Dimension(this.width,this	.height));

	}


	public void setMode(Mode mode)
	{
		this.mode = mode;
		drawDataPanel();
	}
	


}
