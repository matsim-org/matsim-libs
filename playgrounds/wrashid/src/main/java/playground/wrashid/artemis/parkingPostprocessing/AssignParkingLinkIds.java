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

package playground.wrashid.artemis.parkingPostprocessing;

import org.geotools.math.Statistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Matrix;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import playground.wrashid.lib.tools.txtConfig.TxtConfig;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.scoring.ParkingInfo;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;

import java.util.HashMap;
import java.util.LinkedList;

public class AssignParkingLinkIds {

	// key: personId
	private static LinkedListValueHashMap<Id<Person>, ParkingInfo> parkingInfo;
	private static NetworkImpl network;
	private static HashMap<Id<PParking>, PParking> parkings;
	private static TxtConfig config;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		config = new TxtConfig(args[0]);
		
		String parkingLogInfoFileName= config.getParameterValue("parkingLogInfoFileName");
		parkingInfo = ParkingInfo.readParkingInfo(parkingLogInfoFileName);

		String networkFilePath = config.getParameterValue("networkFilePath");
		network = (NetworkImpl) GeneralLib.readNetwork(networkFilePath);

		parkings = readParkings();

		String parkingTimesInputFileName = config.getParameterValue("parkingTimesInputFileName");
		String parkingTimesOutputFileName = config.getParameterValue("parkingTimesOutputFileName");
		replaceLinkIdsInParkingTimesLog(parkingTimesInputFileName, parkingTimesOutputFileName);

		String chargingLogInputFileName = config.getParameterValue("chargingLogInputFileName");
		String chargingLogOutputFileName = config.getParameterValue("chargingLogOutputFileName");
		
		replaceLinkIdsInLogChargigLong(chargingLogInputFileName, chargingLogOutputFileName);
	}

	private static void replaceLinkIdsInLogChargigLong(String chargingLogInputFileName, String chargingLogOutputFileName) {
		Matrix chargingLog = GeneralLib.readStringMatrix(chargingLogInputFileName, "\t");

		for (int i = 1; i < chargingLog.getNumberOfRows(); i++) {
			Id<Person> personId = Id.create(chargingLog.getString(i, 1), Person.class);
			double chargingStartTime = chargingLog.getDouble(i, 2);

			LinkedList<ParkingInfo> parkingInf = parkingInfo.get(personId);

			double minDifference = Double.MAX_VALUE;
			ParkingInfo selectedParking = null;
			for (ParkingInfo pInfo : parkingInf) {
				double currentDistance = Math.abs(pInfo.getArrivalTime()- chargingStartTime);
				if (currentDistance < minDifference) {
					minDifference = currentDistance;
					selectedParking = pInfo;
				}
			}

			chargingLog.replaceString(i, 0, getClosestLinkFromParking(parkings.get(selectedParking.getParkingId())).toString());

			if (minDifference > 5000) {
				//DebugLib.stopSystemAndReportInconsistency();
			}

		}

		chargingLog.writeMatrix(chargingLogOutputFileName);
	}

	private static void replaceLinkIdsInParkingTimesLog(String parkingTimesInputFileName, String parkingTimesOutputFileName) {
		Matrix parkingTimes = GeneralLib.readStringMatrix(parkingTimesInputFileName, "\t");

		Statistics statisticsOnDistanceBetweenParkingAndActivity=new Statistics();
		
		for (int i = 1; i < parkingTimes.getNumberOfRows(); i++) {
			Id<Person> personId = Id.create(parkingTimes.getString(i, 0), Person.class);
			double arrivalTime = parkingTimes.getDouble(i, 1);
			Id<Link> activityLinkId =Id.create(parkingTimes.getString(i, 3), Link.class);
			
			LinkedList<ParkingInfo> parkingInf = parkingInfo.get(personId);

			double minDifference = Double.MAX_VALUE;
			ParkingInfo selectedParking = null;
			for (ParkingInfo pInfo : parkingInf) {
				double currentDistance = Math.abs(pInfo.getArrivalTime()- arrivalTime);
				if (currentDistance < minDifference) {
					minDifference = currentDistance;
					selectedParking = pInfo;
				}
			}

			Id<Link> closestLinkFromParking = getClosestLinkFromParking(parkings.get(selectedParking.getParkingId()));
			parkingTimes.replaceString(i, 3, closestLinkFromParking.toString());

			statisticsOnDistanceBetweenParkingAndActivity.add(GeneralLib.getDistance(network.getLinks().get(closestLinkFromParking).getCoord(), network.getLinks().get(activityLinkId).getCoord()));
			
			if (minDifference > 5000) {
				//DebugLib.stopSystemAndReportInconsistency();
			}

		}

		parkingTimes.writeMatrix(parkingTimesOutputFileName);
		
		System.out.println(statisticsOnDistanceBetweenParkingAndActivity.toString());
		
	}

	private static Id<Link> getClosestLinkFromParking(PParking parking) {
		return NetworkUtils.getNearestLink(network, parking.getCoord()).getId();
	}

	private static HashMap<Id<PParking>, PParking> readParkings() {
		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();
		
		int i=1;
		String parkingFileFlatFormat = config.getParameterValue("parkingFileFlatFormat_" + i);
		while (parkingFileFlatFormat!=null){
			ParkingHerbieControler.readParkings(1.0, parkingFileFlatFormat, parkingCollection);
			i++;
			parkingFileFlatFormat=config.getParameterValue("parkingFileFlatFormat_" + i);
		}

		HashMap<Id<PParking>, PParking> parkingHashmap = new HashMap<>();

		for (PParking parking : parkingCollection) {
			parkingHashmap.put(parking.getId(), parking);
		}

		return parkingHashmap;
	}





}
