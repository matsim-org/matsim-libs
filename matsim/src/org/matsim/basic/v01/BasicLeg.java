/* *********************************************************************** *
 * project: org.matsim.*
 * BasicAct.java
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
package org.matsim.basic.v01;
/**
*
* @author dgrether
*
*/

public interface BasicLeg {
	
	public enum Mode {miv, car, ride, motorbike, pt, train, bus, tram, bike, walk, undefined};
//	
//	public static final String MIVMODE = "miv";
//
//	public static final String CARMODE = "car";
//
//	public static final String RIDEMODE = "ride";
//
//	public static final String MOTORBIKEMODE = "motorbike";
//
//	public static final String PTMODE = "pt";
//
//	public static final String TRAINMODE = "train";
//
//	public static final String BUSMODE = "bus";
//
//	public static final String TRAMMODE = "tram";
//
//	public static final String BIKEMODE = "bike";
//
//	public static final String WALKMODE = "walk";
//
//	public static final String UNDEFINEDMODE = "undef";

	public int getNum();

	public void setNum(int num);

	public Mode getMode();

	public void setMode(Mode mode);

	public BasicRoute getRoute();

	public void setRoute(BasicRoute route);

	public double getDepTime();

	public void setDepTime(final double depTime);

	public double getTravTime();

	public void setTravTime(final double travTime);

	public double getArrTime();

	public void setArrTime(final double arrTime);


}