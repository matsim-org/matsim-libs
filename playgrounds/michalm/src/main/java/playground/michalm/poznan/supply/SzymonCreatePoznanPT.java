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

package playground.michalm.poznan.supply;

public class SzymonCreatePoznanPT {
	public static void main(String[] args) {
		String visumFile = "d:/OneDrive/Poznan/Visum_2014/network/network_ver.4.net";
		String transitScheduleWithNetworkFile = "d:/transitSchedule.networkOevModellZH.xml";
		String transitNetworkFile = "d:/network.oevModellZH.xml";
		String vehicleFile = "d:/vehicles.xml";

		CreatePoznanPT.go(visumFile, transitScheduleWithNetworkFile, transitNetworkFile, vehicleFile);
	}
}
