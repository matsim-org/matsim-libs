/* *********************************************************************** *
 * project: org.matsim.*
 * TripContainer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.mz2005;

import java.util.LinkedList;
import java.util.List;

/**
 * @author illenberger
 *
 */
public class TripData {

	public String personId;
	
	public String tripId;
	
	public double distance;
	
	public double duration;
	
	public boolean outwardTrip;
	
	public int accompanists;
	
	public int leisureType;
	
	public int type;
	
	public int startTime;
	
	public int endTime;
	
	public int mode;
	
	public boolean roundTrip;
	
	public List<LegData> legs = new LinkedList<LegData>();
	
	public AggregatedMode aggrMode;
	
	public double[] startCoord;
	
	public double[] destCoord;
}
