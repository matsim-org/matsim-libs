/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.agentsmating.greedysavings;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.FacilitiesWriter;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTimeImpl;

import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;

/**
 * Creates factories for pick-up and drop-off activities.
 * One facility is created for each link where those events occur: this allows
 * not to break (to deeply at least) the facility-level subtour analysis.
 * However, the meaning of this kind of subtour analysis in the case of joint trips
 * is unclear: the default value "link" should not be changed.
 *
 * @author thibautd
 */
public class FacilitiesFactory {

	//private static final double EPSILON = 1E-7;
	private static final double EPSILON = 1;
	private static final OpeningTime OPENING_TIME = 
		new OpeningTimeImpl(OpeningTime.DayType.wkday, 0d, EPSILON);

	private final ActivityFacilitiesImpl facilities;
	private final Network network;
	private final IdFactory idFactory;
	//private final Map<Id, Id> pickUpFacilities = new TreeMap<Id, Id>();
	//private final Map<Id, Id> dropOffFacilities = new TreeMap<Id, Id>();
	private final Map<Id, Id> puDoFacilities = new TreeMap<Id, Id>();

	// /////////////////////////////////////////////////////////////////////////
	// constructor and relatives
	// /////////////////////////////////////////////////////////////////////////
	public FacilitiesFactory(
			// cannot use interface, as factory methods not part of any interface
			final ActivityFacilitiesImpl facilities,
			final Network network) {
		this.facilities = facilities;
		this.network = network;
		this.idFactory = new IdFactory(facilities);
	}

	// /////////////////////////////////////////////////////////////////////////
	// factory methods
	// /////////////////////////////////////////////////////////////////////////
	public Id getPickUpDropOffFacility(final Id linkId) {
		Id facilityId = puDoFacilities.get(linkId);

		if (facilityId == null) {
			facilityId = this.createFacility(linkId);
		}

		return facilityId;
	}

	// /////////////////////////////////////////////////////////////////////////
	// IO
	// /////////////////////////////////////////////////////////////////////////
	public void write(String file) {
		(new FacilitiesWriter(facilities)).write(file);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helper methods
	// /////////////////////////////////////////////////////////////////////////
	private Id createFacility(final Id linkId) {
		Id factoryId = this.idFactory.createId();

		ActivityFacilityImpl facility =
			this.facilities.createFacility(
					factoryId,
					this.network.getLinks().get(linkId).getCoord());

		createPUOption(facility);
		createDOOption(facility);

		this.puDoFacilities.put(linkId, factoryId);
		return factoryId;
	}

	private void createPUOption(final ActivityFacilityImpl facility) {
		ActivityOption option = facility.createActivityOption(JointActingTypes.PICK_UP);
		option.addOpeningTime(OPENING_TIME);
	}

	private void createDOOption(final ActivityFacilityImpl facility) {
		ActivityOption option = facility.createActivityOption(JointActingTypes.DROP_OFF);
		option.addOpeningTime(OPENING_TIME);
	}

	// /////////////////////////////////////////////////////////////////////////
	// helper classes
	// /////////////////////////////////////////////////////////////////////////
	
	/**
	 * Creates unexisting facility ids.
	 */
	private class IdFactory {
		private long currentId;

		public IdFactory(final ActivityFacilities facilities) {
			Id maxId = Collections.max(
					facilities.getFacilities().keySet(),
					new IdLongComparator());
			this.currentId = Long.parseLong(maxId.toString());
		}

		public Id createId() {
			this.currentId++;
			return new IdImpl(this.currentId);
		}
	}

	/**
	 * Compare Id based on the underlying long value, rather than the
	 * aphabetical order.
	 */
	private final class IdLongComparator implements Comparator<Id> {
		@Override
		public int compare(Id id1, Id id2) {
			long long1 = Long.parseLong(id1.toString());
			long long2 = Long.parseLong(id2.toString());
			return (new Long(long1)).compareTo(long2);
		}
	}
}

