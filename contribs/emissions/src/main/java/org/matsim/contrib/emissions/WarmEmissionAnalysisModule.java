/* *********************************************************************** *
 /* ********************************************************************** *
 * project: org.matsim.*												   *
 * WarmEmissionAnalysisModule.java									       *
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
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.*;

import static org.matsim.contrib.emissions.HbefaTrafficSituation.*;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.AverageSpeed;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction;

/**
 * @author benjamin
 *
 */
public final class WarmEmissionAnalysisModule implements LinkEmissionsCalculator{
	// cannot make non-public: used at least twice outside package.  kai, jan'19

	private static final Logger logger = LogManager.getLogger(WarmEmissionAnalysisModule.class);

	private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor>  avgHbefaWarmTable;
	private final Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable;
	private final Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds;
	private final Set<Pollutant> warmPollutants;

	private final EventsManager eventsManager;
	private final EmissionsConfigGroup ecg;

	private int detailedReadingInfoCnt = 0;
	private int detailedTransformToHbefa4Cnt = 0;
	private int detailedFallbackTechAverageWarnCnt = 0;
	private int detailedFallbackAverageTableWarnCnt = 0;
	private int averageReadingInfoCnt = 0;

	private int freeFlowCounter = 0;
	private int saturatedCounter = 0;
	private int heavyFlowCounter = 0;
	private int stopGoCounter = 0;
	private int heavyStopGoCounter = 0;
	private int fractionCounter = 0;
	private int emissionEventCounter = 0;

	private double kmCounter = 0.0;
	private double freeFlowKmCounter = 0.0;
	private double heavyFlowKmCounter = 0.0;
	private double saturatedKmCounter = 0.0;
	private double stopGoKmCounter = 0.0;
	private double heavyStopGoKmCounter = 0.0;

