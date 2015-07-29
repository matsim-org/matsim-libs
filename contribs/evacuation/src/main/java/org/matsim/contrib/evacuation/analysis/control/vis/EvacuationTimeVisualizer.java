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

public class EvacuationTimeVisualizer {

	private AttributeData<Color> coloration;
	private EventData data;
	private Clusterizer clusterizer;
	private int k;
	private ColorationMode colorationMode;
	private float cellTransparency;

	public EvacuationTimeVisualizer(EventData eventData, Clusterizer clusterizer, int k, ColorationMode colorationMode, float cellTransparency) {
		this.data = eventData;
		this.cellTransparency = cellTransparency;
		this.clusterizer = clusterizer;
		this.k = k;
		this.colorationMode = colorationMode;
		processVisualData();
	}

	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}

	public void processVisualData() {
		LinkedList<Tuple<Id<Cell>, Double>> cellIdsAndTimes = new LinkedList<>();
		LinkedList<Double> cellTimes = new LinkedList<Double>();
		this.coloration = new AttributeData<Color>();

		LinkedList<Cell> cells = data.getCells();

		for (Cell cell : cells) {
			if (!cellTimes.contains(cell.getMedianArrivalTime())) {
				cellTimes.add(cell.getMedianArrivalTime());
				cellIdsAndTimes.add(new Tuple<Id<Cell>, Double>(cell.getId(), cell.getMedianArrivalTime()));
			}
		}

		// calculate data clusters
		LinkedList<Tuple<Id<Cell>, Double>> clusters = this.clusterizer.getClusters(cellIdsAndTimes, k);
		this.data.updateClusters(Mode.EVACUATION, clusters);

		LinkedList<Double> clusterValues = new LinkedList<Double>();
		for (Tuple<Id<Cell>, Double> cluster : clusters)
			clusterValues.add(cluster.getSecond());

		for (Cell cell : cells) {
			double arrivalTime = cell.getMedianArrivalTime();

			if (arrivalTime < clusterValues.get(0)) {
				coloration.setAttribute(cell.getId(), Coloration.getColor(0, colorationMode, cellTransparency));
				continue;
			}
			for (int i = 1; i < k; i++) {
				if ((arrivalTime >= clusterValues.get(i - 1)) && arrivalTime < clusterValues.get(i)) {
					float ik = (float) i / (float) k;
					coloration.setAttribute(cell.getId(), Coloration.getColor(ik, colorationMode, cellTransparency));
					break;
				}
			}
			if (arrivalTime >= clusterValues.get(k - 1))
				coloration.setAttribute(cell.getId(), Coloration.getColor(1, colorationMode, cellTransparency));

		}

	}

	public AttributeData<Color> getColoration() {
		return coloration;
	}

}
