/* *********************************************************************** *
 /* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionAnalysisModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
 *
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;


/**
 * 2 categories for distance driven AFTER coldstart:
 * <ul>
 * <li> 0 - 1 km </li>
 * <li> 1 - 2 km </li>
 * </ul>
 *
 * 13 categories for parking time BEFORE coldstart:
 * <ul>
 * <li> 0 - 1 h [1]</li>
 * <li> 1 - 2 h [2]</li>
 * <li> ... </li>
 * <li> 11 - 12 h [12]</li>
 * <li> > 12 h [13]</li>
 * </ul>
 *
 * Remarks:
 * <ul>
 * <li>HBEFA 3.1 does not provide further distance categories for cold start emission factors when average amient temperature is assumed <br>
 * <li>HBEFA 3.1 does not provide cold start emission factors for Heavy Goods Vehicles; thus, HGV are assumed to produce the same cold start emission factors as passenger cars <br>
 * <li>In the current implementation, vehicles emit one part of their cold start emissions when the engine is started (distance class 0 - 1 km);
 * after reaching 1 km, the rest of their cold start emissions is emitted (difference between distance class 1 - 2 km and distance class 0 - 1 km)
 * </ul>
 *
 *
 * @author benjamin
 */
final class ColdEmissionAnalysisModule {
	private static final Logger logger = LogManager.getLogger(ColdEmissionAnalysisModule.class);

	private final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable;
	private final Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable;

	private final EventsManager eventsManager;
	private final EmissionsConfigGroup ecg;

	private final Set<Pollutant> coldPollutants;

	private int detailedReadingInfoCnt = 0;
	private int detailedTransformToHbefa4Cnt = 0;
	private int detailedFallbackTechAverageWarnCnt = 0;
	private int detailedFallbackAverageTableWarnCnt = 0;
	private int averageReadingInfoCnt = 0;
	private int vehInfoWarnHDVCnt = 0;
	private static final int maxWarnCnt = 3;

	/*package-private*/ ColdEmissionAnalysisModule( Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> avgHbefaColdTable,
													Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> detailedHbefaColdTable, EmissionsConfigGroup ecg,
													Set<Pollutant> coldPollutants, EventsManager eventsManager ){

		Gbl.assertIf( avgHbefaColdTable!=null || detailedHbefaColdTable!=null );
		this.avgHbefaColdTable = avgHbefaColdTable;
		this.detailedHbefaColdTable = detailedHbefaColdTable;
		this.ecg = ecg;
		this.coldPollutants = coldPollutants;

		Gbl.assertNotNull( eventsManager );
		this.eventsManager = eventsManager;
	}