	public WarmEmissionAnalysisModule(
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> avgHbefaWarmTable,
			Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> detailedHbefaWarmTable,
			Map<HbefaRoadVehicleCategoryKey, Map<HbefaTrafficSituation, Double>> hbefaRoadTrafficSpeeds,
			Set<Pollutant> warmPollutants, EventsManager eventsManager, EmissionsConfigGroup ecg ){
		this.ecg = ecg;

		Gbl.assertIf( avgHbefaWarmTable!=null || detailedHbefaWarmTable!=null );
		this.avgHbefaWarmTable = avgHbefaWarmTable;
		this.detailedHbefaWarmTable = detailedHbefaWarmTable;
		this.hbefaRoadTrafficSpeeds = hbefaRoadTrafficSpeeds;
		this.warmPollutants = warmPollutants;

		Gbl.assertNotNull( eventsManager );
		this.eventsManager = eventsManager;

		if ( detailedHbefaWarmTable!=null ) {
			switch (ecg.getHbefaTableConsistencyCheckingLevel()) {
				case allCombinations:
					// The following tests if the detailed table is consistent, i.e. if there exist all combinations of entries.  There used to be some test
					// cases where this was deliberately not the case, implying that this was assumed as plausible also for studies.  This is now forbidding it.
					// If this causes too many problems, we could insert a switch (or attach it to the fallback behavior switch).  kai, feb'20
					// Eventually vehicle category and vehicle attribute should be aligned in order to make the allCombinations setting useful
					// see discussion in  https://github.com/matsim-org/matsim-libs/issues/1226 kturner, nov'20
					Set<String> roadCategories = new HashSet<>();
					Set<HbefaTrafficSituation> trafficSituations = EnumSet.noneOf(HbefaTrafficSituation.class);
					Set<HbefaVehicleCategory> vehicleCategories = EnumSet.noneOf(HbefaVehicleCategory.class);
					Set<HbefaVehicleAttributes> vehicleAttributes = new HashSet<>();
					Set<Pollutant> pollutantsInTable = EnumSet.noneOf(Pollutant.class);
					for (HbefaWarmEmissionFactorKey emissionFactorKey : detailedHbefaWarmTable.keySet()) {
						roadCategories.add(emissionFactorKey.getRoadCategory());
						trafficSituations.add(emissionFactorKey.getTrafficSituation());
						vehicleCategories.add(emissionFactorKey.getVehicleCategory());
						vehicleAttributes.add(emissionFactorKey.getVehicleAttributes());
						pollutantsInTable.add(emissionFactorKey.getComponent());
					}
					for (String roadCategory : roadCategories) {
						for (HbefaTrafficSituation trafficSituation : trafficSituations) {
							for (HbefaVehicleCategory vehicleCategory : vehicleCategories) {
								for (HbefaVehicleAttributes vehicleAttribute : vehicleAttributes) {
									for (Pollutant pollutant : pollutantsInTable) {
										HbefaWarmEmissionFactorKey key = new HbefaWarmEmissionFactorKey();
										key.setRoadCategory(roadCategory);
										key.setTrafficSituation(trafficSituation);
										key.setVehicleCategory(vehicleCategory);
										key.setVehicleAttributes(vehicleAttribute);
										key.setComponent(pollutant);
										HbefaWarmEmissionFactor result = detailedHbefaWarmTable.get(key);
										if (result == null) {
											throw new RuntimeException("emissions factor for key=" + key + " is missing." +
													"  There used to be some " +
													"fallback, but it was " +
													"inconsistent and confusing, so " +
													"we are now just aborting.");
										}
									}
								}
							}
						}
					}
					break;

				case consistent:
					// yy The above consistency check might actually be too restrictive.  The code only needs that both freeflow and stopgo traffic
					// conditions exist for a certain lookup.  So we could still have some road categories, vehicle categories or vehicle attributes
					// where some detailed values exist and others don't.  So the thing to check would be if for each existing
					//   roadCategory x vehicleCategory x vehicleAttribute x pollutant
					// there is a freeflow and a stop-go entry.  Maybe something like this here:
					Set<String> freeflowSet = new HashSet<>();
					Set<String> stopgoSet = new HashSet<>();
					for (HbefaWarmEmissionFactorKey key : detailedHbefaWarmTable.keySet()) {
						String syntheticKey = key.getRoadCategory() + "--" + key.getVehicleCategory() + "--" + key.getVehicleAttributes() + "--" + key.getComponent();
						switch ( key.getTrafficSituation() ) {
							case FREEFLOW -> freeflowSet.add( syntheticKey );
							case STOPANDGO -> stopgoSet.add( syntheticKey );
							default -> { } // do nothing
						}
					}
					for (String syntheticKey : freeflowSet) {
						if (!stopgoSet.contains(syntheticKey)) {
							throw new RuntimeException("inconsistent Freeflow and Stop&Go entries in detailed HBEFA table - Stop&Go entry missing ");
						}
					}
					for (String syntheticKey : stopgoSet) {
						if (!freeflowSet.contains(syntheticKey)) {
							throw new RuntimeException("inconsistent Freeflow and Stop&Go entries in detailed HBEFA table - Freeflow entry missing");
						}
					}
					break;
				case none:
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + ecg.getHbefaTableConsistencyCheckingLevel());
			}
		}
	}

	void reset() {
		logger.info("resetting counters...");

		freeFlowCounter = 0;
		saturatedCounter = 0;
		heavyFlowCounter = 0;
		stopGoCounter = 0;
		heavyStopGoCounter = 0;
		emissionEventCounter = 0;

		kmCounter = 0.0;
		freeFlowKmCounter = 0.0;
		heavyFlowKmCounter = 0.0;
		saturatedKmCounter = 0.0;
		fractionCounter = 0;
		stopGoKmCounter = 0.0;
		heavyStopGoKmCounter = 0.0;
	}

	void throwWarmEmissionEvent(double leaveTime, Id<Link> linkId, Id<Vehicle> vehicleId, Map<Pollutant, Double> warmEmissions) {
		Event warmEmissionEvent = new WarmEmissionEvent(leaveTime, linkId, vehicleId, warmEmissions);
		this.eventsManager.processEvent(warmEmissionEvent);
	}

	@Override
	public Map<Pollutant, Double> checkVehicleInfoAndCalculateWarmEmissions(Vehicle vehicle, Link link, double travelTime) {
		return checkVehicleInfoAndCalculateWarmEmissions(vehicle.getType(), vehicle.getId(), link, travelTime);
	}

	private static int cnt = 10;

	/*package-private*/ Map<Pollutant, Double> checkVehicleInfoAndCalculateWarmEmissions(VehicleType vehicleType, Id<Vehicle> vehicleId,
																						 Link link, double travelTime) {
		{
			String hbefaVehicleTypeDescription = EmissionUtils.getHbefaVehicleDescription(vehicleType, this.ecg);
			// (this will, importantly, repair the hbefa description in the vehicle type. kai/kai, jan'20)
			Gbl.assertNotNull(hbefaVehicleTypeDescription);
		}
		Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple = EmissionUtils.convertVehicleDescription2VehicleInformationTuple(vehicleType);
		Gbl.assertNotNull(vehicleInformationTuple);

		if (vehicleInformationTuple.getFirst() == null) {
			throw new RuntimeException("Vehicle category for vehicle " + vehicleType + " is not valid. " +
					"Please make sure that requirements for emission vehicles in " +
					EmissionsConfigGroup.GROUP_NAME + " config group are met. Aborting...");
		}

		double freeVelocity = link.getFreespeed(); //TODO: what about time dependence

		return calculateWarmEmissions(travelTime, EmissionUtils.getHbefaRoadType(link), freeVelocity, link.getLength(), vehicleInformationTuple);
	}

	Map<Pollutant, Double> calculateWarmEmissions(double travelTime_sec, String roadType, double freeVelocity_ms,
												  double linkLength_m, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {

		Map<Pollutant, Double> warmEmissionsOfEvent = new EnumMap<>(Pollutant.class);

		// fallback vehicle types that we cannot or do not want to map onto a hbefa vehicle type:
		if (vehicleInformationTuple.getFirst() == HbefaVehicleCategory.NON_HBEFA_VEHICLE) {
			for (Pollutant warmPollutant : warmPollutants) {
				warmEmissionsOfEvent.put(warmPollutant, 0.0);
				// yyyyyy todo replace by something more meaningful. kai, jan'20
			}
			if (cnt > 0) {
				logger.warn("Just encountered non hbefa vehicle; currently, this code is setting the emissions of such vehicles to zero.  " +
						"Might be necessary to find a better solution for this.  kai, jan'20");
				cnt--;
				if (cnt == 0) {
					logger.warn(Gbl.FUTURE_SUPPRESSED);
				}
			}
			return warmEmissionsOfEvent;
		}

		// translate vehicle information type into factor key.  yyyy maybe combine these two? kai, jan'20
		HbefaWarmEmissionFactorKey efkey = new HbefaWarmEmissionFactorKey();
		efkey.setVehicleCategory(vehicleInformationTuple.getFirst());
		efkey.setRoadCategory(roadType);
		if (this.detailedHbefaWarmTable != null) {
			HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
			hbefaVehicleAttributes.setHbefaTechnology(vehicleInformationTuple.getSecond().getHbefaTechnology());
			hbefaVehicleAttributes.setHbefaSizeClass(vehicleInformationTuple.getSecond().getHbefaSizeClass());
			hbefaVehicleAttributes.setHbefaEmConcept(vehicleInformationTuple.getSecond().getHbefaEmConcept());
			efkey.setVehicleAttributes(hbefaVehicleAttributes);
		}


		double averageSpeed_kmh = (linkLength_m / 1000) / (travelTime_sec / 3600);


		// hedge against odd average speeds:
		if(averageSpeed_kmh <= 0.0){
			throw new RuntimeException("Average speed has been calculated to 0.0 or a negative value. Aborting...");
		}
		if ((averageSpeed_kmh - freeVelocity_ms * 3.6) > 1.0){
			if (ecg.getHandleHighAverageSpeeds()) {
				logger.warn("averageSpeed was capped from {} to{}", averageSpeed_kmh, freeVelocity_ms * 3.6);
				averageSpeed_kmh = freeVelocity_ms * 3.6;
			} else {
				throw new RuntimeException("Average speed has been calculated to be greater than free flow speed; this might produce negative warm emissions. Aborting...");
			}
		}

		// for the average speed method, the traffic situation is already known here:
		if (ecg.getEmissionsComputationMethod() == AverageSpeed) {
			final HbefaTrafficSituation trafficSituation = getTrafficSituation(efkey, averageSpeed_kmh, freeVelocity_ms * 3.6);
//			logger.warn( "trafficSituation=" + trafficSituation );
			efkey.setTrafficSituation(trafficSituation);
		}

		double fractionStopGo = 0;

		// for each pollutant, compute and memorize emissions:
		for ( Pollutant warmPollutant : warmPollutants) {

			efkey.setComponent(warmPollutant);

			double ef_gpkm;
			if (ecg.getEmissionsComputationMethod() == StopAndGoFraction) {

				// compute faction.  This cannot be done earlier since efkey.component is needed.
				fractionStopGo = getFractionStopAndGo(freeVelocity_ms * 3.6, averageSpeed_kmh, vehicleInformationTuple, efkey);

				double efStopGo_gpkm = 0.;
				if (fractionStopGo > 0) {
					// compute emissions from stop-go fraction:
					efkey.setTrafficSituation(STOPANDGO);
					efStopGo_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
					logger.debug("pollutant={}; efStopGo={}", warmPollutant, efStopGo_gpkm);

				}

				double efFreeFlow_gpkm = 0. ;
				if ( fractionStopGo<1.) {
					// compute emissions for free-flow fraction:
					efkey.setTrafficSituation(FREEFLOW);
					efFreeFlow_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
					logger.debug("pollutant={}; efFreeFlow={}", warmPollutant, efFreeFlow_gpkm);
				}

				// sum them up:
				double fractionFreeFlow = 1 - fractionStopGo;
				ef_gpkm = (fractionFreeFlow * efFreeFlow_gpkm) + (fractionStopGo * efStopGo_gpkm);

			} else if (ecg.getEmissionsComputationMethod() == AverageSpeed) {
				ef_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
			} else {
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
			}

			double generatedEmissions = (linkLength_m / 1000) * ef_gpkm;
			warmEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}

		// update counters:
		// yy I don't know what this is good for; I would base downstream analysis rather on events.  kai, jan'20
		if (ecg.getEmissionsComputationMethod() == StopAndGoFraction) {
			incrementCountersFractional( linkLength_m / 1000, fractionStopGo );
		}
		else if (ecg.getEmissionsComputationMethod() == AverageSpeed) {
			incrementCountersAverage(efkey.getTrafficSituation(), linkLength_m / 1000);
		} else {
			throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
		}

		return warmEmissionsOfEvent;
	}

	private double getFractionStopAndGo(double freeFlowSpeed_kmh, double averageSpeed_kmh,
										Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple,
										HbefaWarmEmissionFactorKey efkey) {

		efkey.setTrafficSituation(STOPANDGO);
		double stopGoSpeedFromTable_kmh = getEf(vehicleInformationTuple, efkey).getSpeed();

		double fractionStopGo;

		if ((averageSpeed_kmh - freeFlowSpeed_kmh) >= -1.0) { // both speeds are assumed to be not very different > only freeFlow on link
			fractionStopGo = 0.0;
		} else if ((averageSpeed_kmh - stopGoSpeedFromTable_kmh) <= 0.0) { // averageSpeed is less than stopGoSpeed > only stop&go on link
			fractionStopGo = 1.0;
		} else {
			fractionStopGo = stopGoSpeedFromTable_kmh * (freeFlowSpeed_kmh - averageSpeed_kmh) / (averageSpeed_kmh * (freeFlowSpeed_kmh - stopGoSpeedFromTable_kmh));
		}

		return fractionStopGo;
	}

	private HbefaWarmEmissionFactor getEf(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey) {

		switch ( ecg.getDetailedVsAverageLookupBehavior() ) {
			case onlyTryDetailedElseAbort -> {
				if ( detailedReadingInfoCnt <= 1 ) {
					logger.info( "try reading detailed values" );
					logger.info( Gbl.ONLYONCE );
					logger.info( Gbl.FUTURE_SUPPRESSED );
					detailedReadingInfoCnt++;
				}
				if ( this.detailedHbefaWarmTable.get( efkey ) != null ) {
					HbefaWarmEmissionFactor ef = this.detailedHbefaWarmTable.get( efkey );
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					return ef;
				} else {
					if ( detailedTransformToHbefa4Cnt <= 1 ) {
						logger.info( "try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again" );
						logger.info( Gbl.ONLYONCE );
						logger.info( Gbl.FUTURE_SUPPRESSED );
						detailedTransformToHbefa4Cnt++;
					}
					HbefaWarmEmissionFactorKey efkey2 = new HbefaWarmEmissionFactorKey( efkey );
					HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4( vehicleInformationTuple );
					// put this into a new key ...
					efkey2.setVehicleAttributes( attribs2 );
					// ... and try to look up:
					if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
						HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey2 );
						logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
						return ef2;
					}
				}
			}
			case tryDetailedThenTechnologyAverageElseAbort -> {
				//Look up detailed values
				if ( detailedReadingInfoCnt <= 1 ) {
					logger.info( "try reading detailed values" );
					logger.info( Gbl.ONLYONCE );
					logger.info( Gbl.FUTURE_SUPPRESSED );
					detailedReadingInfoCnt++;
				}
				if ( this.detailedHbefaWarmTable.get( efkey ) != null ) {
					HbefaWarmEmissionFactor ef = this.detailedHbefaWarmTable.get( efkey );
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					return ef;
				} else {
					if ( detailedTransformToHbefa4Cnt <= 1 ) {
						logger.info( "try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again" );
						logger.info( Gbl.ONLYONCE );
						logger.info( Gbl.FUTURE_SUPPRESSED );
						detailedTransformToHbefa4Cnt++;
					}
					HbefaWarmEmissionFactorKey efkey2 = new HbefaWarmEmissionFactorKey( efkey );
					HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4( vehicleInformationTuple );
					// put this into a new key ...
					efkey2.setVehicleAttributes( attribs2 );
					// ... and try to look up:
					if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
						HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey2 );
						logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
						return ef2;
					}

					//if not possible, try "<technology>; average; average":
					if ( ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort || ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable ) {
						attribs2.setHbefaSizeClass( "average" );
						attribs2.setHbefaEmConcept( "average" );
						if ( detailedFallbackTechAverageWarnCnt <= 1 ) {
							logger.warn("Did not find emission factor in the detailed-table for efkey={}", efkey);
							logger.warn("We are now trying to search in the detailed-table, but with an technology-averaged-key : efkey was re-written to {}", efkey2);
							logger.warn( "will try it with '<technology>; average; average'" );
							logger.warn( Gbl.ONLYONCE );
							logger.warn( Gbl.FUTURE_SUPPRESSED );
							detailedFallbackTechAverageWarnCnt++;
						}
						if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
							HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey2 );
							logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
							return ef2;
						}
						//lookups of type "<technology>; average; average" should, I think, just be entered as such. kai, feb'20
						logger.error( "Could not find an entry in the technology-averaged-table for <technology>; average; average. " );
					}
				}
			}
			case tryDetailedThenTechnologyAverageThenAverageTable -> {
				//Look up detailed values
				if ( detailedReadingInfoCnt <= 1 ) {
					logger.info( "try reading detailed values" );
					logger.info( Gbl.ONLYONCE );
					logger.info( Gbl.FUTURE_SUPPRESSED );
					detailedReadingInfoCnt++;
				}
				if ( this.detailedHbefaWarmTable.get( efkey ) != null ) {
					HbefaWarmEmissionFactor ef = this.detailedHbefaWarmTable.get( efkey );
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					return ef;
				} else {
					if ( detailedTransformToHbefa4Cnt <= 1 ) {
						logger.info( "try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again" );
						logger.info( Gbl.ONLYONCE );
						logger.info( Gbl.FUTURE_SUPPRESSED );
						detailedTransformToHbefa4Cnt++;
					}
					HbefaWarmEmissionFactorKey efkey2 = new HbefaWarmEmissionFactorKey( efkey );
					HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4( vehicleInformationTuple );
					// put this into a new key ...
					efkey2.setVehicleAttributes( attribs2 );
					// ... and try to look up:
					if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
						HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey2 );
						logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
						return ef2;
					}

					//if not possible, try "<technology>; average; average":
					if ( ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort || ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable ) {
						attribs2.setHbefaSizeClass( "average" );
						attribs2.setHbefaEmConcept( "average" );
						if ( detailedFallbackTechAverageWarnCnt <= 1 ) {
							logger.warn("Did not find emission factor in the detailed-table for efkey={}", efkey);
							logger.warn("We are now trying to search in the detailed-table, but with an technology-averaged-key : efkey was re-written to {}", efkey2);
							logger.warn( "will try it with '<technology>; average; average'" );
							logger.warn( Gbl.ONLYONCE );
							logger.warn( Gbl.FUTURE_SUPPRESSED );
							detailedFallbackTechAverageWarnCnt++;
						}
						if ( this.detailedHbefaWarmTable.get( efkey2 ) != null ) {
							HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey2 );
							logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
							return ef2;
						}
						//lookups of type "<technology>; average; average" should, I think, just be entered as such. kai, feb'20
					}
				}
				if ( detailedFallbackAverageTableWarnCnt <= 1 ) {
					logger.error( "Could not find an technology-averaged entry for <technology>; average; average. " );
					logger.warn( "Now trying with setting to vehicle attributes to \"average; average; average\" and try it with the average table" );
					logger.warn( Gbl.ONLYONCE );
					logger.warn( Gbl.FUTURE_SUPPRESSED );
					detailedFallbackAverageTableWarnCnt++;
				}
				HbefaWarmEmissionFactorKey efkey3 = new HbefaWarmEmissionFactorKey( efkey );
				efkey3.setVehicleAttributes( new HbefaVehicleAttributes() );
				if ( this.avgHbefaWarmTable.get( efkey3 ) != null ) {
					HbefaWarmEmissionFactor ef = this.avgHbefaWarmTable.get( efkey3 );
					logger.debug("Lookup result for {} is {}", efkey3, ef.toString());
					Gbl.assertNotNull( ef );
					return ef;
				}
			}
			case directlyTryAverageTable -> {
				if ( averageReadingInfoCnt <= 1 ) {
					logger.info( "try reading average values" );
					logger.info( Gbl.ONLYONCE );
					logger.info( Gbl.FUTURE_SUPPRESSED );
					averageReadingInfoCnt++;
				}
				efkey.setVehicleAttributes( new HbefaVehicleAttributes() );
				if ( this.avgHbefaWarmTable.get( efkey ) != null ) {
					HbefaWarmEmissionFactor ef = this.avgHbefaWarmTable.get( efkey );
					logger.debug("Lookup result for {} is {}", efkey, ef.toString());
					Gbl.assertNotNull( ef );
					return ef;
				} else {
					logger.warn("Did not find average emission factor in average-table for efkey={}", efkey);
					List<HbefaWarmEmissionFactorKey> list = new ArrayList<>( this.avgHbefaWarmTable.keySet() );
					list.sort( Comparator.comparing( HbefaWarmEmissionFactorKey::toString ) );
					for ( HbefaWarmEmissionFactorKey key : list ) {
						logger.warn( key.toString() );
					}
				}
			}
			default ->
					throw new IllegalStateException( "Unexpected value: " + ecg.getDetailedVsAverageLookupBehavior() );
		}

		throw new RuntimeException("Was not able to lookup emissions factor. Maybe you wanted to look up detailed values and did not specify this in " +
                                                           "the config OR " +
				"you should use another fallback setting when using detailed calculation OR values ar missing in your emissions table(s) either average or detailed OR... ? efkey: " + efkey.toString());
	}


	//TODO: this is based on looking at the speeds in the HBEFA files, using an MFP, maybe from A.Loder would be nicer, jm  oct'18
	private HbefaTrafficSituation getTrafficSituation(HbefaWarmEmissionFactorKey efkey, double averageSpeed_kmh, double freeFlowSpeed_kmh) {
		//TODO: should this be generated only once much earlier?
		HbefaRoadVehicleCategoryKey hbefaRoadVehicleCategoryKey = new HbefaRoadVehicleCategoryKey(efkey);
		Map<HbefaTrafficSituation, Double> trafficSpeeds = this.hbefaRoadTrafficSpeeds.get(hbefaRoadVehicleCategoryKey);

		//TODO: Hier die Berechnung einfügen, die die trafficSpeedTabelle entsprechend aus den Werten erstellt?
		//Frage Laufzeit: Einmal berechnen ha

		if (trafficSpeeds == null || !trafficSpeeds.containsKey(FREEFLOW)) {
			throw new RuntimeException("At least the FREEFLOW condition must be specified for all emission factor keys. " +
					"It was not found for " + efkey);
		}

		HbefaTrafficSituation trafficSituation  = FREEFLOW;
		if (trafficSpeeds.containsKey(HEAVY) && averageSpeed_kmh <= trafficSpeeds.get(HEAVY)) {
			trafficSituation = HEAVY;
		}
		if (trafficSpeeds.containsKey(SATURATED) && averageSpeed_kmh <= trafficSpeeds.get(SATURATED)) {
			trafficSituation = SATURATED;
		}
		if (trafficSpeeds.containsKey(STOPANDGO) && averageSpeed_kmh <= trafficSpeeds.get(STOPANDGO)) {
			if (averageSpeed_kmh != trafficSpeeds.get(FREEFLOW)) { //handle case testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6
				trafficSituation = STOPANDGO;
			}
		}
		/*FIXME The following lines should be added to account for the HBEFA 4.1's additional traffic situation,
		   but it currently causes a test failure (jwj, Nov'20) */
//		if (trafficSpeeds.containsKey(STOPANDGO_HEAVY) && averageSpeed_kmh <= trafficSpeeds.get(STOPANDGO_HEAVY)) {
//			if (averageSpeed_kmh != trafficSpeeds.get(FREEFLOW)) { //handle case testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6
//				trafficSituation = STOPANDGO_HEAVY;
//			}
//		}
		return trafficSituation;
	}

	private void incrementCountersFractional(double linkLength_km, double fractionStopGo) {
		kmCounter = kmCounter + linkLength_km;
		emissionEventCounter++;

		freeFlowKmCounter += linkLength_km * (1-fractionStopGo);
		stopGoKmCounter += linkLength_km * fractionStopGo;

		freeFlowCounter += (int) (1-fractionStopGo);
		stopGoCounter += (int) fractionStopGo;
		fractionCounter += (fractionStopGo < 1.0 && fractionStopGo > 0.0) ? 1 : 0;
	}

	private void incrementCountersAverage(HbefaTrafficSituation hbefaTrafficSituation, double linkLength_km) {
		kmCounter = kmCounter + linkLength_km;
		emissionEventCounter++;

		switch ( hbefaTrafficSituation ) { // both speeds are assumed to be not very different > only freeFlow on link
			case FREEFLOW -> {
				freeFlowCounter++;
				freeFlowKmCounter += linkLength_km;
			}
			case HEAVY -> {
				saturatedCounter++;
				saturatedKmCounter += linkLength_km;
			}
			case SATURATED -> {
				heavyFlowCounter++;
				heavyFlowKmCounter += linkLength_km;
			}
			case STOPANDGO -> {
				stopGoCounter++;
				stopGoKmCounter += linkLength_km;
			}
			case STOPANDGO_HEAVY -> {
				heavyStopGoCounter++;
				heavyStopGoKmCounter += linkLength_km;
			}
		}
	}

	//------ These (occurrences) seem to be used only for logging statements and tests. KMT/GR Jul'20
	/*package-private*/ int getFreeFlowOccurences() {
		return freeFlowCounter;
	}
	/*package-private*/ private int getHeavyOccurences() { return heavyFlowCounter; }
	/*package-private*/ private int getSaturatedOccurences() { return saturatedCounter; }
	/*package-private*/ int getStopGoOccurences() { return stopGoCounter; }
	/*package-private*/ int getHeavyStopGoCounter(){ return heavyStopGoCounter; }
	/*package-private*/ double getKmCounter() {
		return kmCounter;
	}
	/*package-private*/ double getFreeFlowKmCounter() {
		return freeFlowKmCounter;
	}
	/*package-private*/ private double getHeavyFlowKmCounter() {
		return heavyFlowKmCounter;
	}
	/*package-private*/ private double getSaturatedKmCounter() {
		return saturatedKmCounter;
	}
	/*package-private*/ double getStopGoKmCounter() {
		return stopGoKmCounter;
	}
	/*package-private*/ double getHeavyStopGoKmCounter() {
		return heavyStopGoKmCounter;
	}
	/*package-private*/ int getWarmEmissionEventCounter() {
		return emissionEventCounter;
	}
	/*package-private*/ int getFractionOccurences() {
		return fractionCounter;
	}
	/*package-private*/ double getFractionKmCounter() {
		return getSaturatedKmCounter() + getHeavyFlowKmCounter();
	}
	/*package-private*/ EmissionsConfigGroup getEcg() {
		return ecg;
	}

}
