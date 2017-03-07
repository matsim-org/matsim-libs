/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.taxibus.algorithm.scheduler.vehreqpath;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.DrtRequest;
import org.matsim.contrib.drt.DrtRequest.DrtRequestStatus;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

public class TaxibusDispatch {
	public final Vehicle vehicle;
	public final Set<DrtRequest> requests;
	public final ArrayList<VrpPathWithTravelData> path;

	private double earliestNextDeparture = 0;

	double twMax;

	public TaxibusDispatch(Vehicle vehicle, DrtRequest request, VrpPathWithTravelData path) {
		this.requests = new LinkedHashSet<>();
		this.path = new ArrayList<>();
		this.requests.add(request);
		this.vehicle = vehicle;
		this.path.add(path);
		this.earliestNextDeparture = Math.max(request.getEarliestStartTime(), path.getArrivalTime());

	}

	public TaxibusDispatch(Vehicle vehicle, VrpPathWithTravelData path) {
		this.requests = new LinkedHashSet<>();
		this.path = new ArrayList<>();
		this.vehicle = vehicle;
		this.path.add(path);
		this.earliestNextDeparture = path.getArrivalTime();

	}

	public void addRequests(Collection<DrtRequest> requests) {
		this.requests.addAll(requests);
	}

	public void addRequest(DrtRequest request) {
		this.requests.add(request);
	}

	public void addRequestAndPath(DrtRequest request, VrpPathWithTravelData path) {
		this.requests.add(request);
		// System.out.println(requests);
		this.path.add(path);
		this.earliestNextDeparture = Math.max(request.getEarliestStartTime(), path.getArrivalTime());
	}

	public void addPath(VrpPathWithTravelData path) {
		this.path.add(path);
		this.earliestNextDeparture = Math.max(this.earliestNextDeparture, path.getArrivalTime());
	}

	public double getEarliestNextDeparture() {

		return earliestNextDeparture;
	}

	public VrpPathWithTravelData getLastPathAdded() {
		return this.path.get(path.size() - 1);
	}

	public void removeLastPathAdded() {
		this.path.remove(path.size() - 1);
	}

	public void setTwMax(double twMax) {
		this.twMax = twMax;
	}

	public double getTwMax() {
		return twMax;
	}

	public void failIfAnyRequestNotUnplanned() {
		for (DrtRequest request : this.requests) {
			if (request.getStatus() != DrtRequestStatus.UNPLANNED) {
				throw new IllegalStateException();
			}
		}
	}

	public TreeSet<DrtRequest> getPickUpsForLink(Link link) {
		TreeSet<DrtRequest> beginningRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
		for (DrtRequest req : this.requests) {
			if (req.getFromLink().equals(link)) {

				if (req.getPickupTask() == null) {
					beginningRequests.add(req);
				}
			}
		}

		return beginningRequests.isEmpty() ? null : beginningRequests;
	}

	public TreeSet<DrtRequest> getDropOffsForLink(Link link) {
		TreeSet<DrtRequest> endingRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
		for (DrtRequest req : this.requests) {
			if (req.getToLink().equals(link)) {

				endingRequests.add(req);
			}
		}

		return endingRequests.isEmpty() ? null : endingRequests;
	}

}