/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.dvrp.examples.onetruck;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

import com.google.inject.name.Named;

/**
 * @author michalm
 */
public final class OneTruckRequestCreator implements MobsimAfterSimStepListener {
	private final VrpOptimizer optimizer;
	private final PriorityQueue<OneTruckRequest> requests = new PriorityQueue<>(10,
			Comparator.comparing(Request::getSubmissionTime));

	@Inject
	public OneTruckRequestCreator(@DvrpMode(TransportMode.truck) VrpOptimizer optimizer,
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network) {
		this.optimizer = optimizer;
		requests.addAll(Arrays.asList(createRequest("parcel_0", "114", "349", 0, network),
				createRequest("parcel_1", "144", "437", 300, network),
				createRequest("parcel_2", "223", "347", 600, network),
				createRequest("parcel_3", "234", "119", 900, network),
				createRequest("parcel_4", "314", "260", 1200, network),
				createRequest("parcel_5", "333", "438", 1500, network),
				createRequest("parcel_6", "325", "111", 1800, network),
				createRequest("parcel_7", "412", "318", 2100, network),
				createRequest("parcel_8", "455", "236", 2400, network),
				createRequest("parcel_9", "139", "330", 2700, network)));
	}

	private OneTruckRequest createRequest(String requestId, String fromLinkId, String toLinkId, double time,
			Network network) {
		return new OneTruckRequest(Id.create(requestId, Request.class),
				network.getLinks().get(Id.createLinkId(fromLinkId)), network.getLinks().get(Id.createLinkId(toLinkId)),
				time);
	}

	@Override
	public void notifyMobsimAfterSimStep(@SuppressWarnings("rawtypes") MobsimAfterSimStepEvent e) {
		while (isReadyForSubmission(requests.peek(), e.getSimulationTime())) {
			optimizer.requestSubmitted(requests.poll());
		}
	}

	private boolean isReadyForSubmission(OneTruckRequest request, double currentTime) {
		return request != null && request.getSubmissionTime() <= currentTime;
	}
}