	/**
	 *  yy I do not know what the "W" in the method name means.  Normally, it means "With".  But that does not make sense here.  kai, dec'22
	 */
	/*package-private*/ Map<Pollutant, Double> checkVehicleInfoAndCalculateWColdEmissions(
			VehicleType vehicleType, Id<Vehicle> vehicleId, Id<Link> coldEmissionEventLinkId,
			double eventTime, double parkingDuration, int distance_km) {
		{
			String hbefaVehicleTypeDescription = EmissionUtils.getHbefaVehicleDescription( vehicleType, this.ecg );
			// (this will, importantly, repair the hbefa description in the vehicle type. kai/kai, jan'20)
			Gbl.assertNotNull( hbefaVehicleTypeDescription );
		}
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = EmissionUtils.convertVehicleDescription2VehicleInformationTuple(vehicleType );
		Gbl.assertNotNull( vehicleInformationTuple );

		if (vehicleInformationTuple.getFirst() == null){
			throw new RuntimeException("Vehicle category for vehicle " + vehicleType + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " +
					EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}

		return calculateColdEmissions(vehicleId, parkingDuration, vehicleInformationTuple, distance_km);
	}

	/*package-private*/ void throwColdEmissionEvent(Id<Vehicle> vehicleId, Id<Link> coldEmissionEventLinkId, double eventTime, Map<Pollutant, Double> coldEmissions) {
		Event coldEmissionEvent = new ColdEmissionEvent(eventTime, coldEmissionEventLinkId, vehicleId, coldEmissions);
		this.eventsManager.processEvent(coldEmissionEvent);
	}

	private static int cnt =10;
	private Map<Pollutant, Double> calculateColdEmissions(Id<Vehicle> vehicleId, double parkingDuration, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, int distance_km ) {

		final Map<Pollutant, Double> coldEmissionsOfEvent = new EnumMap<>( Pollutant.class );

		logger.debug("VehId: {} ; Tuple.first = {}", vehicleId, vehicleInformationTuple.getFirst());
		// fallback vehicle types that we cannot or do not want to map onto a hbefa vehicle type:
		if ( vehicleInformationTuple.getFirst()==HbefaVehicleCategory.NON_HBEFA_VEHICLE ) {
			for ( Pollutant coldPollutant : coldPollutants) {
				coldEmissionsOfEvent.put( coldPollutant, 0.0 );
				// yyyyyy todo replace by something more meaningful. kai, jan'20
			}
			if ( cnt >0 ) {
				logger.warn( "Just encountered non hbefa vehicle; currently, this code is setting the emissions of such vehicles to zero.  " +
						"Might be necessary to find a better solution for this.  kai, jan'20" );
				cnt--;
				if ( cnt ==0 ) {
					logger.warn( Gbl.FUTURE_SUPPRESSED );
				}
			}
			return coldEmissionsOfEvent;
		}

		// translate vehicle information type into factor key.  yyyy maybe combine these two? kai, jan'20
		HbefaColdEmissionFactorKey key = new HbefaColdEmissionFactorKey();
		key.setVehicleCategory(vehicleInformationTuple.getFirst());

		//HBEFA 3 provides cold start emissions for "pass. car" and Light_Commercial_Vehicles (LCV) only.
		//HBEFA 4.1 provides cold start emissions for "pass. car" and Light_Commercial_Vehicles (LCV) only.
		//see https://www.hbefa.net/e/documents/HBEFA41_Development_Report.pdf (WP 4 , page 23)  kturner, may'20
		if (vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE) ||
			vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.COACH) ||
			vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.URBAN_BUS) ||
			vehicleInformationTuple.getFirst().equals(HbefaVehicleCategory.MOTORCYCLE)) {
			if (vehInfoWarnHDVCnt < maxWarnCnt) {
				vehInfoWarnHDVCnt++;
				logger.warn("Automagic changing of VehCategory is disabled. Please make sure that your table contains the necessary values for {}", HbefaVehicleCategory.HEAVY_GOODS_VEHICLE.name());
				if (vehInfoWarnHDVCnt == maxWarnCnt) logger.warn(Gbl.FUTURE_SUPPRESSED);
			}
		}

		if(this.detailedHbefaColdTable != null) {
			HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
			key.setVehicleAttributes(hbefaVehicleAttributes);
		}

		int parkingDuration_h = Math.max(1, (int) (parkingDuration / 3600));
		if (parkingDuration_h >= 12) parkingDuration_h = 13;

		key.setParkingTime(parkingDuration_h);

		for (Pollutant coldPollutant : coldPollutants) {
			double generatedEmissions;
			// this is a really weird logic. Probably a million ways how this could fail janek jan'21
			if (distance_km == 1) {
				generatedEmissions = getEmissionsFactor(vehicleInformationTuple, 1, key, coldPollutant).getFactor();
			} else {
				generatedEmissions = getEmissionsFactor(vehicleInformationTuple, 2, key, coldPollutant).getFactor() - getEmissionsFactor(vehicleInformationTuple, 1, key, coldPollutant).getFactor();
			}
			coldEmissionsOfEvent.put(coldPollutant, generatedEmissions);
		}
		return coldEmissionsOfEvent;
	}

	private HbefaColdEmissionFactor getEmissionsFactor(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, int distance_km, HbefaColdEmissionFactorKey efkey, Pollutant coldPollutant) {

		efkey.setDistance(distance_km);

		efkey.setComponent(coldPollutant);
		efkey.setVehicleAttributes(vehicleInformationTuple.getSecond());

		switch (ecg.getDetailedVsAverageLookupBehavior()) {
			case onlyTryDetailedElseAbort:
				if (detailedReadingInfoCnt <= 1) {
					logger.info("try reading detailed values");
					logger.info(Gbl.ONLYONCE);
					logger.info(Gbl.FUTURE_SUPPRESSED);
					detailedReadingInfoCnt++;
				}
				if (this.detailedHbefaColdTable.get(efkey) != null) {
					HbefaColdEmissionFactor ef = this.detailedHbefaColdTable.get(efkey);
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					return ef;
				} else {
					if (detailedTransformToHbefa4Cnt <= 1) {
						logger.info("try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again");
						logger.info(Gbl.ONLYONCE);
						logger.info(Gbl.FUTURE_SUPPRESSED);
						detailedTransformToHbefa4Cnt++;
					}
					HbefaColdEmissionFactorKey efkey2 = new HbefaColdEmissionFactorKey(efkey);
					HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4(vehicleInformationTuple);
					// put this into a new key ...
					efkey2.setVehicleAttributes(attribs2);
					// ... and try to look up:
					if (this.detailedHbefaColdTable.get(efkey2) != null) {
						HbefaColdEmissionFactor ef2 = this.detailedHbefaColdTable.get(efkey2);
						logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
						return ef2;
					}
				}
				break;
			case tryDetailedThenTechnologyAverageElseAbort:
				//Look up detailed values
				if (detailedReadingInfoCnt <= 1) {
					logger.info("try reading detailed values");
					logger.info(Gbl.ONLYONCE);
					logger.info(Gbl.FUTURE_SUPPRESSED);
					detailedReadingInfoCnt++;
				}
				if (this.detailedHbefaColdTable.get(efkey) != null) {
					HbefaColdEmissionFactor ef = this.detailedHbefaColdTable.get(efkey);
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					return ef;
				} else {
					if (detailedTransformToHbefa4Cnt <= 1) {
						logger.info("try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again");
						logger.info(Gbl.ONLYONCE);
						logger.info(Gbl.FUTURE_SUPPRESSED);
						detailedTransformToHbefa4Cnt++;
					}
					HbefaColdEmissionFactorKey efkey2 = new HbefaColdEmissionFactorKey(efkey);
					HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4(vehicleInformationTuple);
					// put this into a new key ...
					efkey2.setVehicleAttributes(attribs2);
					// ... and try to look up:
					if (this.detailedHbefaColdTable.get(efkey2) != null) {
						HbefaColdEmissionFactor ef2 = this.detailedHbefaColdTable.get(efkey2);
						logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
						return ef2;
					}

					//if not possible, try "<technology>; average; average":
					if (ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort || ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable) {
						attribs2.setHbefaSizeClass("average");
						attribs2.setHbefaEmConcept("average");
						if (detailedFallbackTechAverageWarnCnt <= 1) {
							logger.warn("Did not find emission factor in the detailed-table for efkey={}", efkey);
							logger.warn("We are now trying to search in the detailed-table, but with an technology-averaged-key : efkey was re-written to {}", efkey2);
							logger.warn("will try it with '<technology>; average; average'");
							logger.warn(Gbl.ONLYONCE);
							logger.warn(Gbl.FUTURE_SUPPRESSED);
							detailedFallbackTechAverageWarnCnt++;
						}
						if (this.detailedHbefaColdTable.get(efkey2) != null) {
							HbefaColdEmissionFactor ef2 = this.detailedHbefaColdTable.get(efkey2);
							logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
							return ef2;
						}
						//lookups of type "<technology>; average; average" should, I think, just be entered as such. kai, feb'20
						logger.error("Could not find an entry in the technology-averaged-table for <technology>; average; average. ");
					}
				}
				break;
			case tryDetailedThenTechnologyAverageThenAverageTable:
				//Look up detailed values
				if (detailedReadingInfoCnt <= 1) {
					logger.info("try reading detailed values");
					logger.info(Gbl.ONLYONCE);
					logger.info(Gbl.FUTURE_SUPPRESSED);
					detailedReadingInfoCnt++;
				}
				/* The `this.detailedHbefaColdTable.get(efkey)` call may result in a NullPointerException
				 * if the vehicle size class was not set. The following check tries to at least throw a
				 * useful error message. A more complete solution would be to fix the key. (JWJ, June 2022)*/
				HbefaColdEmissionFactor coldEmissionFactor = null;
				try{
					coldEmissionFactor = this.detailedHbefaColdTable.get(efkey);
				} catch(NullPointerException e){
					e.printStackTrace();
					logger.error("Cannot find an emissions factor. One possible cause might be " +
							"that the HbefaSizeClass is not set, in which case it defaults to null " +
							"instead of 'not specified' (for HBEFA 4,1).");
				}
				if (coldEmissionFactor != null) {
					HbefaColdEmissionFactor ef = this.detailedHbefaColdTable.get(efkey);
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					return ef;
				} else {
					if (detailedTransformToHbefa4Cnt <= 1) {
						logger.info("try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again");
						logger.info(Gbl.ONLYONCE);
						logger.info(Gbl.FUTURE_SUPPRESSED);
						detailedTransformToHbefa4Cnt++;
					}
					HbefaColdEmissionFactorKey efkey2 = new HbefaColdEmissionFactorKey(efkey);
					HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4(vehicleInformationTuple);
					// put this into a new key ...
					efkey2.setVehicleAttributes(attribs2);
					// ... and try to look up:
					if (this.detailedHbefaColdTable.get(efkey2) != null) {
						HbefaColdEmissionFactor ef2 = this.detailedHbefaColdTable.get(efkey2);
						logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
						return ef2;
					}

					//if not possible, try "<technology>; average; average":
					if (ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort || ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable) {
						attribs2.setHbefaSizeClass("average");
						attribs2.setHbefaEmConcept("average");
						if (detailedFallbackTechAverageWarnCnt <= 1) {
							logger.warn("Did not find emission factor in the detailed-table for efkey={}", efkey);
							logger.warn("We are now trying to search in the detailed-table, but with an technology-averaged-key : efkey was re-written to {}", efkey2);
							logger.warn("will try it with '<technology>; average; average'");
							logger.warn(Gbl.ONLYONCE);
							logger.warn(Gbl.FUTURE_SUPPRESSED);
							detailedFallbackTechAverageWarnCnt++;
						}
						if (this.detailedHbefaColdTable.get(efkey2) != null) {
							HbefaColdEmissionFactor ef2 = this.detailedHbefaColdTable.get(efkey2);
							logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
							return ef2;
						}
						//lookups of type "<technology>; average; average" should, I think, just be entered as such. kai, feb'20
					}
				}
				if (detailedFallbackAverageTableWarnCnt <= 1) {
					logger.warn("Could not find a technology-averaged entry for <technology>; average; average. ");
					logger.warn("Now trying with setting to vehicle attributes to \"average; average; average\" and try it with the average table");
					logger.warn(Gbl.ONLYONCE);
					logger.warn(Gbl.FUTURE_SUPPRESSED);
					detailedFallbackAverageTableWarnCnt++;
				}
				HbefaColdEmissionFactorKey efkey3 = new HbefaColdEmissionFactorKey(efkey);
				efkey3.setVehicleAttributes(new HbefaVehicleAttributes());
				if (this.avgHbefaColdTable.get(efkey3) != null) {
					HbefaColdEmissionFactor ef = this.avgHbefaColdTable.get(efkey3);
					logger.debug("Lookup result for {} is {}", efkey3, ef.toString());
					Gbl.assertNotNull(ef);
					return ef;
				}
				break;
			case directlyTryAverageTable:
				if (averageReadingInfoCnt <= 1) {
					logger.info("try reading average values");
					logger.info(Gbl.ONLYONCE);
					logger.info(Gbl.FUTURE_SUPPRESSED);
					averageReadingInfoCnt++;
				}
				efkey.setVehicleAttributes(new HbefaVehicleAttributes());
				if (this.avgHbefaColdTable.get(efkey) != null) {
					HbefaColdEmissionFactor ef = this.avgHbefaColdTable.get(efkey);
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					Gbl.assertNotNull(ef);
					return ef;
				} else {
					logger.warn("did not find average emission factor for efkey={}", efkey);
					List<HbefaColdEmissionFactorKey> list = new ArrayList<>(this.avgHbefaColdTable.keySet());
					list.sort(Comparator.comparing(HbefaColdEmissionFactorKey::toString));
					for (HbefaColdEmissionFactorKey key : list) {
						logger.warn(key.toString());
					}
				}
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + ecg.getDetailedVsAverageLookupBehavior());
		}

		throw new RuntimeException("Was not able to lookup emissions factor. Maybe you wanted to look up detailed values and did not specify this in " +
				"the config OR you should use another fallback setting when using detailed calculation OR " +
				"values ar missing in your emissions table(s) either average or detailed OR... ? efkey: " + efkey);
	}

	static HbefaVehicleAttributes createHbefaVehicleAttributes( final String hbefaTechnology, final String hbefaSizeClass, final String hbefaEmConcept ) {
		HbefaVehicleAttributes vehAtt = new HbefaVehicleAttributes();
		vehAtt.setHbefaTechnology( hbefaTechnology );
		vehAtt.setHbefaSizeClass( hbefaSizeClass );
		vehAtt.setHbefaEmConcept( hbefaEmConcept );
		return vehAtt;
	}

}
