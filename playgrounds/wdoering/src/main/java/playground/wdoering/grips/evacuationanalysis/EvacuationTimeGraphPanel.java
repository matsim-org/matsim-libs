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

package playground.wdoering.grips.evacuationanalysis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.collections.Tuple;

public class EvacuationTimeGraphPanel extends AbstractGraphPanel {
	
	//inherited field:
	//protected EventData data
	
	//TODO: GRAPH graph;
	
	public EvacuationTimeGraphPanel()
	{
		this.setBackground(Color.blue);
		this.setPreferredSize(new Dimension(300,300));
		drawGraph();
	}
	
	@Override
	public void drawGraph()
	{
		//TODO: Graphen zeichnen / aktualisieren
		
		//if data is not set yet: do nothing
		if (data==null)
			return;
		
		System.out.println("EVACUATION TIME GRAPH");
		System.out.println("cell size:" + data.getCellSize());
		System.out.println("time sum:" + data.getTimeSum());
		System.out.println("arrivals:" + data.getArrivals());
		
		List<Tuple<Double, Double>> xyData;
		
		XYLineChart chart = new XYLineChart("Evakuierungszeit", "x", "y");
		
		double [] xs = new double[10];
		double [] ys = new double[10];
		
		int pos = 0;
		for (int i = 0; i < 10; i++)
		{

			xs[i] = i;
			ys[i] = i*i;
		}
		
		JFreeChart freeChart = chart.getChart();
		
		Graphics g = this.getGraphics();
		Graphics2D g2d = (Graphics2D)g;
		
		freeChart.draw(g2d, new Rectangle2D.Double(0,0,200,200));
		
		
	}

}
