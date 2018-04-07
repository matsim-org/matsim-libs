/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.av.robotaxi.scoring;

import java.util.Map;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author  jbischoff
 * Config Group to set taxi fares.
 */
/**
 *
 */
public class TaxiFareConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "taxifare";

	public static final String BASEFARE = "basefare";
	public static final String DAILY_FEE = "dailySubscriptionFee";
	public static final String TIMEFARE = "timeFare_h";
	public static final String DISTANCEFARE = "distanceFare_m";

	private double basefare;
	private double dailySubscriptionFee;
	private double timeFare_h;
	private double distanceFare_m;

	
	public TaxiFareConfigGroup() {
		super(GROUP_NAME);

	}
	@SuppressWarnings("deprecation")
	public static TaxiFareConfigGroup get(Config config) {
		return (TaxiFareConfigGroup)config.getModule(GROUP_NAME);
	}
	
    @Override
    public Map<String, String> getComments()
    {
        Map<String, String> map = super.getComments();
        map.put(BASEFARE, "Basefare per Trip (fare = positive value)");
        map.put(DAILY_FEE, "Daily subscription fee (fee = positive value)");
        map.put(TIMEFARE , "taxi fare per hour (fee = positive value)");
        map.put(DISTANCEFARE, "taxi fare per meter (fee = positive value)");
		return map;
    }

	
	@StringGetter(BASEFARE)
	public double getBasefare() {
		return basefare;
	}

	@StringSetter(BASEFARE)
	public void setBasefare(double basefare) {
		this.basefare = basefare;
	}

	@StringGetter(DAILY_FEE)
	public double getDailySubscriptionFee() {
		return dailySubscriptionFee;
	}


	@StringSetter(DAILY_FEE)
	public void setDailySubscriptionFee(double dailySubscriptionFee) {
		this.dailySubscriptionFee = dailySubscriptionFee;
	}

	@StringGetter(TIMEFARE)
	public double getTimeFare_h() {
		return timeFare_h;
	}


	@StringSetter(TIMEFARE)
	public void setTimeFare_h(double timeFare_h) {
		this.timeFare_h = timeFare_h;
	}

	@StringGetter(DISTANCEFARE)
	public double getDistanceFare_m() {
		return distanceFare_m;
	}


	@StringSetter(DISTANCEFARE)
	public void setDistanceFare_m(double distanceFare_m) {
		this.distanceFare_m = distanceFare_m;
	}
	
	

}
