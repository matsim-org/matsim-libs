/* *********************************************************************** *
 * project: org.matsim.*
 * CommuterInformationRevisor.java
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

package playground.lnicolas.ktiProject;

import java.util.ArrayList;
import java.util.Set;

import org.matsim.basic.v01.Id;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.Facility;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;
import org.matsim.world.Location;
import org.matsim.world.Zone;

/**
 * "Corrects" the given commuter matrices.
 * @author lnicolas
 *
 */
public class CommuterInformationRevisor {

	private final Facilities facilities;
	private final ArrayList<Zone> zones;

	public CommuterInformationRevisor(final ArrayList<Zone> zones,
			final Facilities facilities) {
		this.facilities = facilities;
		this.zones = zones;
	}

	/**
	 * Removes entries from the given commuter matrices that point to zones that
	 * contain no facility (or only facilities with zero capacity) for the respective
	 * activity. Adds commuter entries to the nearest zone for zones whose home activity
	 * capacity is greater 0 (i.e. where there may live persons) but for which there
	 * are no outgoing work or education entries in the respective commuter matrix.
	 * @param workCommuterMatrix
	 * @param educationCommuterMatrix
	 */
	public void run(final Matrix workCommuterMatrix,
			final Matrix educationCommuterMatrix) {
		removeCommuterEntriesWithZeroToCapacity(workCommuterMatrix,
				GroupFacilitiesPerZone.workActType);
		addMissingCommuterEntries(workCommuterMatrix,
				GroupFacilitiesPerZone.workActType);
		removeCommuterEntriesWithZeroToCapacity(educationCommuterMatrix,
				GroupFacilitiesPerZone.eduActType);
		addMissingCommuterEntries(educationCommuterMatrix,
				GroupFacilitiesPerZone.eduActType);
	}

	/**
	 * Adds commuter entries to the nearest zone for zones whose home activity
	 * capacity is greater 0 (i.e. where there may live persons) but for which there
	 * are no outgoing {@code actType} entries in the respective commuter matrix
	 * {@code commuterMatrix}.
	 * @param commuterMatrix
	 * @param actType
	 */
	private void addMissingCommuterEntries(final Matrix commuterMatrix, final String actType) {
		for (Zone zone : this.zones) {
			if (CommuterInformationGenerator.getCapacity(this.facilities,
					CommuterInformationGenerator.homeActType, zone) > 0) {
				ArrayList<Entry> fromZoneDistr =
					commuterMatrix.getFromLocEntries(zone);
				if (fromZoneDistr == null ||
						CommuterInformationGenerator.getRandomToZone(fromZoneDistr) == null) {
					Zone toZone = getNearestZone(zone, actType);
					commuterMatrix.setEntry(zone, toZone, 1.0);
					System.out.println(actType + " from zone " + zone.getName() + " (" + zone.getId()
							+ ") is now done in zone " + toZone.getName() + " (" + toZone.getId() +
							"). Distance is " +
							Math.round(zone.getCenter().calcDistance(toZone.getCenter())));
				}
			}
		}
	}

	private Zone getNearestZone(final Zone zone, final String actType) {
		double nearestDist = Double.POSITIVE_INFINITY;
		Zone nearestZone = null;
		for (Zone zone2 : this.zones) {
			if (containsActivityFacility(zone2, actType)) {
				double dist = zone.getCenter().calcDistance(zone2.getCenter());
				if (dist < nearestDist) {
					nearestDist = dist;
					nearestZone = zone2;
				}
			}
		}
		return nearestZone;
	}

	/**
	 * Removes entries from the given commuter matrix {@code commuterMatrix} that point to zones that
	 * contain no facility (or only facilities with zero capacity) for the respective
	 * activity of type {@code actType}.
	 * @param commuterMatrix
	 * @param actType
	 */
	private void removeCommuterEntriesWithZeroToCapacity(final Matrix commuterMatrix, final String actType) {
		for (Zone zone : this.zones) {
			int capacityCount =
				CommuterInformationGenerator.getCapacity(this.facilities,
						actType, zone);
			if (capacityCount == 0) {
				commuterMatrix.removeToLocEntries(zone);
			}
		}
	}

	private boolean containsActivityFacility(final Location location, final String activityType) {
		Set<Id> facilityIds = location.getDownMapping().keySet();
		for (Id facilityId : facilityIds) {
			Facility facility = (Facility)this.facilities.getLocation(facilityId);
			Activity activity = facility.getActivity(activityType);
			if (activity != null && activity.getCapacity() > 0) {
				return true;
			}
		}

		return false;
	}
}
