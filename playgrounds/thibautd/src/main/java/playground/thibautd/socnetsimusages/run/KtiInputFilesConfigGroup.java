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

package playground.thibautd.socnetsimusages.run;

import org.matsim.core.config.ReflectiveConfigGroup;

class KtiInputFilesConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "ktiInputFiles";

	private String worldFile = null;
	private String travelTimesFile = null;
	private String ptStopsFile = null;
	private double intrazonalPtSpeed = 4.361111;

	public KtiInputFilesConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "worldFile" )
	public String getWorldFile() {
		return this.worldFile;
	}

	@StringSetter( "worldFile" )
	public void setWorldFile(String worldFile) {
		this.worldFile = worldFile;
	}

	@StringGetter( "travelTimesFile" )
	public String getTravelTimesFile() {
		return this.travelTimesFile;
	}

	@StringSetter( "travelTimesFile" )
	public void setTravelTimesFile(String travelTimesFile) {
		this.travelTimesFile = travelTimesFile;
	}

	@StringGetter( "ptStopsFile" )
	public String getPtStopsFile() {
		return this.ptStopsFile;
	}

	@StringSetter( "ptStopsFile" )
	public void setPtStopsFile(String ptStopsFile) {
		this.ptStopsFile = ptStopsFile;
	}

	@StringGetter( "intrazonalPtSpeed" )
	public double getIntrazonalPtSpeed() {
		return intrazonalPtSpeed;
	}

	@StringSetter( "intrazonalPtSpeed" )
	public void setIntrazonalPtSpeed(final double v) {
		this.intrazonalPtSpeed = v;
	}
}
