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

package org.matsim.facilities.algorithms;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.enterprisecensus.EnterpriseCensus;
import org.matsim.enterprisecensus.EnterpriseCensusParser;
import org.matsim.facilities.Activity;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.FacilitiesProductionKTI;
import org.matsim.facilities.Facility;
import org.matsim.facilities.FacilitiesProductionKTI.KTIYear;
import org.matsim.gbl.Gbl;

public class FacilitiesAllActivitiesFTE extends FacilitiesAlgorithm {

	private static Logger log = Logger.getLogger(FacilitiesAllActivitiesFTE.class);

	private final static String TEMPORARY_FACILITY_ID_SEPARATOR = "_";

	private EnterpriseCensus myCensus;
	private TreeMap<String, String> facilityActivities = new TreeMap<String, String>();

	private FacilitiesProductionKTI.KTIYear ktiYear;
	
	public FacilitiesAllActivitiesFTE(KTIYear ktiYear) {
		super();
		this.ktiYear = ktiYear;
	}

	@Override
	public void run(Facilities facilities) {

//		this.createThem2008();
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
		
		myCensus.printPresenceCodesReport();
		myCensus.printHectareAggregationReport();
		
		log.info("Reading enterprise census files into EnterpriseCensus object...done.");
	}
	
	private void createThem(Facilities facilities) {

		this.loadFacilityActivities();

		TreeMap<String, Integer> tempFacilities = new TreeMap<String, Integer>();

		// in the enterprise census there are 4 size classes of facilities
		TreeMap<Integer, Integer> minFTEsPerFacility = new TreeMap<Integer, Integer>();
		minFTEsPerFacility.put(1, 0); // 0-9 fulltime equivalents
		minFTEsPerFacility.put(2, 10); // 10-49 fulltime equivalents
		minFTEsPerFacility.put(3, 50); // 50-249 fulltime equivalents
		minFTEsPerFacility.put(4, 250); // >250 fulltime equivalents
		int minFTEs = Integer.MIN_VALUE;

		TreeMap<Integer, Integer> maxFTEsPerFacility = new TreeMap<Integer, Integer>();
		// commented out you see the correct ones, the used ones are +1 because of rounding
		// but I think the error is minimal (in fact it is completely irrelevant but I am a perfectionist)
//		maxFTEsPerFacility.put(1, 9); // 0-9 fulltime equivalents
//		maxFTEsPerFacility.put(2, 49); // 10-49 fulltime equivalents
//		maxFTEsPerFacility.put(3, 249); // 50-249 fulltime equivalents
//		maxFTEsPerFacility.put(4, Integer.MAX_VALUE); // >250 fulltime equivalents
		maxFTEsPerFacility.put(1, 10); // 0-9 fulltime equivalents
		maxFTEsPerFacility.put(2, 50); // 10-49 fulltime equivalents
		maxFTEsPerFacility.put(3, 250); // 50-249 fulltime equivalents
		maxFTEsPerFacility.put(4, Integer.MAX_VALUE); // >250 fulltime equivalents
//		int maxFTEs = Integer.MIN_VALUE;

		// after we will have distributed the minimum FTEs,
		// we randomly distribute the remaining ones
		// by adding a fixed additional number
		// until
		// a, there are no FTEs left or
		// b, the facility is "full"
		// we add a number proportional to the range
		TreeMap<Integer, Integer> additionalFTEsPerFacility = new TreeMap<Integer, Integer>();
		for (int i=1; i<=3; i++) {
			additionalFTEsPerFacility.put(
					i,
					(maxFTEsPerFacility.get(i) - minFTEsPerFacility.get(i) + 1) / 10);
		}
		// this choice is not probably not consistent with the previous ones, because theres no upper limit
		additionalFTEsPerFacility.put(4, 10); // >250 fulltime equivalents
		int additionalFTEs = Integer.MIN_VALUE;

		int hectareCnt = 0, facilityCnt = 0;
		int skip = 1;
		int numFacilities, numFTEs, numSectorFTE = Integer.MAX_VALUE, sizeRange;
		String X, Y, attributeId, tempFacilityId, activityId;
		Facility f;
		Activity a;

		loadCensus();
		
		System.out.println("  creating facilities... ");
		Set<Double> ecHectares = this.myCensus.getHectareAggregation().keySet();
//		Set<String> ecHectares = this.myCensus.getHectareAggregationKeys();

		TreeSet<String> sector2_attributeIds = this.myCensus.getHectareAttributeIdentifiersBySector(2);
		TreeSet<String> sector3_attributeIds = this.myCensus.getHectareAttributeIdentifiersBySector(3);
		Iterator<String> attributeIds_it = null;

		for (Double reli : ecHectares) {
			X = Integer.toString(this.myCensus.getHectareAggregationInformationFloor(reli, "X"));
			Y = Integer.toString(this.myCensus.getHectareAggregationInformationFloor(reli, "Y"));

			for (int sector = 2; sector <=3; sector++) {

				if (sector == 2) {
					numSectorFTE = this.myCensus.getHectareAggregationInformationFloor(reli, "B01EQTS2");
					attributeIds_it = sector2_attributeIds.iterator();
				} else if (sector == 3) {
					numSectorFTE = this.myCensus.getHectareAggregationInformationFloor(reli, "B01EQTS3");
					attributeIds_it = sector3_attributeIds.iterator();
				}
				if (numSectorFTE == Integer.MAX_VALUE) {
					Gbl.errorMsg("numFTE was not correctly set.");
				}

				// create temporary facilities with minimum number of FTEs
				while (attributeIds_it.hasNext()) {
					attributeId = attributeIds_it.next();
					numFacilities = this.myCensus.getHectareAggregationInformationFloor(reli, attributeId);
					// assign minimum work capacity, here number of fulltime equivalents
					sizeRange =	Integer.parseInt(attributeId.substring(attributeId.length() - 1));
					minFTEs = (minFTEsPerFacility.get(sizeRange)).intValue();

					// create temporary facilities, set minimum FTEs
					for (int i=0; i < numFacilities; i++) {

						tempFacilityId = this.createTemporaryFacilityID(facilityCnt++, attributeId);
						//System.out.println("Creating temporary " + facilityId + "...");
						tempFacilities.put(tempFacilityId, minFTEs);
						numSectorFTE -= minFTEs;
						// the number of distributed FTEs should not exceed the number of available ones
						if (numSectorFTE < 0) {
							Gbl.errorMsg("numFTE exceeded.");
						}
					}

				}

				// distribute remaining FTEs to temporary facilities
				Object[] tempFacilitiesIds = tempFacilities.keySet().toArray();
				while (numSectorFTE > 0) {
					// choose a random temp facility
					tempFacilityId = (String) tempFacilitiesIds[Gbl.random.nextInt(tempFacilitiesIds.length)];
					// determine the size range
					sizeRange = Integer.parseInt(tempFacilityId.substring(tempFacilityId.length() - 1));
					// put in fixed additional number of FTEs, but consider upper limit, and remaining numSectorFTEs
					numFTEs = tempFacilities.get(tempFacilityId);
					//System.out.println("Current: " + numFTEs);
					additionalFTEs = Math.min(
							additionalFTEsPerFacility.get(sizeRange),
							maxFTEsPerFacility.get(sizeRange) - numFTEs);
					additionalFTEs = Math.min(additionalFTEs, numSectorFTE);
					//System.out.println("Additional: " + additionalFTEs);
					tempFacilities.put(tempFacilityId, numFTEs + additionalFTEs);
					numSectorFTE -= additionalFTEs;
					// again, the number of distributed FTEs should not exceed the number of available ones
					if (numSectorFTE < 0) {
						Gbl.errorMsg("numSectorFTE exceeded (numSectorFTE = " + Integer.toString(numSectorFTE) + ").");
					}

				}

				// create singleton Facility objects from temporary ones
				Iterator<String> tempFacilities_it = tempFacilities.keySet().iterator();
				while (tempFacilities_it.hasNext()) {
					tempFacilityId = tempFacilities_it.next();
					attributeId = this.getAttributeIdFromTemporaryFacilityID(tempFacilityId);
					f = facilities.createFacility(
							this.getNumberFromTemporaryFacilityID(tempFacilityId),
							X,
							Y);

					if (ktiYear.equals(KTIYear.KTI_YEAR_2007)) {
						// create the work activity and its capacity according to all the computation done before
						a = f.createActivity("work");
						a.setCapacity(tempFacilities.get(tempFacilityId));

						// create the other activities
						if (this.facilityActivities.containsKey(attributeId)) {
							activityId = this.facilityActivities.get(attributeId);
							a = f.createActivity(activityId);
						}
					} else if (ktiYear.equals(KTIYear.KTI_YEAR_2008)) {
						// na dann mach mal hier weiter
					}
					
				}

				tempFacilities.clear();

//				System.out.println(
//						"Remaining FTEs in sector " + new Integer(sector).toString() +
//						" of hectare " + new Integer(hectareCnt).toString() +
//						": " + new Integer(numSectorFTE).toString());
			}
			hectareCnt++;
			//System.out.println("\t\t\tProcessed " + hectareCnt + " hectares.");
			if ((hectareCnt % skip) == 0) {
				System.out.println("\t\t\tProcessed " + hectareCnt + " hectares.");
				skip *= 2;
			}
		}
		System.out.println("  creating facilities...DONE.");

//		System.out.println("  writing EnterpriseCensus object to output file... ");
//		EnterpriseCensusWriter myCensusWriter = new EnterpriseCensusWriter();
//		try {
//			myCensusWriter.write(myCensus);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		System.out.println("  done.");
	}

//	private void testTemporaryFacilityIds() {
//
//		int number = 2000;
//		String attrId = "krassomat";
//		String tempFacId = this.createTemporaryFacilityID(number, attrId);
//
//		System.out.println(
//				tempFacId + System.getProperty("line.separator") +
//				this.getNumberFromTemporaryFacilityID(tempFacId) + System.getProperty("line.separator") +
//				this.getAttributeIdFromTemporaryFacilityID(tempFacId));
//
//	}

