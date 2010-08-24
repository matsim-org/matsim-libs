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

package playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ.FacilitiesProductionKTI.KTIYear;
import playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ.entreprisecensus.EnterpriseCensus;
import playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ.entreprisecensus.EnterpriseCensusParser;
import playground.anhorni.locationchoice.preprocess.facilities.facilitiescreation.fromBZ.entreprisecensus.EnterpriseCensus.ProductionSector;

public class FacilitiesAllActivitiesFTE {

	private static Logger log = Logger.getLogger(FacilitiesAllActivitiesFTE.class);

	private final static String TEMPORARY_FACILITY_ID_SEPARATOR = "_";

	private EnterpriseCensus myCensus;
	private TreeMap<String, String> facilityActivities = new TreeMap<String, String>();

	private FacilitiesProductionKTI.KTIYear ktiYear;

	public FacilitiesAllActivitiesFTE(KTIYear ktiYear2) {
		super();
		this.ktiYear = ktiYear2;
	}

	public void run(ActivityFacilitiesImpl facilities) {
		this.loadFacilityActivities();
		this.createThem(facilities);
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

	private void createThem(ActivityFacilitiesImpl facilities) {

		Random random = new Random(4711);

		// see http://www.matsim.org/node/36 for the next step
		random.nextInt();

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
		String X, Y, attributeId, tempFacilityId, activityId = null;
		HashSet<String> presenceCodeItems = null;
		ActivityFacilityImpl f;
		ActivityOptionImpl a;

		loadCensus();

		System.out.println("  creating facilities... ");
		Set<Integer> ecHectares = this.myCensus.getHectareAggregation().keySet();

		Iterator<String> attributeIds_it = null;

		for (Integer reli : ecHectares) {
			X = Integer.toString((int) this.myCensus.getHectareAggregationInformation(reli, "X"));
			Y = Integer.toString((int) this.myCensus.getHectareAggregationInformation(reli, "Y"));

			for (ProductionSector sector : ProductionSector.values()) {

				numSectorFTE = (int) Math.round(this.myCensus.getHectareAggregationInformation(reli, sector.getFteItem()));
				attributeIds_it = this.myCensus.getHectareAttributeIdentifiersBySector(sector).iterator();
					
				if (numSectorFTE == Integer.MAX_VALUE) {
					log.info("numFTE was not correctly set.");
				}

				// create temporary facilities with minimum number of FTEs
				while (attributeIds_it.hasNext()) {
					attributeId = attributeIds_it.next();
					numFacilities = (int) this.myCensus.getHectareAggregationInformation(reli, attributeId);
					// assign minimum work capacity, here number of fulltime equivalents
					sizeRange =	Integer.parseInt(attributeId.substring(attributeId.length() - 1));
					minFTEs = (minFTEsPerFacility.get(sizeRange)).intValue();

					// create temporary facilities, set minimum FTEs
					for (int i=0; i < numFacilities; i++) {

						tempFacilityId = this.createTemporaryFacilityID(facilityCnt++, attributeId);
//						System.out.println("Creating temporary " + tempFacilityId + "...");
						tempFacilities.put(tempFacilityId, minFTEs);
						numSectorFTE -= minFTEs;
						// the number of distributed FTEs should not exceed the number of available ones
						if (numSectorFTE < 0) {
							log.info("numFTE exceeded.");
						}
					}

				}

				// distribute remaining FTEs to temporary facilities
				Object[] tempFacilitiesIds = tempFacilities.keySet().toArray();
				while (numSectorFTE > 0) {
					// choose a random temp facility
					tempFacilityId = (String) tempFacilitiesIds[MatsimRandom.getRandom().nextInt(tempFacilitiesIds.length)];
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
						log.info("numSectorFTE exceeded (numSectorFTE = " + Integer.toString(numSectorFTE) + ").");
					}

				}

				// create singleton Facility objects from temporary ones
				Iterator<String> tempFacilities_it = tempFacilities.keySet().iterator();
				while (tempFacilities_it.hasNext()) {
					tempFacilityId = tempFacilities_it.next();
					attributeId = this.getAttributeIdFromTemporaryFacilityID(tempFacilityId);
					f = facilities.createFacility(
							new IdImpl(this.getNumberFromTemporaryFacilityID(tempFacilityId)),
							new CoordImpl(X, Y));

					if (ktiYear.equals(KTIYear.KTI_YEAR_2007)) {
						// create the work activity and its capacity according to all the computation done before
						a = f.createActivityOption("work");
						a.setCapacity(tempFacilities.get(tempFacilityId));

						// create the other activities
						if (this.facilityActivities.containsKey(attributeId)) {
							activityId = this.facilityActivities.get(attributeId);
							a = f.createActivityOption(activityId);
						}
					} else if (ktiYear.equals(KTIYear.KTI_YEAR_2008)) {

						//						System.out.println("attributeId: " + attributeId);
						// usually more than one presence code is available,
						// or there are multiple facilities with the same presence code, clearly
						// without marginal sums over the presence codes we cannot find out
						// the correct code (this is intended in the presence code concept)
						// so...let's randomly chose a presence code...
						presenceCodeItems = myCensus.getPresenceCodeItemsPerNOGASection(reli, attributeId);
						activityId = (String) presenceCodeItems.toArray()[random.nextInt(presenceCodeItems.size())];

						// let's put the presence code in as an activity,
						// so one can refer to it when modeling something with activities
						a = f.createActivityOption(activityId);

						// create the work activity, because the enterprise census is a directory of workplaces
						// set the capacity to the number of fulltime equivalents revealed
						if (sector.equals(ProductionSector.SECTOR2)) {
							a = f.createActivityOption(FacilitiesProductionKTI.WORK_SECTOR2);
						} else if(sector.equals(ProductionSector.SECTOR3)) {
							a = f.createActivityOption(FacilitiesProductionKTI.WORK_SECTOR3);
						}
						a.setCapacity(tempFacilities.get(tempFacilityId));

						// add more activities as planned for KTI Year 2008
						if (this.facilityActivities.containsKey(activityId)) {
							a = f.createActivityOption(this.facilityActivities.get(activityId));
							// for all activity types assume the same simple thing
							// one worker (teacher, salesman, sports facility employee) can at maximum serve 10-20 clients (pupils, costumers, trainees)
							// so capacity depends on the number of people working somewhere
							// this is especially opposed to using sales area as trip attraction/trip production parameter
							// as used in established transport models
							a.setCapacity(tempFacilities.get(tempFacilityId) * (10 + random.nextInt(10)));
						}
					}

				}

				tempFacilities.clear();

//				System.out.println(
//				"Remaining FTEs in sector " + new Integer(sector).toString() +
//				" of hectare " + new Integer(hectareCnt).toString() +
//				": " + new Integer(numSectorFTE).toString());
			}

			hectareCnt++;
//			if ((hectareCnt % skip) == 0) {
//			log.info("Processed " + hectareCnt + " hectares.");
//			skip *= 2;
//			}
		}
		log.info("Processed " + hectareCnt + " hectares.");
		log.info("creating facilities...DONE.");

	}

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

