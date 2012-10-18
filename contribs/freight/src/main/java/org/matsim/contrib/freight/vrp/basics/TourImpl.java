/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.vrp.basics;

import java.util.LinkedList;

/**
 * 
 * @author stefan schroeder
 * 
 */

public class TourImpl implements Tour {

	public static class TourData {
		public double transportTime;
		public double transportCosts;
		public int totalLoad;
		public double totalCost;

		public void reset() {
			transportTime = 0.0;
			transportCosts = 0.0;
			totalLoad = 0;
			totalCost = 0.0;
		}

	}

	private final LinkedList<TourActivity> tourActivities = new LinkedList<TourActivity>();

	public TourData tourData = new TourData();

	public TourImpl(TourImpl tour2copy) {
		for (TourActivity tourAct : tour2copy.getActivities()) {
			this.tourActivities.add(tourAct.duplicate());
		}
		this.tourData.transportCosts = tour2copy.tourData.transportCosts;
		this.tourData.transportTime = tour2copy.tourData.transportTime;
		this.tourData.totalLoad = tour2copy.tourData.totalLoad;
		this.tourData.totalCost = tour2copy.tourData.totalCost;
	}

	public TourImpl() {
		super();
	}

	@Override
	public LinkedList<TourActivity> getActivities() {
		return tourActivities;
	}

	public boolean isEmpty() {
		return (tourActivities.size() <= 2);
	}

	@Override
	public String toString() {
		String tour = "";
		for (TourActivity c : tourActivities) {
			tour += "[" + c.getClass().getSimpleName() + "@"
					+ c.getLocationId() + "@"
					+ c.getEarliestOperationStartTime() + "-"
					+ c.getLatestOperationStartTime() + "]";
		}
		tour += "[transportTime=" + tourData.transportTime
				+ "][transportCosts=" + tourData.transportCosts + "]";
		return tour;
	}

}
