/* *********************************************************************** *
 * project: org.matsim.*
 * FacilitiesProduction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.staheale.preprocess;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;

import playground.staheale.preprocess.AgentInteractionEnterpriseCensus.ProductionSector;
import playground.staheale.preprocess.FacilitiesProduction.KTIYear;




public class FacilitiesCreation { 

	private TreeMap<String, String> facilityActivities = new TreeMap<String, String>();

	private static Logger log = Logger.getLogger(FacilitiesCreation.class);

	private KTIYear ktiYear;

	private final static String TEMPORARY_FACILITY_ID_SEPARATOR = "_";

	private AgentInteractionEnterpriseCensus myCensus;

	public FacilitiesCreation(KTIYear ktiYear) {
		//super(null);
		this.ktiYear = ktiYear;

	}



	public void run(ActivityFacilitiesImpl facilities) {

		this.loadFacilityActivities();
		this.createThem(facilities);

	}

	private void loadCensus() {

		log.info("Reading enterprise census files into EnterpriseCensus object...");
		this.myCensus = new AgentInteractionEnterpriseCensus();

		AgentInteractionEnterpriseCensusParser myCensusParser = new AgentInteractionEnterpriseCensusParser(this.myCensus);
		try {
			myCensusParser.parse(this.myCensus);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//	myCensus.printPresenceCodesReport();
		//	myCensus.printHectareAggregationReport();

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
		//int skip = 1;
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
					throw new RuntimeException("numFTE was not correctly set.");
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
						//-------------TODO: setting id offset-----------------
						tempFacilityId = this.createTemporaryFacilityID(10000000+facilityCnt++, attributeId);
						//						System.out.println("Creating temporary " + tempFacilityId + "...");
						tempFacilities.put(tempFacilityId, minFTEs);
						numSectorFTE -= minFTEs;
						// the number of distributed FTEs should not exceed the number of available ones
						if (numSectorFTE < 0) {
							throw new RuntimeException("numFTE exceeded.");
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
						throw new RuntimeException("numSectorFTE exceeded (numSectorFTE = " + Integer.toString(numSectorFTE) + ").");
					}

				}

				// create singleton Facility objects from temporary ones
				Iterator<String> tempFacilities_it = tempFacilities.keySet().iterator();
				while (tempFacilities_it.hasNext()) {
					tempFacilityId = tempFacilities_it.next();
					attributeId = this.getAttributeIdFromTemporaryFacilityID(tempFacilityId);
					f = facilities.createAndAddFacility(
							Id.create(this.getNumberFromTemporaryFacilityID(tempFacilityId), ActivityFacility.class),
							new Coord(Double.parseDouble(X), Double.parseDouble(Y)));

					if (ktiYear.equals(KTIYear.KTI_YEAR_2007)) {
						// create the work activity and its capacity according to all the computation done before
						a = f.createActivityOption("work");
						a.setCapacity(Math.max(1,tempFacilities.get(tempFacilityId)));

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
							a = f.createActivityOption(FacilitiesProduction.WORK_SECTOR2);
							a.setCapacity(Math.max(1,tempFacilities.get(tempFacilityId)));
						} else if(sector.equals(ProductionSector.SECTOR3)) {
							a = f.createActivityOption(FacilitiesProduction.WORK_SECTOR3);
							a.setCapacity(Math.max(1,tempFacilities.get(tempFacilityId)));
						}

						if (this.facilityActivities.containsKey(activityId)) {
							a = f.createActivityOption(this.facilityActivities.get(activityId));
						}
						//--------------------shop retail capacity definition

						//shop retail capacity is assumed to depend on sales area where arbitrary 50% are subtracted for shelfs, cash points etc.,
						//then a value of 0.135 P/m2 is assumed as capacity limit;
						//for sales area a random pick in the given intervals is performed, except for get100 

						if (f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GT2500)) {
							double n = Math.round(0.0675*(random.nextInt(4500) + 2501));
							a.setCapacity(n);
							//log.info("capacity of shop retail gt2500 set to " +n);
						} else if(f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GET1000)) {
							double x = Math.round(0.0675*(1000 + random.nextInt(1501)));
							a.setCapacity(x);
							log.info("capacity of shop retail get1000 set to " +x);
						} else if(f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GET400)) {
							a.setCapacity(Math.round(0.0675*(400 + random.nextInt(600))));
						} else if(f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_GET100)) {
							double z = Math.round(0.0675*(100 + random.nextInt(300)));
							a.setCapacity(z);
							//log.info("capacity of shop retail get100 set to " +z);
						} else if(f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_LT100)) {
							a.setCapacity(20);
						}

						//shop retail other sales area is assumed to vary between 150 and 1000 m2

						else if(f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_RETAIL_OTHER)) {
							a.setCapacity(Math.round(0.0675*(150 + random.nextInt(851))));
						}

						//-------------------------shop service capacity definition

						//capacity is assumed to depend on the number of people working there, one employee can serve 1 customer, subtract 10% for vacancies and 20% for shift operation

						else if(f.getActivityOptions().containsKey(FacilitiesProduction.SHOP_SERVICE)) {
							double v = Math.max(1,Math.round((tempFacilities.get(tempFacilityId)*0.7)));
							a.setCapacity(v);
							//log.info("setting shop service capacity to " +v);
						}

						//-------------------------sports & fun capacity definition

						//bar, disco, dancings, arcades casino: capacity is assumed to depend on the number of people working there, one employee can serve between 10-20 customer, subtract 10% for vacancies and 20% for shift operation

						else if(f.getActivityOptions().containsKey("B015540A") || f.getActivityOptions().containsKey("B019234B") || f.getActivityOptions().containsKey("B019234C")|| f.getActivityOptions().containsKey("B019271A")) {
							double y = Math.max(1,Math.round((tempFacilities.get(tempFacilityId))*0.7*(10 + random.nextInt(11))));
							a.setCapacity(y);
							//log.info("setting capacity for bar, disco, etc. with a value of: " +y);
						}

						//dancing school, other sportive activities like tennis or golf schools: capacity is assumed to depend on the number of people working there, one employee can serve between 20-30 customer, subtract 10% for vacancies and 20% for shift operation

						else if(f.getActivityOptions().containsKey("B019234A") || f.getActivityOptions().containsKey("B019262B") || f.getActivityOptions().containsKey("B019272A")) {
							a.setCapacity(Math.max(1,Math.round((tempFacilities.get(tempFacilityId))*0.7*(20 + random.nextInt(11)))));
						}

						//operation of sport facilities: 

						else if(f.getActivityOptions().containsKey("B019261A")) {
							a.setCapacity(Math.round(30+(0.7*(tempFacilities.get(tempFacilityId)))*(0.7*(tempFacilities.get(tempFacilityId)))));
							//	log.info("setting capacity for sport facilities");

						}

						//sport clubs:

						else if(f.getActivityOptions().containsKey("B019262A")) {
							a.setCapacity(Math.round(20+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(1))));
							//	log.info("setting capacity for sport club");
						}

						//sauna, solarium, gym, thermal bath, etc.:

						else if(f.getActivityOptions().containsKey("B019304A")|| f.getActivityOptions().containsKey("B019304B") || f.getActivityOptions().containsKey("B019304C")) {
							a.setCapacity(Math.max(1,Math.round((tempFacilities.get(tempFacilityId))*0.7*(2 + random.nextInt(9)))));
							//	log.info("setting capacity for sauna, solarium, gym, etc.");
						}

						//amusement parks:

						else if(f.getActivityOptions().containsKey("B019233A")) {
							a.setCapacity(Math.round(100+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(25))));
						}

						//-------------------------gastro & culture capacity definition

						//restaurant, canteen: arbitrary set to vary between 25 and 200

						else if(f.getActivityOptions().containsKey("B015530A") || f.getActivityOptions().containsKey("B015551A")) {
							a.setCapacity(Math.max(1,Math.min(1000,Math.round((tempFacilities.get(tempFacilityId))*0.7*(10 + random.nextInt(11))))));
						}

						//cinema:

						else if(f.getActivityOptions().containsKey("B019213A")) {
							a.setCapacity(Math.max(1,Math.min(800,Math.round(200+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(8))))));
						}

						//theater, orchestra, circus, etc.:

						else if(f.getActivityOptions().containsKey("B019231A") || f.getActivityOptions().containsKey("B019231B") || f.getActivityOptions().containsKey("B019234D")) {
							a.setCapacity(Math.round(50+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(8))));
						}

						//libraries:

						else if(f.getActivityOptions().containsKey("B019251A")) {
							a.setCapacity(Math.round(20+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(5))));
						}

						//museum:

						else if(f.getActivityOptions().containsKey("B019252A")) {
							a.setCapacity(Math.round(50+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(8))));
						}

						//zoo, gardens, natural parks: arbitrary set to vary between 50 and 1000

						else if(f.getActivityOptions().containsKey("B019253A")) {
							a.setCapacity(Math.round(50+(tempFacilities.get(tempFacilityId))*0.7*(1 + random.nextInt(15))));
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

		//------------------new classification for KTI_YEAR_2008
		if (this.ktiYear.equals(KTIYear.KTI_YEAR_2008)) {

			// education
			for (String str : new String[]{"B018010A"}) {
				this.facilityActivities.put(
						str,
						FacilitiesProduction.EDUCATION_KINDERGARTEN);
			}

			for (String str : new String[]{"B018010B"}) {
				this.facilityActivities.put(
						str,
						FacilitiesProduction.EDUCATION_PRIMARY);
			}

			for (String str : new String[]{"B018021A", "B018021B", "B018021C", "B018022A"}) {
				this.facilityActivities.put(
						str,
						FacilitiesProduction.EDUCATION_SECONDARY);
			}

			for (String str : new String[]{"B018030A", "B018030B", "B018030C", "B018030D"}) {
				this.facilityActivities.put(
						str,
						FacilitiesProduction.EDUCATION_HIGHER);
			}

			for (String str : new String[]{"B018041A", "B018042A", "B018042B", "B018042C", "B018042D", "B018042E"}) {
				this.facilityActivities.put(
						str,
						FacilitiesProduction.EDUCATION_OTHER);
			}

			// shopping
			for (String str : new String[]{"11A"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +
						str,
						FacilitiesProduction.SHOP_RETAIL_GT2500);
			}

			for (String str : new String[]{"11B"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +
						str,
						FacilitiesProduction.SHOP_RETAIL_GET1000);
			}

			for (String str : new String[]{"11C"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +
						str,
						FacilitiesProduction.SHOP_RETAIL_GET400);
			}

			for (String str : new String[]{"11D"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +
						str,
						FacilitiesProduction.SHOP_RETAIL_GET100);
			}

			for (String str : new String[]{"11E"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +
						str,
						FacilitiesProduction.SHOP_RETAIL_LT100);
			}

			// new classification: SHOP_RETAIL_OTHER
			for (String str : new String[]{
					"12A","12B",
					"21A","22A","23A","24A","25A","26A","27A","27B",
					"31A","32A","33A","33B",
					"41A","42A","42B","42C","42D","42E","43A","43B","44A","44B","44C","45A","45B","45C","45D","45E","46A","46B","47A","47B","47C","48A","48B","48C","48D","48E","48F","48G","48H","48I","48J","48K","48L","48M","48N","48O","48P",
					"50A","50B"
					//,"61A","62A","63A"-->retail shopping not in sales rooms!
					//,"71A","72A","73A","74A"-->repair stores now in shop service class!
			}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +
						str,
						FacilitiesProduction.SHOP_RETAIL_OTHER);
			}

			// new classification: SHOP_SERVICE
			for (String str : new String[]{"71A","72A","73A","74A"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.SHOP_NOGA_SECTION +//52
						str,
						FacilitiesProduction.SHOP_SERVICE); //CHANGED
			}
			for (String str : new String[]{"01A","02A","02B","05A"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.PERSONAL_SERVICE_NOGA_SECTION + //93
						str,
						FacilitiesProduction.SHOP_SERVICE);
			}

			// new classification: SPORTS_FUN
			for (String str : new String[]{"40A",}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.HOSPITALITY_NOGA_SECTION +//55
						str,
						FacilitiesProduction.SPORTS_FUN);
			}
			for (String str : new String[]{"33A","34A","34B","34C",
					"61A","62A","62B","71A","72A"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.CULTURE_NOGA_SECTION +//92
						str,
						FacilitiesProduction.SPORTS_FUN);
			}
			for (String str : new String[]{"04A","04B","04C"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.PERSONAL_SERVICE_NOGA_SECTION +//93
						str,
						FacilitiesProduction.SPORTS_FUN);
			}

			// new classification: GASTRO_CULTURE
			for (String str : new String[]{"30A","51A"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.HOSPITALITY_NOGA_SECTION +
						str,
						FacilitiesProduction.GASTRO_CULTURE);
			}
			for (String str : new String[]{"13A","31A","31B","34D","51A","52A","53A"}) {
				this.facilityActivities.put(
						AgentInteractionEnterpriseCensus.EC01_PREFIX +
						AgentInteractionEnterpriseCensus.CULTURE_NOGA_SECTION +
						str,
						FacilitiesProduction.GASTRO_CULTURE);
			}
		}
		for (String str : facilityActivities.keySet()) {
			System.out.println(str + "\t|\t" + facilityActivities.get(str));
		}

	}

}
