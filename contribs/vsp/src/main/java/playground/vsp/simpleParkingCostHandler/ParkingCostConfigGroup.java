/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package playground.vsp.simpleParkingCostHandler;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author ikaddoura
 */

public class ParkingCostConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "parkingCost" ;

	public ParkingCostConfigGroup() {
		super(GROUP_NAME);
	}
	private String mode = "car";
	private String dailyParkingCostLinkAttributeName = "dailyPCost";
	private String firstHourParkingCostLinkAttributeName = "oneHourPCost";
	private String extraHourParkingCostLinkAttributeName = "extraHourPCost";
	private String maxDailyParkingCostLinkAttributeName = "maxDailyPCost";
	private String maxParkingDurationAttributeName = "maxPDuration";
	private String parkingPenaltyAttributeName = "penalty";
	private String residentialParkingFeePerDay = "residentialPFee";
	private String activityPrefixForDailyParkingCosts = "home";
	private final Set<String> activityPrefixToBeExcludedFromParkingCost = new HashSet<>();

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getDailyParkingCostLinkAttributeName() {
		return dailyParkingCostLinkAttributeName;
	}

	public void setDailyParkingCostLinkAttributeName(String dailyParkingCostLinkAttributeName) {
		this.dailyParkingCostLinkAttributeName = dailyParkingCostLinkAttributeName;
	}

	public String getFirstHourParkingCostLinkAttributeName() {
		return firstHourParkingCostLinkAttributeName;
	}

	public void setFirstHourParkingCostLinkAttributeName(String firstHourParkingCostLinkAttributeName) {
		this.firstHourParkingCostLinkAttributeName = firstHourParkingCostLinkAttributeName;
	}

	public String getExtraHourParkingCostLinkAttributeName() {
		return extraHourParkingCostLinkAttributeName;
	}

	public void setExtraHourParkingCostLinkAttributeName(String extraHourParkingCostLinkAttributeName) {
		this.extraHourParkingCostLinkAttributeName = extraHourParkingCostLinkAttributeName;
	}

	public String getMaxDailyParkingCostLinkAttributeName() {
		return maxDailyParkingCostLinkAttributeName;
	}

	public void setMaxDailyParkingCostLinkAttributeName(String maxDailyParkingCostLinkAttributeName) {
		this.maxDailyParkingCostLinkAttributeName = maxDailyParkingCostLinkAttributeName;
	}

	public String getActivityPrefixesToBeExcludedFromParkingCostAsString() {
		return CollectionUtils.setToString(this.activityPrefixToBeExcludedFromParkingCost);
	}

	public Set<String> getActivityPrefixesToBeExcludedFromParkingCost() {
		return this.activityPrefixToBeExcludedFromParkingCost;
	}

	public void setActivityPrefixToBeExcludedFromParkingCost(String prefixes) {
		setActivityPrefixToBeExcludedFromParkingCost(CollectionUtils.stringToSet(prefixes));
	}

	public void setActivityPrefixToBeExcludedFromParkingCost(Set<String> prefixes) {
		this.activityPrefixToBeExcludedFromParkingCost.clear();
		this.activityPrefixToBeExcludedFromParkingCost.addAll(prefixes);
	}

    public String getMaxParkingDurationAttributeName() {
		return maxParkingDurationAttributeName;
    }

	public void setMaxParkingDurationAttributeName(String maxParkingDurationAttributeName) {
		this.maxParkingDurationAttributeName = maxParkingDurationAttributeName;
	}

	public String getParkingPenaltyAttributeName() {
		return parkingPenaltyAttributeName;
	}

	public void setParkingPenaltyAttributeName(String parkingPenaltyAttributeName) {
		this.parkingPenaltyAttributeName = parkingPenaltyAttributeName;
	}

	public String getResidentialParkingFeeAttributeName() {
		return residentialParkingFeePerDay;
	}

	public void setResidentialParkingFeeAttributeName(String residentialParkingFeePerDay) {
		this.residentialParkingFeePerDay = residentialParkingFeePerDay;
	}

	public String getActivityPrefixForDailyParkingCosts() {
		return activityPrefixForDailyParkingCosts;
	}

	public void setActivityPrefixForDailyParkingCosts(String activityPrefixForDailyParkingCosts) {
		this.activityPrefixForDailyParkingCosts = activityPrefixForDailyParkingCosts;
	}

}