	private String createTemporaryFacilityID(final int number, final String attributeId) {

		return Integer.toString(number) + TEMPORARY_FACILITY_ID_SEPARATOR + attributeId;

	}


	private String getNumberFromTemporaryFacilityID(final String temporaryFacilityId) {

		return temporaryFacilityId.substring(
				0,
				temporaryFacilityId.indexOf(TEMPORARY_FACILITY_ID_SEPARATOR)
				);

	}
	private String getAttributeIdFromTemporaryFacilityID(final String temporaryFacilityId) {

		return temporaryFacilityId.substring(
				temporaryFacilityId.indexOf(TEMPORARY_FACILITY_ID_SEPARATOR) + 1,
				temporaryFacilityId.length()
				);

	}

	private void loadFacilityActivities() {

		// shop
		// "52: Detailhandel; Reparatur von Gebrauchsgütern"
		this.facilityActivities.put("B015201", "shop");
		this.facilityActivities.put("B015202", "shop");
		this.facilityActivities.put("B015203", "shop");
		this.facilityActivities.put("B015204", "shop");

		// education
		// "80: Unterrichtswesen"
		this.facilityActivities.put("B018001", "education");
		this.facilityActivities.put("B018002", "education");
		this.facilityActivities.put("B018003", "education");
		this.facilityActivities.put("B018004", "education");

		// leisure
		// "55: Gastgewerbe"
		this.facilityActivities.put("B015501", "leisure");
		this.facilityActivities.put("B015502", "leisure");
		this.facilityActivities.put("B015503", "leisure");
		this.facilityActivities.put("B015504", "leisure");

		// "92: Unterhaltung, Kultur und Sport"
		this.facilityActivities.put("B019201", "leisure");
		this.facilityActivities.put("B019202", "leisure");
		this.facilityActivities.put("B019203", "leisure");
		this.facilityActivities.put("B019204", "leisure");

	}
}
