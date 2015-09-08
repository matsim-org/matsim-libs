/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.ca.matlabInfra;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;

import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

public class Config {

	private static double radiusInMetersOfStudyArea = 1500;
	private static Coord studyAreaCenter = ParkingHerbieControler.getCoordinatesQuaiBridgeZH();
	public static String baseFolder = "C:/l/studies/LaHowara_Parking/input/zurichCity/matsim_scenario/teleatlas/";
	private static String outputFolder = baseFolder + "/created/";
	
	private static NetworkImpl network=null;
	
	public static String getNetworkFile() {
		return baseFolder + "network.xml.gz";
	}

	public static String getOutputFolder(){
		return outputFolder;
	}
	
	public static String getEventsFile() {
		return baseFolder + "events.xml.gz";
	}

	public static boolean isInsideStudyArea(double x, double y) {
		return isInsideStudyArea(new Coord(x, y));
	}
	
	public static boolean isInsideStudyArea(Coord coord) {
		return GeneralLib.getDistance(coord, studyAreaCenter) < radiusInMetersOfStudyArea;
	}
	
	public static boolean isInsideStudyArea(Id linkId) {
		Coord coord=getNetwork().getLinks().get(linkId).getCoord();
		return isInsideStudyArea(coord);
	}
	
	public static NetworkImpl getNetwork(){
		if (network==null){
			network=(NetworkImpl) GeneralLib.readNetwork(getNetworkFile());
		}
		return network;
	}
	
	public static boolean isInsideSNetworkArea(Coord coord) {
		return GeneralLib.getDistance(coord, studyAreaCenter) < 1.0 * radiusInMetersOfStudyArea;
	}
}
