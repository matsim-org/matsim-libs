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

package org.matsim.contrib.evacuation.analysis.control.vis;

import java.awt.Color;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.evacuation.analysis.control.Clusterizer;
import org.matsim.contrib.evacuation.analysis.data.AttributeData;
import org.matsim.contrib.evacuation.analysis.data.Cell;
import org.matsim.contrib.evacuation.analysis.data.ColorationMode;
import org.matsim.contrib.evacuation.analysis.data.EventData;
import org.matsim.contrib.evacuation.model.Constants.Mode;
import org.matsim.core.utils.collections.Tuple;

public class ClearingTimeVisualizer {

	private AttributeData<Color> coloration;
	private EventData data;
	private int k;
	private ColorationMode colorationMode;
	private float cellTransparency;
	private Clusterizer clusterizer;

	public ClearingTimeVisualizer(EventData eventData, Clusterizer clusterizer, int k, ColorationMode colorationMode, float cellTransparency) {
		this.data = eventData;
		this.colorationMode = colorationMode;
		this.clusterizer = clusterizer;
		this.k = k;
		this.cellTransparency = cellTransparency;
		processVisualData();

	}

	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}

	public void processVisualData() {
		LinkedList<Tuple<Id<Cell>, Double>> cellIdsAndTimes = new LinkedList<>();
		LinkedList<Double> cellTimes = new LinkedList<Double>();

		// create new coloration (id <-> color relation)
		coloration = new AttributeData<Color>();

		// get cells
		LinkedList<Cell> cells = data.getCells();
		for (Cell cell : cells) {
			// System.out.println("cellid:" + cell.getId());
			if (!cellTimes.contains(cell.getClearingTime())) {
				cellTimes.add(cell.getClearingTime());
				cellIdsAndTimes.add(new Tuple<Id<Cell>, Double>(cell.getId(), cell.getClearingTime()));
			}

			
		}

	
		// calculate data clusters

		LinkedList<Tuple<Id<Cell>, Double>> clusters = this.clusterizer.getClusters(cellIdsAndTimes, k);
		this.data.updateClusters(Mode.CLEARING, clusters);


		for (Cell cell : cells) {
			double clearingTime = cell.getClearingTime();

			if (clearingTime < clusters.get(0).getSecond()) {
				coloration.setAttribute(cell.getId(), Coloration.getColor(0, colorationMode, cellTransparency));
				continue;
			}
			for (int i = 1; i < k; i++) {
				if ((clearingTime >= clusters.get(i - 1).getSecond()) && clearingTime < clusters.get(i).getSecond()) {
					float ik = (float) i / (float) k;
					coloration.setAttribute(cell.getId(), Coloration.getColor(ik, colorationMode, cellTransparency));
					break;
				}
			}
			if (clearingTime >= clusters.get(k - 1).getSecond())
				coloration.setAttribute(cell.getId(), Coloration.getColor(1, colorationMode, cellTransparency));

		}

	}

	public AttributeData<Color> getColoration() {
		return coloration;
	}

}
