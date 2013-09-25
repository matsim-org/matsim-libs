/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.jbischoff.taxi.rank;

import pl.poznan.put.vrp.dynamic.data.network.Arc;
import pl.poznan.put.vrp.dynamic.data.schedule.DriveTask;
import pl.poznan.put.vrp.dynamic.taxi.schedule.TaxiDriveTask;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */
public class BackToRankTask extends TaxiDriveTask implements DriveTask {
	 private final TaxiDriveType driveType;

	
	private Arc arc;
	
	
	public BackToRankTask(int beginTime, int endTime, Arc arc) {
		super(beginTime, endTime, arc);
		
		//the following is nonsense, as a drive to rank is not a pickup task. 
		//But it works for the time being as expected
	
		driveType = TaxiDriveType.PICKUP;
		this.arc = arc;	}

	@Override
	public TaskType getType() {
		 return TaskType.DRIVE;
	}
	@Override
	public TaxiDriveType getDriveType()
	    {
	        return driveType;
	    }
	
	
	@Override
	public Arc getArc() {
		return arc;
	}

	   @Override
	    public String toString()
	    {
	        return "DtoRank(@" + arc.getFromVertex().getId() + "->@" + arc.getToVertex().getId() + ")"
	                + commonToString();
	    }
}