		if (this.ktiYear.equals(KTIYear.KTI_YEAR_2007)) {

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

		} else if (this.ktiYear.equals(KTIYear.KTI_YEAR_2008)) {

			// education
			for (String str : new String[]{"B018010A"}) {
				this.facilityActivities.put(
						str, 
						FacilitiesProductionKTI.EDUCATION_KINDERGARTEN);
			}

			for (String str : new String[]{"B018010B"}) {
				this.facilityActivities.put(
						str, 
						FacilitiesProductionKTI.EDUCATION_PRIMARY);
			}

			for (String str : new String[]{"B018021A", "B018021B", "B018021C", "B018022A"}) {
				this.facilityActivities.put(
						str, 
						FacilitiesProductionKTI.EDUCATION_SECONDARY);
			}

			for (String str : new String[]{"B018030A", "B018030B", "B018030C", "B018030D"}) {
				this.facilityActivities.put(
						str, 
						FacilitiesProductionKTI.EDUCATION_HIGHER);
			}

			for (String str : new String[]{"B018041A", "B018042A", "B018042B", "B018042C", "B018042D", "B018042E"}) {
				this.facilityActivities.put(
						str, 
						FacilitiesProductionKTI.EDUCATION_OTHER);
			}

			// shopping
			for (String str : new String[]{"11A"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.SHOP_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.SHOP_RETAIL_GT2500);
			}

			for (String str : new String[]{"11B"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.SHOP_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.SHOP_RETAIL_GET1000);
			}

			for (String str : new String[]{"11C"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.SHOP_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.SHOP_RETAIL_GET400);
			}

			for (String str : new String[]{"11D"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.SHOP_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.SHOP_RETAIL_GET100);
			}

			for (String str : new String[]{"11E"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.SHOP_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.SHOP_RETAIL_LT100);
			}

			for (String str : new String[]{
					"12A","12B",
					"21A","22A","23A","24A","25A","26A","27A","27B",
					"31A","32A","33A","33B",
					"41A","42A","42B","42C","42D","42E","43A","43B","44A","44B","44C","45A","45B","45C","45D","45E","46A","46B","47A","47B","47C","48A","48B","48C","48D","48E","48F","48G","48H","48I","48J","48K","48L","48M","48N","48O","48P",
					"50A","50B",
					"61A","62A","63A",
					"71A","72A","73A","74A"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.SHOP_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.SHOP_OTHER);
			}

			// leisure
			for (String str : new String[]{"30A", "40A", "51A", "52A"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.HOSPITALITY_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.LEISURE_GASTRO);
			}

			for (String str : new String[]{"11A","12A","21A","22A","23A","23B","23C"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX +
						EnterpriseCensus.HOSPITALITY_NOGA_SECTION + 
						str, 
//						FacilitiesProductionKTI.LEISURE_HOSPITALITY);	
						FacilitiesProductionKTI.LEISURE_GASTRO);
			}

			for (String str : new String[]{"B019261A", "B019262A", "B019262B"}) {
				this.facilityActivities.put(
						str, 
						FacilitiesProductionKTI.LEISURE_SPORTS);
			}

			for (String str : new String[]{
					"11A","12A","13A",
					"20A","20B",
					"31A","31B","31C","31D","32A","32B","33A","34A","34B","34C","34D",
					"40A","40B",
					"51A","52A","53A"}) {
				this.facilityActivities.put(
						EnterpriseCensus.EC01_PREFIX + 
						EnterpriseCensus.CULTURE_NOGA_SECTION + 
						str, 
						FacilitiesProductionKTI.LEISURE_CULTURE);
			}
		}
		for (String str : facilityActivities.keySet()) {
			System.out.println(str + "\t|\t" + facilityActivities.get(str));
		}
	}
}
