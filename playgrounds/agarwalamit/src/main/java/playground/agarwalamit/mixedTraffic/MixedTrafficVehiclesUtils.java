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
	
	public final static String car = TransportMode.car;
	public final static String bike = TransportMode.bike;
	public final static String walk = TransportMode.walk;
	public final static String motorbike = "motorbike";
	public final static String truck = "truck";
	
	/**
	 * @param travelMode
	 * for which PCU value is required
	 * @return 
	 * default is PCU for car (1.0)
	 */
	public static double getPCU(String travelMode){
		double pcu;
		switch (travelMode) {
		case car: pcu = 1.0; break;
		case bike: pcu = 0.25; break;
		case motorbike: pcu = 0.25;break;
		case walk: pcu = 0.10;break;
		case truck: pcu = 3.0; break;
		default: pcu = 1.0; break;
		}
		return pcu;
	}
	
	/**
	 * @param travelMode
	 * for which speed is required
	 * @return 
	 * speed in mps; default is speed for car (16.67 mps)
	 */
	public static double getSpeed(String travelMode){
		double speed;
		switch (travelMode) {
		case car: speed = 16.67; break;
		case bike: speed = 4.17; break;
		case motorbike: speed = 16.67;break;
		case walk: speed = 1.2;break;
		case truck : speed = 8.33; break;
		default: speed = 16.67; break;
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
	public static double getCellSize(String travelMode){
		double MATSimCellSize = 7.5;
		return MATSimCellSize*getPCU(travelMode);
	}
}
