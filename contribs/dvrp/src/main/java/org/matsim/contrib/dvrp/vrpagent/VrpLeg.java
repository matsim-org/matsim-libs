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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;

/**
 * ASSUMPTION: A vehicle enters and exits links at their ends (link.getToNode())
 *
 * @author michalm
 */
public class VrpLeg implements DivertibleLeg {
	private OnlineDriveTaskTracker onlineTracker;

	private VrpPath path;
	private int currentLinkIdx = 0;
	private boolean askedAboutNextLink = false;

	private final String mode;

	public VrpLeg(String mode, VrpPath path) {
		this.mode = mode;
		this.path = path;
	}

	public void initOnlineTracking(OnlineDriveTaskTracker onlineTracker) {
		Preconditions.checkState(this.onlineTracker == null, "Tracking already initialized");
		Preconditions.checkState(currentLinkIdx == 0, "Too late for initializing online tracking");
		this.onlineTracker = onlineTracker;
	}

	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkIdx++;
		askedAboutNextLink = false;

		Link currentLink = path.getLink(currentLinkIdx);
		if (currentLink.getId() != newLinkId) {
			throw new IllegalStateException();
		}

		if (onlineTracker != null) {
			onlineTracker.movedOverNode(currentLink);
		}
	}

	@Override
	public boolean canChangeNextLink() {
		return !askedAboutNextLink;
	}

	@Override
	public void pathDiverted(DivertedVrpPath divertedPath) {
		int immediateDiversionLinkIdx = currentLinkIdx + (canChangeNextLink() ? 0 : 1);
		Preconditions.checkState(divertedPath.getDiversionLinkIdx() >= immediateDiversionLinkIdx);
		Preconditions.checkArgument(divertedPath.getOriginalPath() == path,
				"divertedPath must be derived from the original one");
		path = divertedPath;
	}

	@Override
	public Id<Link> getNextLinkId() {
		askedAboutNextLink = true;

		if (currentLinkIdx == path.getLinkCount() - 1) {
			return null;
		}

		return path.getLink(currentLinkIdx + 1).getId();
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return path.getToLink().getId();
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return null;
	}

	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double getExpectedTravelTime() {
		return null;// teleportation is not handled
	}

	@Override
	public Double getExpectedTravelDistance() {
		return null;// teleportation is not handled
	}
}
