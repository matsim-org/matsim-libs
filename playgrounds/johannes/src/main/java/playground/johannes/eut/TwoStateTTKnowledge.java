/* *********************************************************************** *
 * project: org.matsim.*
 * TwoStateTTKnowledge.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.johannes.eut;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class TwoStateTTKnowledge extends TravelTimeMemory {

	private List<Integer> n_samples = new ArrayList<Integer>();

	private int sampleSum;

	public TwoStateTTKnowledge() {
		setMaxMemorySlots(2);
	}

	@Override
	public void appendNewStorage(TimevariantTTStorage storage) {
		this.sampleSum++;
		if(getStorageList().size() < getMaxMemorySlots()) {
			getStorageList().add(storage);
			this.n_samples.add(1);
		} else {
			double avr = storage.getAverage();
			double min_diff = Double.MAX_VALUE;
			int slotIdx = -1;
			for(int i = 0; i < getStorageList().size(); i++) {
				double diff = Math.abs(getStorageList().get(i).getAverage() - avr);
				if(diff < min_diff) {
					slotIdx = i;
					min_diff = diff;
				}
			}
			getStorageList().get(slotIdx).accumulate(storage, getLearningRate());
			this.n_samples.set(slotIdx, this.n_samples.get(slotIdx) + 1);
		}
	}

	public double getWeigth(int state) {
		return this.n_samples.get(state)/(double)this.sampleSum;
	}

	@Override
	public TravelTime getMeanTravelTimes() {
		return new MeanLinkCost(getStorageList());
	}

	private class MeanLinkCost implements TravelTime {

		private List<TimevariantTTStorage> linkcosts;

		public MeanLinkCost(List<TimevariantTTStorage> linkcosts) {
			this.linkcosts = linkcosts;
		}

		public double getLinkTravelTime(Link link, double time) {
			double sum = 0;
			for(int i = 0; i < this.linkcosts.size(); i++)
				sum += this.linkcosts.get(i).getLinkTravelTime(link, time) * getWeigth(i);

			return sum;
		}

	}
}
