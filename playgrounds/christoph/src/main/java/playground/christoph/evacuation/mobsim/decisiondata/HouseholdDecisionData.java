/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdDecisionData.java
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

package playground.christoph.evacuation.mobsim.decisiondata;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;

import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;

/**
 * Data structure containing information used by a household for the decision
 * "evacuate directly vs. meet at home first".
 * 
 * Contains only static information, i.e. stuff like a household's home facility id
 * but no dynamic information like whether a household is currently joined. 
 * 
 * @author cdobler
 */
public class HouseholdDecisionData {

	/*
	 * Model results
	 */
	private EvacuationDecision evacuationDecision = EvacuationDecision.UNDEFINED;
	
	/*
	 * Model input data
	 */
	private final Id<Household> householdId;
	private Id<Link> homeLinkId = null;
	private Id<ActivityFacility> homeFacilityId = null;
	private Id<ActivityFacility> meetingFacilityId = null;
	private HouseholdPosition householdPosition = null;
	private boolean hasChildren = false;
	private Participating participating = Participating.UNDEFINED;
	private boolean homeFacilityIsAffected = false;
	
	/*
	 * Time when the household wants to have left the evacuation area.
	 */
	private double latestAcceptedLeaveTime = Double.MAX_VALUE;
	
	/*
	 * Time when all household members would arrive at home when traveling 
	 * there to evacuate jointly.
	 */
	private double householdReturnHomeTime = Double.MAX_VALUE;
	
	/*
	 * Time when all members of a household which evacuates from home have
	 * arrived at a secure facility.
	 */
	private double householdEvacuateFromHomeTime = Double.MAX_VALUE;
	
	/*
	 * Time when the last household member would arrive at a secure facility 
	 * when evacuating directly = max(mebers.directEvacuationTime).
	 */	
	private double householdDirectEvacuationTime = Double.MAX_VALUE;
	
	/*
	 * Time that a household waits after having met at its meeting point
	 * before leaving.
	 */
	private double departureTimeDelay = Double.MAX_VALUE;
	
	public HouseholdDecisionData(Id<Household> householdId) {
		this.householdId = householdId;
	}
	
	public Id<Household> getHouseholdId() {
		return this.householdId;
	}
	
	public Id<Link> getHomeLinkId() {
		return homeLinkId;
	}

	public void setHomeLinkId(Id<Link> homeLinkId) {
		this.homeLinkId = homeLinkId;
	}

	public Id<ActivityFacility> getHomeFacilityId() {
		return homeFacilityId;
	}

	public void setHomeFacilityId(Id<ActivityFacility> homeFacilityId) {
		this.homeFacilityId = homeFacilityId;
	}
	
	public Id<ActivityFacility> getMeetingPointFacilityId() {
		return this.meetingFacilityId;
	}
	
	public void setMeetingPointFacilityId(Id<ActivityFacility> meetingFacilityId) {
		this.meetingFacilityId = meetingFacilityId;
	}
	
	public EvacuationDecision getEvacuationDecision() {
		return evacuationDecision;
	}

	public void setEvacuationDecision(EvacuationDecision evacuationDecision) {
		this.evacuationDecision = evacuationDecision;
	}

	public double getDepartureTimeDelay() {
		return this.departureTimeDelay;
	}
	
	public void setDepartureTimeDelay(double departureTimeDelay) {
		this.departureTimeDelay = departureTimeDelay;
	}
	
	public HouseholdPosition getHouseholdPosition() {
		return householdPosition;
	}

	public void setHouseholdPosition(HouseholdPosition householdPosition) {
		this.householdPosition = householdPosition;
	}

	public boolean hasChildren() {
		return hasChildren;
	}

	public void setChildren(boolean hasChildren) {
		this.hasChildren = hasChildren;
	}
	
	public Participating getParticipating() {
		return participating;
	}

	public void setParticipating(Participating participating) {
		this.participating = participating;
	}

	public boolean isHomeFacilityIsAffected() {
		return homeFacilityIsAffected;
	}

	public void setHomeFacilityIsAffected(boolean homeFacilityIsAffected) {
		this.homeFacilityIsAffected = homeFacilityIsAffected;
	}

	public double getLatestAcceptedLeaveTime() {
		return latestAcceptedLeaveTime;
	}

	public void setLatestAcceptedLeaveTime(double latestAcceptedLeaveTime) {
		this.latestAcceptedLeaveTime = latestAcceptedLeaveTime;
	}

	public double getHouseholdReturnHomeTime() {
		return householdReturnHomeTime;
	}

	public void setHouseholdReturnHomeTime(double householdReturnHomeTime) {
		this.householdReturnHomeTime = householdReturnHomeTime;
	}

	public double getHouseholdEvacuateFromHomeTime() {
		return householdEvacuateFromHomeTime;
	}

	public void setHouseholdEvacuateFromHomeTime(double householdEvacuateFromHomeTime) {
		this.householdEvacuateFromHomeTime = householdEvacuateFromHomeTime;
	}

	public double getHouseholdDirectEvacuationTime() {
		return householdDirectEvacuationTime;
	}

	public void setHouseholdDirectEvacuationTime(double householdDirectEvacuationTime) {
		this.householdDirectEvacuationTime = householdDirectEvacuationTime;
	}
	
}