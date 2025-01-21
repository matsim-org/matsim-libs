/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgentImpl;
import org.matsim.core.mobsim.qsim.pt.TransitStopAgentTracker;
import org.matsim.pt.Umlauf;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;

/**
 * Railsim specific transit driver that can be re-routed.
 */
public final class RailsimTransitDriverAgent extends TransitDriverAgentImpl {

	private static final Logger log = LogManager.getLogger(RailsimTransitDriverAgent.class);

	/**
	 * Contains the original stop if it was overwritten by a detour.
	 */
	private TransitStopFacility originalStop;
	private TransitStopFacility detourStop;

	private final Map<Id<TransitStopArea>, List<TransitStopFacility>> stopAreas;

	public RailsimTransitDriverAgent(Map<Id<TransitStopArea>, List<TransitStopFacility>> stopAreas, Umlauf umlauf, String transportMode, TransitStopAgentTracker thisAgentTracker, InternalInterface internalInterface) {
		super(umlauf, transportMode, thisAgentTracker, internalInterface);
		this.stopAreas = stopAreas;
	}

	/**
	 * Add a detour to this driver schedule.
	 *
	 * @return next transit stop, if it needs to be changed.
	 */
	public TransitStopFacility addDetour(List<RailLink> original, List<RailLink> detour) {

		// The current next stop which may already be a detour
		TransitStopFacility nextStop = detourStop != null ? detourStop : getNextTransitStop();

		// Adjust the link index so that it fits to the new size
		// the original route inside this agent is not updated because it is currently not necessary
		// after the detour the link index should be consistent again
		setNextLinkIndex(getNextLinkIndex() + (original.size() - detour.size()));

		if (nextStop != null) {

			Id<TransitStopArea> areaId = nextStop.getStopAreaId();

			boolean adjust = false;
			for (RailLink link : original) {
				if (nextStop.getLinkId().equals(link.getLinkId())) {
					adjust = true;
					break;
				}
			}

			// pt stop needs to be remapped
			if (adjust) {

				List<TransitStopFacility> inArea = stopAreas.get(areaId);

				for (TransitStopFacility stop : inArea) {
					for (RailLink d : detour) {

						if (stop.getLinkId().equals(d.getLinkId())) {

							// Always contains the stop from original schedule
							if (originalStop == null) {
								this.originalStop = nextStop;
							}

							this.detourStop = stop;
							return stop;
						}
					}
				}

				log.warn("Could not re-route vehicle {} to a replacement transit stop", getVehicle().getId());
			}
		}

		return null;
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now) {

		// This function will call the API with the original stop as if no reroute has happened

		// use the original stop exactly one time
		if (originalStop != null) {
			stop = originalStop;
			double t = super.handleTransitStop(stop, now);

			// Vehicle has not yet departed and must wait, stops are not updated yet
			if (t > 0) {
				return t;
			}

			originalStop = null;
			detourStop = null;
			return t;
		}

		return super.handleTransitStop(stop, now);
	}
}
