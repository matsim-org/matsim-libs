/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.mrieser.core.facilities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;

/**
 * Generates {@link ActivityFacility}s from the {@link Activity Activities} in a population 
 * and assigns the activity facilities as the activity locations in the population while 
 * removing the old locations (coord and link) from the population.
 * If an activity already has an ActivityFacility assigned, the ActivityFacility is overwritten.
 * If an activity only has a coordinate, different behavior is possible, see 
 * {@link #setAssignLinksToFacilitiesIfMissing(boolean, Network)}.
 * 
 * @author mrieser / Senozon AG
 */
public class FacilitiesFromPopulation {

	private final static Logger log = Logger.getLogger(FacilitiesFromPopulation.class);
	
	private final ActivityFacilities facilities;
	private boolean oneFacilityPerLink = true;
	private String idPrefix = "";
	private Network network = null;
	private boolean removeLinksAndCoordinates = true;
	private PlanCalcScoreConfigGroup config = null;
	
	public FacilitiesFromPopulation(final ActivityFacilities facilities) {
		this.facilities = facilities;
	}
	
	/**
	 * Sets where all activities on a link should be collected within one ActivityFacility.
	 * Default is <code>true</code>. If set to <code>false</code>, for each coordinate
	 * found in the population's activities a separate ActivityFacility will be created.
	 * 
	 * @param oneFacilityPerLink
	 */
	public void setOneFacilityPerLink(final boolean oneFacilityPerLink) {
		this.oneFacilityPerLink = oneFacilityPerLink;
	}
	
	public void setIdPrefix(final String prefix) {
		this.idPrefix = prefix;
	}
	
	/**
	 * In the case that an activity has no link assigned, the activity can be assigned to the closest link.
	 * If there should be only one ActivityFacility per link (see {@link #setOneFacilityPerLink(boolean)}), 
	 * and if no assignment should be done, then a new ActivityFacility will be created at that coordinate
	 * and the facility will not be assigned to a link, essentially breaking the contract of 
	 * {@link #setOneFacilityPerLink(boolean)}.
	 * 
	 * @param doAssignment
	 * @param network
	 */
	public void setAssignLinksToFacilitiesIfMissing(final boolean doAssignment, final Network network) {
		if (doAssignment && network == null) {
			throw new IllegalArgumentException("Network cannot be null if assignment should be done.");
		}
		this.network = doAssignment ? network : null;
	}
	
	/**
	 * If set to <code>true</code> (which is the default), the link and coordinate attributes
	 * are nulled in the activities, as this information is now available via the facility.
	 * 
	 * @param doRemoval
	 */
	public void setRemoveLinksAndCoordinates(final boolean doRemoval) {
		this.removeLinksAndCoordinates = doRemoval;
	}
	
	public void assignOpeningTimes(final boolean doAssignment, final PlanCalcScoreConfigGroup config) {
		if (doAssignment && config == null) {
			throw new IllegalArgumentException("Config must not be null if opening times should be assigned.");
		}
		this.config = doAssignment ? config : null;
	}
	
	public void run(final Population population) {
		handleActivities(population);
		if (this.config != null) {
			this.assignOpeningTimes();
		}
	}
	
	private void handleActivities(final Population population) {
		int idxCounter = 0;
		ActivityFacilitiesFactory factory = this.facilities.getFactory();
		Map<Id<Link>, ActivityFacility> facilitiesPerLinkId = new HashMap<>();
		Map<Coord, ActivityFacility> facilitiesPerCoordinate = new HashMap<Coord, ActivityFacility>();
		
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity a = (Activity) pe;
						
						Coord c = a.getCoord();
						Id<Link> linkId = a.getLinkId();
						ActivityFacility facility = null;

						if (linkId == null && this.network != null) {
							final Coord coord = c;
							linkId = NetworkUtils.getNearestLinkExactly(this.network,coord).getId();
						}

						if (this.oneFacilityPerLink && linkId != null) {
							facility = facilitiesPerLinkId.get(linkId);
							if (facility == null) {
								facility = factory.createActivityFacility(Id.create(this.idPrefix + linkId.toString(), ActivityFacility.class), c, linkId);
								this.facilities.addActivityFacility(facility);
								facilitiesPerLinkId.put(linkId, facility);
							}
						} else {
							facility = facilitiesPerCoordinate.get(c);
							if (facility == null) {
								facility = factory.createActivityFacility(Id.create(this.idPrefix + idxCounter++, ActivityFacility.class), c, linkId);
								this.facilities.addActivityFacility(facility);
								facilitiesPerCoordinate.put(c, facility);
							}
						}
						
						String actType = a.getType();
						ActivityOption option = facility.getActivityOptions().get(actType);
						if (option == null) {
							option = factory.createActivityOption(actType);
							facility.addActivityOption(option);
						}
						
						((Activity) a).setFacilityId(facility.getId());
						if (this.removeLinksAndCoordinates) {
							((Activity) a).setLinkId(null);
							((Activity) a).setCoord(null);
						}
					}
				}
			}
		}
	}

	private void assignOpeningTimes() {
		Set<String> missingActTypes = new HashSet<String>();
		for (ActivityFacility af : this.facilities.getFacilities().values()) {
			for (ActivityOption ao : af.getActivityOptions().values()) {
				String actType = ao.getType();
				ActivityParams params = this.config.getActivityParams(actType);
				if (params == null) {
					if (missingActTypes.add(actType)) {
						log.error("No information for activity type " + actType + " found in given configuration.");
					}
				} else {
					ao.addOpeningTime(new OpeningTimeImpl(params.getOpeningTime(), params.getClosingTime()));
				}
			}
		}
	}
	
}
