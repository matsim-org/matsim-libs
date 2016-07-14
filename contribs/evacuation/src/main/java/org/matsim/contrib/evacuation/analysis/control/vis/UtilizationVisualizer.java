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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.evacuation.analysis.control.Clusterizer;
import org.matsim.contrib.evacuation.analysis.data.AttributeData;
import org.matsim.contrib.evacuation.analysis.data.ColorationMode;
import org.matsim.contrib.evacuation.analysis.data.EventData;
import org.matsim.contrib.evacuation.model.Constants.Mode;
import org.matsim.core.utils.collections.Tuple;

public class UtilizationVisualizer {

	private AttributeData<Tuple<Float, Color>> coloration;
	private final List<Link> links;
	private final EventData data;
	private final Clusterizer clusterizer;
	private final int k;
	private ColorationMode colorationMode;
	private final float cellTransparency;

	public UtilizationVisualizer(List<Link> links, EventData eventData, Clusterizer clusterizer, int k, ColorationMode colorationMode, float cellTransparency) {
		this.links = links;
		this.data = eventData;
		this.clusterizer = clusterizer;
		this.k = k;
		this.colorationMode = colorationMode;
		this.cellTransparency = cellTransparency;
		processVisualData();

	}

	public void setColorationMode(ColorationMode colorationMode) {
		this.colorationMode = colorationMode;
	}

	public void processVisualData() {
		LinkedList<Tuple<Id<Link>, Double>> linkTimes = new LinkedList<>();

		this.coloration = new AttributeData<Tuple<Float, Color>>();

		HashMap<Id<Link>, List<Tuple<Id<Person>, Double>>> linkLeaveTimes = this.data.getLinkLeaveTimes();
		HashMap<Id<Link>, List<Tuple<Id<Person>, Double>>> linkEnterTimes = this.data.getLinkEnterTimes();

		for (Link link : this.links) {
			List<Tuple<Id<Person>, Double>> leaveTimes = linkLeaveTimes.get(link.getId());
			List<Tuple<Id<Person>, Double>> enterTimes = linkEnterTimes.get(link.getId());

			if ((enterTimes != null) && (enterTimes.size() > 0) && (leaveTimes != null)) {

//				if (!linkTimes.contains(enterTimes.size())) {
					linkTimes.add(new Tuple<Id<Link>, Double>(link.getId(), (double) leaveTimes.size()));
//				}

			}
		}

		LinkedList<Tuple<Id<Link>, Double>> clusters = this.clusterizer.getClusters(linkTimes, this.k);

		// calculate data clusters
		this.data.updateClusters(Mode.UTILIZATION, clusters);


		// assign clusterized colors to all link ids
		for (Link link : this.links) {
			List<Tuple<Id<Person>, Double>> enterTimes = linkEnterTimes.get(link.getId());

			if ((enterTimes != null) && (enterTimes.size() > 0)) {
				double enterTime = enterTimes.size();

				if (enterTime < clusters.get(0).getSecond()) {
					this.coloration.setAttribute(link.getId(), new Tuple<Float, Color>(0f, Coloration.getColor(0, this.colorationMode, this.cellTransparency)));
					continue;
				}
				for (int i = 1; i < this.k; i++) {
					if ((enterTime >= clusters.get(i - 1).getSecond()) && enterTime < clusters.get(i).getSecond()) {
						float ik = (float) i / (float) this.k;
						this.coloration.setAttribute(link.getId(), new Tuple<Float, Color>(ik, Coloration.getColor(ik, this.colorationMode, this.cellTransparency)));
						break;
					}
				}
				if (enterTime >= clusters.get(this.k - 1).getSecond())
					this.coloration.setAttribute(link.getId(), new Tuple<Float, Color>(1f, Coloration.getColor(1f, this.colorationMode, this.cellTransparency)));
			}
		}

	}

	public AttributeData<Tuple<Float, Color>> getColoration() {
		return this.coloration;
	}

}
