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
import java.util.HashMap;

import javax.swing.JPanel;

import org.matsim.core.utils.collections.QuadTree;

public class GraphPanel extends JPanel {
	
	private QuadTree<Cell> cellTree;
	private double cellSize;
	private HashMap<MetaData, Object> data;
	
	public GraphPanel()
	{
		this.setBackground(Color.blue);
		this.setPreferredSize(new Dimension(300,300));
	}

	public void updateData(QuadTree<Cell> cellTree, HashMap<MetaData, Object> data)
	{
		this.cellTree = cellTree;
		this.data = data;
		this.cellSize = (Double) data.get(MetaData.CELLSIZE);
	}

}
