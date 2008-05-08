/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesAllActivitiesFTE.java
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

package playground.meisterk.facilities;

import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.enterprisecensus.EnterpriseCensus;
import org.matsim.enterprisecensus.EnterpriseCensusParser;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.algorithms.FacilitiesAlgorithm;

public class FacilitiesActivitiesAreNogaFTE extends FacilitiesAlgorithm {

	private static Logger log = Logger.getLogger(FacilitiesActivitiesAreNogaFTE.class);

	private final static String TEMPORARY_FACILITY_ID_SEPARATOR = "_";

	private EnterpriseCensus myCensus;
	private TreeMap<String, String> facilityActivities = new TreeMap<String, String>();

	public FacilitiesActivitiesAreNogaFTE() {

		super();

	}

	@Override
	public void run(Facilities facilities) {

		this.createThem(facilities);
		//this.testTemporaryFacilityIds();

	}

	private void loadCensus() {
		
		log.info("Reading enterprise census files into EnterpriseCensus object...");
		this.myCensus = new EnterpriseCensus();

		EnterpriseCensusParser myCensusParser = new EnterpriseCensusParser(this.myCensus);
		try {
			myCensusParser.parse(this.myCensus);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Reading enterprise census files into EnterpriseCensus object...done.");
	}
	
	private void createThem(Facilities facilities) {

		this.loadCensus();
		myCensus.printPresenceCodesReport();
		myCensus.printHectareAggregationReport();

		// CONTINUE HERE
		// WITH COPYING FROM org.matsim.facilities.algorithms.FacilitiesAllActivitiesFTE
	}

}
