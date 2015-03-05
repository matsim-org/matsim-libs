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

package playground.smetzler.santiago.demand;


/**
 * Container holding the data of one single line entry
 * 
 * @author aneumann
 *
 */
public class TripTableEntry {

	final String boardingZone;
	final String alightingZone;
	final double timeOfBoarding;
	final double avgNumberOfTripsPerWorkingDay;
	final double avgNumberOfTripsOfAdults;
	final double avgNumberOfTripsOfPupils;
	final double avgNumberOfTripsWith0Transfers;
	final double avgNumberOfTripsWith1Transfers;
	final double avgNumberOfTripsWith2Transfers;
	final double avgNumberOfTripsWith3Transfers;
	final double avgNumberOfTripsWith4orMoreTransfers;
	final double avgNumberOfTripsIncludingSubway;
	final double avgNumberOfTripsWithSubwayOnly;
	
	public TripTableEntry(final String boardingZone,
			final String alightingZone,
			final double timeOfBoarding,
			final double avgNumberOfTripsPerWorkingDay,
			final double avgNumberOfTripsOfAdults,
			final double avgNumberOfTripsOfPupils,
			final double avgNumberOfTripsWith0Transfers,
			final double avgNumberOfTripsWith1Transfers,
			final double avgNumberOfTripsWith2Transfers,
			final double avgNumberOfTripsWith3Transfers,
			final double avgNumberOfTripsWith4orMoreTransfers,
			final double avgNumberOfTripsIncludingSubway,
			final double avgNumberOfTripsWithSubwayOnly) {

		this.boardingZone = boardingZone;
		this.alightingZone = alightingZone;
		this.timeOfBoarding = timeOfBoarding;
		this.avgNumberOfTripsPerWorkingDay = avgNumberOfTripsPerWorkingDay;
		this.avgNumberOfTripsOfAdults = avgNumberOfTripsOfAdults;
		this.avgNumberOfTripsOfPupils = avgNumberOfTripsOfPupils;
		this.avgNumberOfTripsWith0Transfers = avgNumberOfTripsWith0Transfers;
		this.avgNumberOfTripsWith1Transfers = avgNumberOfTripsWith1Transfers;
		this.avgNumberOfTripsWith2Transfers = avgNumberOfTripsWith2Transfers;
		this.avgNumberOfTripsWith3Transfers = avgNumberOfTripsWith3Transfers;
		this.avgNumberOfTripsWith4orMoreTransfers = avgNumberOfTripsWith4orMoreTransfers;
		this.avgNumberOfTripsIncludingSubway = avgNumberOfTripsIncludingSubway;
		this.avgNumberOfTripsWithSubwayOnly = avgNumberOfTripsWithSubwayOnly;
		
	}
}
