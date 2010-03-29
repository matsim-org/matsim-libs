/* *********************************************************************** *
 * project: org.matsim.*
 * TimevariantTTStorage.java
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

/**
 *
 */
package playground.johannes.eut;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/**
 * @author illenberger
 *
 */
public class TimevariantTTStorage extends TimevariantValueStorage implements TravelTime, TravelCost {

	private static final Logger logger = Logger.getLogger(TimevariantTTStorage.class);

	/**
	 * @param startTime
	 * @param endTime
	 * @param binSize
	 */
	public TimevariantTTStorage(Network network, int startTime, int endTime, int binSize) {
		super(startTime, endTime, binSize);
		/*
		 * Initialize all bins with free flow travel time.
		 */
		for (Link link : network.getLinks().values()) {
			double freeTT = link.getLength() / link.getFreespeed();
			for (int i = 0; i < getBinCnt(); i++) {
				setBinValue(link, i, freeTT);
			}
		}
	}

	public double getLinkTravelTime(Link link, double time) {
		try {
			return getValue(link, (int) time);
		} catch (IndexOutOfBoundsException e) {
			logger.warn(String.format(
				"Trying to access travel time out of bounds (time=%1$d, starttime=%2$d, endtime=%3$d). Returning free flow travel time.",
				time, getStartTime(), getEndTime()), e);
			return link.getLength() / link.getFreespeed();
		}
	}

	public void setLinkTravelTime(Link link, double time, double value) {
		if(value <= 0)
			throw new IllegalArgumentException("Travel time values must be greater than zero!");

		try {
			setValue(link, (int)time, value);
		} catch (IndexOutOfBoundsException e) {
			logger.warn(String.format(
					"Trying to set travel time out of bounds (time=%1$d, starttime=%2$d, endtime=%3$d). Value will be ignored.",
					time, getStartTime(), getEndTime()), e);
		}
	}

	public double getLinkTravelCost(Link link, double time) {
		return getLinkTravelTime(link, time);
	}
}
