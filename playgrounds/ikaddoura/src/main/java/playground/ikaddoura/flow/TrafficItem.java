/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.flow;

import com.vividsolutions.jts.geom.Coordinate;

/**
* @author ikaddoura
*/

public class TrafficItem {

	private final String downloadTime;
	private final String id;
	private final int fc;
	private final double length;
	private final double confidence;
	private final double freespeed;
	private final double jamFactor;
	private final double actualSpeed;
	private final Coordinate[] coordinates;

	public TrafficItem(String id, String downloadTime, int fc, double length, double confidence, double freespeed, double jamFactor,
			double actualSpeed, Coordinate[] coordinates) {
		this.id = id;
		this.downloadTime = downloadTime;
		this.fc = fc;
		this.length = length;
		this.confidence = confidence;
		this.freespeed = freespeed;
		this.jamFactor = jamFactor;
		this.actualSpeed = actualSpeed;
		this.coordinates = coordinates;
	}

	public String getDownloadTime() {
		return downloadTime;
	}

	public int getFc() {
		return fc;
	}

	public double getLength() {
		return length;
	}

	public double getFreespeed() {
		return freespeed;
	}

	public double getJamFactor() {
		return jamFactor;
	}

	public double getActualSpeed() {
		return actualSpeed;
	}

	public double getConfidence() {
		return confidence;
	}

	public Coordinate[] getCoordinates() {
		return coordinates;
	}
	
	@Override
	public String toString() {
		
		return "TrafficItem [downloadTime=" + downloadTime + ", fc=" + fc + ", length=" + length + ", confidence="
				+ confidence + ", freespeed=" + freespeed + ", jamFactor=" + jamFactor + ", actualSpeed=" + actualSpeed
				+ ", coordinates=" + coordinates + "]";
	}

	public String getId() {
		return id;
	}
	
}

