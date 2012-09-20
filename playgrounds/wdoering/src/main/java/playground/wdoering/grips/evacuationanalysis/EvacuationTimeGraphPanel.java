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
		
	}

}
