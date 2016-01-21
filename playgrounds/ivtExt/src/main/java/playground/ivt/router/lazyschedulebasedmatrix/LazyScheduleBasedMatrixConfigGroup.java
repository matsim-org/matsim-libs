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
package playground.ivt.router.lazyschedulebasedmatrix;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

/**
 * @author thibautd
 */
public class LazyScheduleBasedMatrixConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "scheduleBasedPtMatrix";

	private double cellSize_m = 1000;
	private double timeBinDuration_s = 15 * 60;

	public LazyScheduleBasedMatrixConfigGroup() {
		super( GROUP_NAME );
	}

	@StringGetter( "cellSize_m" )
	public double getCellSize_m() {
		return cellSize_m;
	}

	@StringSetter( "cellSize_m" )
	public void setCellSize_m( double cellSize_m ) {
		this.cellSize_m = cellSize_m;
	}

	@StringGetter( "timeBinDuration" )
	private String getTimeBinDurationString() {
		return Time.writeTime( getTimeBinDuration_s() );
	}

	@StringSetter( "timeBinDuration" )
	public void setTimeBinDuration( final String formattedTime ) {
		setTimeBinDuration_s( Time.parseTime( formattedTime ) );
	}

	public double getTimeBinDuration_s() {
		return timeBinDuration_s;
	}

	public void setTimeBinDuration_s( double timeBinDuration_s ) {
		this.timeBinDuration_s = timeBinDuration_s;
	}
}
