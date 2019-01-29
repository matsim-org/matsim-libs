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

package org.matsim.contrib.dvrp.tracker;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.DivertedVrpPath;
import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class OnlineDriveTaskTrackerImpl implements OnlineDriveTaskTracker {
	private final Vehicle vehicle;
	private final DriveTask driveTask;
	private final VrpLeg vrpDynLeg;

	private final OnlineTrackerListener onlineTrackerListener;
	private final MobsimTimer timer;

	private VrpPath path;
	private int currentLinkIdx;
	private double linkEnterTime;
	private double[] remainingTTs;// excluding the current link

	public OnlineDriveTaskTrackerImpl(Vehicle vehicle, VrpLeg vrpDynLeg, OnlineTrackerListener onlineTrackerListener,
			MobsimTimer timer) {
		this.vehicle = vehicle;
		this.driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		this.vrpDynLeg = vrpDynLeg;
		this.onlineTrackerListener = onlineTrackerListener;
		this.timer = timer;

		initForPath(driveTask.getPath());
		currentLinkIdx = 0;
		linkEnterTime = driveTask.getBeginTime();
	}

	private void initForPath(VrpPath path) {
		this.path = path;
		remainingTTs = new double[path.getLinkCount()];

		double tt = 0;
		for (int i = remainingTTs.length - 1; i >= 0; i--) {
			remainingTTs[i] = tt;
			tt += path.getLinkTravelTime(i);
		}
	}

	@Override
	public VrpPath getPath() {
		return path;
	}

	@Override
	public int getCurrentLinkIdx() {
		return currentLinkIdx;
	}

	@Override
	public void movedOverNode(Link nextLink) {
		currentLinkIdx++;
		linkEnterTime = timer.getTimeOfDay();
		onlineTrackerListener.vehicleEnteredNextLink(vehicle, nextLink);
	}

	/**
	 * Assumption: vehicle is diverted as soon as possible, i.e.:
	 * <ul>
	 * <li>if the next link can be changed: after the current link</li>
	 * <li>If not then, (a) if the current link is not the last one, after the next link, or</li>
	 * <li>(b) no diversion possible (the leg ends on the current link)</li>
	 * </ul>
	 */
	@Override
	public LinkTimePair getDiversionPoint() {
		if (vrpDynLeg.canChangeNextLink()) {
			return new LinkTimePair(path.getLink(currentLinkIdx), predictLinkExitTime());
		}

		if (path.getLinkCount() == currentLinkIdx + 1) {// the current link is the last one
			return null;// too late to divert (reason: cannot change the next link)
		}

		double nextLinkTT = path.getLinkTravelTime(currentLinkIdx + 1);
		double predictedNextLinkExitTime = predictLinkExitTime() + nextLinkTT;
		return new LinkTimePair(path.getLink(currentLinkIdx + 1), predictedNextLinkExitTime);
	}

	@Override
	public void divertPath(VrpPathWithTravelData newSubPath) {
		LinkTimePair diversionPoint = getDiversionPoint();

		if (!newSubPath.getFromLink().equals(diversionPoint.link)
				|| newSubPath.getDepartureTime() != diversionPoint.time) {
			throw new IllegalArgumentException();
		}

		int diversionLinkIdx = currentLinkIdx + (vrpDynLeg.canChangeNextLink() ? 0 : 1);
		DivertedVrpPath divertedPath = new DivertedVrpPath(path, newSubPath, diversionLinkIdx);
		initForPath(divertedPath);

		vrpDynLeg.pathDiverted(divertedPath);
		driveTask.pathDiverted(divertedPath, newSubPath.getArrivalTime());
	}

	@Override
	public double predictEndTime() {
		return predictLinkExitTime() + remainingTTs[currentLinkIdx];
	}

	private double predictLinkExitTime() {
		return Math.max(timer.getTimeOfDay(), linkEnterTime + path.getLinkTravelTime(currentLinkIdx));
	}
}
