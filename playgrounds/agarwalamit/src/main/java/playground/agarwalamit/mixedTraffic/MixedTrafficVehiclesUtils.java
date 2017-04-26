/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic;

import org.matsim.api.core.v01.TransportMode;

/**
 * @author amit
 */
public class MixedTrafficVehiclesUtils {
	
	public final static String CAR = TransportMode.car;
	public final static String BIKE = TransportMode.bike;
	public final static String WALK = TransportMode.walk;
//	public final static String PT = TransportMode.pt;
	public final static String MOTORBIKE = "motorbike";
	public final static String TRUCK = "truck";
	
	/**
	 * @param travelMode
	 * for which PCU value is required
	 */
	public static double getPCU(final String travelMode){
		double pcu;
		switch (travelMode) {
		case CAR: pcu = 1.0; break;
		case "bicycle":
		case BIKE: pcu = 0.25; break;
		case MOTORBIKE: pcu = 0.25;break;
		case WALK: pcu = 0.10;break;
//		case PT :
		case TRUCK: pcu = 3.0; break;
		default: throw new RuntimeException("No PCU is set for travel mode "+travelMode+ ".");
		}
		return pcu;
	}
	
	/**
	 * @param travelMode
	 * for which speed is required
	 */
	public static double getSpeed(final String travelMode){
		double speed;
		switch (travelMode) {
		case CAR: speed = 16.67; break;
		case "bicycle":
		case BIKE: speed = 4.17; break;
		case MOTORBIKE: speed = 16.67;break;
		case WALK: speed = 1.2;break;
//		case PT :
		case TRUCK : speed = 8.33; break;
		default: throw new RuntimeException("No speed is set for travel mode "+travelMode+ ".");
		}
		return speed;
	}
	
	/**
	 * @param travelMode
	 * for which effective cell size is required
	 * @return 
	 * physical road space occupied based on PCU unit 
	 * default is cell size for car (7.5 m)
	 */
	public static double getCellSize(final String travelMode){
		double matsimCellSize = 7.5;
		return matsimCellSize*getPCU(travelMode);
	}
}
