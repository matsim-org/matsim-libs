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
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.matsim.contrib.emissions.HbefaTrafficSituation.*;
import static org.matsim.contrib.emissions.utils.EmissionsConfigGroup.EmissionsComputationMethod.*;

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
	private final Map<HbefaWarmEmissionFactorKey, List<HbefaWarmEmissionFactorKey>> hbefaRoadVClasses = new HashMap<>(); //TODO Remove if BiLinInt is not useful
	private final Set<Pollutant> warmPollutants;

	private final EventsManager eventsManager;
	private final EmissionsConfigGroup ecg;

	private int detailedReadingInfoCnt = 0;
	private int techAverageReadingInfoCnt = 0;
	private int averageReadingInfoCnt = 0;
	private int detailedTransformToHbefa4Cnt = 0;
	private int fallbackTechAverageWarnCnt = 0;
	private int fallbackAverageTableWarnCnt = 0;

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

		double deltaHeight = link.getToNode().getCoord().getZ() - link.getFromNode().getCoord().getZ();

		return calculateWarmEmissions(travelTime, EmissionUtils.getHbefaRoadType(link), freeVelocity, link.getLength(), deltaHeight, vehicleInformationTuple);
	}

	// TODO Check if calls to this method can be updated to use slopes
	@Deprecated
	Map<Pollutant, Double> calculateWarmEmissions(double travelTime_sec, String roadType, double freeVelocity_ms,
												  double linkLength_m, Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple){
		return calculateWarmEmissions(travelTime_sec, roadType, freeVelocity_ms, linkLength_m, 0, vehicleInformationTuple);
	}

	Map<Pollutant, Double> calculateWarmEmissions(double travelTime_sec, String roadType, double freeVelocity_ms,
												  double linkLength_m, double deltaHeight_m,
												  Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple) {

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
		efkey.setRoadGradient(getHbefaRoadGradientFromSlope(deltaHeight_m/linkLength_m));
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
		double fractionLow = 0;

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
			} else if (ecg.getEmissionsComputationMethod() == InterpolationFraction) {

				// Determine higher traffic-situation class
				HbefaTrafficSituation higher = getTrafficSituation(efkey, averageSpeed_kmh, freeVelocity_ms * 3.6);

				// Determine the lower traffic-situation class
				HbefaTrafficSituation lower = getLowerTrafficSituation(efkey, higher);

				// compute faction.
				fractionLow = getFractionInterpolation(higher, lower, averageSpeed_kmh, vehicleInformationTuple, efkey);

				double efLow_gpkm = 0.;
				if (fractionLow > 0) {
					// compute emissions from lower fraction:
					efkey.setTrafficSituation(lower);
					efLow_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
					logger.debug("pollutant={}; efStopGo={}", warmPollutant, efLow_gpkm);

				}

				double efHigh_gpkm = 0. ;
				if ( fractionLow<1.) {
					// compute emissions for higher fraction:
					efkey.setTrafficSituation(higher);
					efHigh_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
					logger.debug("pollutant={}; efFreeFlow={}", warmPollutant, efHigh_gpkm);
				}

				// sum them up:
				double fractionHigh = 1 - fractionLow;
				ef_gpkm = (fractionHigh * efHigh_gpkm) + (fractionLow * efLow_gpkm);

			} else if(ecg.getEmissionsComputationMethod() == BilinearInterpolationFraction){
				// TODO This is a temporary and inefficient implementation. The final solutions should not determine the keys each time but save them

				// Determine the keys for higher and lower V-class (using just the freespeed)
				HbefaWarmEmissionFactorKey higherVClassKey = new HbefaWarmEmissionFactorKey(getHigherVClass(vehicleInformationTuple, efkey, freeVelocity_ms * 3.6));
				HbefaWarmEmissionFactorKey lowerVClassKey = new HbefaWarmEmissionFactorKey(getLowerVClass(vehicleInformationTuple, efkey, freeVelocity_ms * 3.6));

				if (!higherVClassKey.equals(lowerVClassKey)){
					// Approach 1
					/*
					// Determine the interpolation-triangle
					List<Tuple<HbefaWarmEmissionFactorKey, Double>> triangle = determineInterpolationTriangle(lowerVClassKey, higherVClassKey, vehicleInformationTuple, freeVelocity_ms *3.6, averageSpeed_kmh);

					// Compute the emission key (using brycentric coordinates)
					ef_gpkm =
						triangle.get(0).getSecond() * getEf(vehicleInformationTuple, triangle.get(0).getFirst()).getFactor() +
							triangle.get(1).getSecond() * getEf(vehicleInformationTuple, triangle.get(1).getFirst()).getFactor() +
							triangle.get(2).getSecond() * getEf(vehicleInformationTuple, triangle.get(2).getFirst()).getFactor();
					 */

					// Approach 2
					double higherVValue = getEf(vehicleInformationTuple, higherVClassKey).getSpeed();
					double lowerVValue = getEf(vehicleInformationTuple, lowerVClassKey).getSpeed();

					double higherVClassFraction = (higherVValue - (freeVelocity_ms*3.6)) / (higherVValue - lowerVValue);
					double lowerVClassFraction = 1 - higherVClassFraction;

					HbefaTrafficSituation higherVClassHigherTrafficSit = getTrafficSituation(efkey, averageSpeed_kmh, freeVelocity_ms * 3.6);
					HbefaTrafficSituation higherVClassLowerTrafficSit = getLowerTrafficSituation(efkey, higherVClassHigherTrafficSit);

					HbefaTrafficSituation lowerVClassHigherTrafficSit = getTrafficSituation(efkey, averageSpeed_kmh, freeVelocity_ms * 3.6);
					HbefaTrafficSituation lowerVClassLowerTrafficSit = getLowerTrafficSituation(efkey, lowerVClassHigherTrafficSit);

					double fractionHighLow = getFractionInterpolation(higherVClassHigherTrafficSit, higherVClassLowerTrafficSit, averageSpeed_kmh, vehicleInformationTuple, efkey);
					double fractionLowLow = getFractionInterpolation(lowerVClassHigherTrafficSit, lowerVClassLowerTrafficSit, averageSpeed_kmh, vehicleInformationTuple, efkey);

					higherVClassKey.setTrafficSituation(higherVClassHigherTrafficSit);
					double efHighHigh_gpkm = getEf(vehicleInformationTuple, higherVClassKey).getFactor();
					higherVClassKey.setTrafficSituation(higherVClassLowerTrafficSit);
					double efHighLow_gpkm = getEf(vehicleInformationTuple, higherVClassKey).getFactor();

					lowerVClassKey.setTrafficSituation(lowerVClassHigherTrafficSit);
					double efLowHigh_gpkm = getEf(vehicleInformationTuple, lowerVClassKey).getFactor();
					lowerVClassKey.setTrafficSituation(lowerVClassLowerTrafficSit);
					double efLowLow_gpkm = getEf(vehicleInformationTuple, lowerVClassKey).getFactor();

					assert higherVClassFraction >= -1e-6 && higherVClassFraction <= 1+1e-6;
					assert lowerVClassFraction >= -1e-6 && lowerVClassFraction <= 1+1e-6;
					assert fractionHighLow >= -1e-6 && fractionHighLow <= 1+1e-6;
					assert fractionLowLow >= -1e-6 && fractionLowLow <= 1+1e-6;

					ef_gpkm =
						higherVClassFraction*(fractionHighLow*efHighLow_gpkm + (1-fractionHighLow)*efHighHigh_gpkm) +
						lowerVClassFraction*(fractionLowLow*efLowLow_gpkm + (1-fractionLowLow)*efLowHigh_gpkm);

				} else{
					// If keys are the same, we are outside the possible interpolation range. Thus we have to use basic interpolation fraction with the boundary v-class

					// TODO Duplicate code
					// Determine higher traffic-situation class
					HbefaTrafficSituation higher = getTrafficSituation(efkey, averageSpeed_kmh, freeVelocity_ms * 3.6);

					// Determine the lower traffic-situation class
					HbefaTrafficSituation lower = getLowerTrafficSituation(efkey, higher);

					// compute faction.
					fractionLow = getFractionInterpolation(higher, lower, averageSpeed_kmh, vehicleInformationTuple, higherVClassKey);

					double efLow_gpkm = 0.;
					if (fractionLow > 0) {
						// compute emissions from lower fraction:
						efkey.setTrafficSituation(lower);
						efLow_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
						logger.debug("pollutant={}; efStopGo={}", warmPollutant, efLow_gpkm);

					}

					double efHigh_gpkm = 0. ;
					if ( fractionLow<1.) {
						// compute emissions for higher fraction:
						efkey.setTrafficSituation(higher);
						efHigh_gpkm = getEf(vehicleInformationTuple, efkey).getFactor();
						logger.debug("pollutant={}; efFreeFlow={}", warmPollutant, efHigh_gpkm);
					}

					// sum them up:
					double fractionHigh = 1 - fractionLow;
					ef_gpkm = (fractionHigh * efHigh_gpkm) + (fractionLow * efLow_gpkm);
				}

			}
			else {
				throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
			}

			double generatedEmissions = (linkLength_m / 1000) * ef_gpkm;
			warmEmissionsOfEvent.put(warmPollutant, generatedEmissions);
		}

		// update counters:
		// yy I don't know what this is good for; I would base downstream analysis rather on events.  kai, jan'20
		if (ecg.getEmissionsComputationMethod() == StopAndGoFraction || ecg.getEmissionsComputationMethod() == InterpolationFraction || ecg.getEmissionsComputationMethod() == BilinearInterpolationFraction) {
			incrementCountersFractional( linkLength_m / 1000, fractionStopGo );
		}
		else if (ecg.getEmissionsComputationMethod() == AverageSpeed) {
			incrementCountersAverage(efkey.getTrafficSituation(), linkLength_m / 1000);
		} else {
			throw new RuntimeException( Gbl.NOT_IMPLEMENTED );
		}

		return warmEmissionsOfEvent;
	}

	private HbefaRoadGradient getHbefaRoadGradientFromSlope(double slope_percent) {
		// TODO +/- values are currently not implemented
		slope_percent *= 100;

		if (slope_percent < -5) return HbefaRoadGradient.MINUS_6;
		if (slope_percent < -3) return HbefaRoadGradient.MINUS_4;
		if (slope_percent < -1) return HbefaRoadGradient.MINUS_2;
		if (slope_percent < 1) return HbefaRoadGradient.ZERO;
		if (slope_percent < 3) return HbefaRoadGradient.PLUS_2;
		if (slope_percent < 5) return HbefaRoadGradient.PLUS_4;
		if (slope_percent >= 5) return HbefaRoadGradient.PLUS_6;

		throw new IllegalArgumentException("Unexpected slope: " + slope_percent);
	}

	// TODO Replace list by a fixed sized structure
	// TODO Refactor the method as a whole
	private List<Tuple<HbefaWarmEmissionFactorKey, Double>> determineInterpolationTriangle(HbefaWarmEmissionFactorKey lowerVClassKey,
																						   HbefaWarmEmissionFactorKey higherVClassKey,
																						   Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple,
																						   Double freeFlowSpeed_kmh,
																						   Double averageSpeed_kmh) {
		// Prepare the x-values
		var efkey_lowerVClass = new HbefaWarmEmissionFactorKey(lowerVClassKey);
		efkey_lowerVClass.setTrafficSituation(FREEFLOW);
		double xLowerVClass = getEf(vehicleInformationTuple, efkey_lowerVClass).getSpeed();

		var efkey_higherVClass = new HbefaWarmEmissionFactorKey(higherVClassKey);
		efkey_higherVClass.setTrafficSituation(FREEFLOW);
		double xHigherVClass = getEf(vehicleInformationTuple, efkey_higherVClass).getSpeed();

		// Prepare the interpolated point
		Tuple<Double, Double> point = new Tuple<>(freeFlowSpeed_kmh, averageSpeed_kmh);

		for(var t : HbefaTrafficSituation.values()){
			// Return error if we could not match any triangle
			if (t == STOPANDGO_HEAVY){
				throw new RuntimeException("Iterated through all Situations and could not determine interpolation triangle!");
			}

			// First triangle
			// (v0, t)
			var efkey0 = new HbefaWarmEmissionFactorKey(lowerVClassKey);
			efkey0.setTrafficSituation(t);

			Tuple<Double, Double> v0 = new Tuple<>(
				xLowerVClass,
				getEf(vehicleInformationTuple, efkey0).getSpeed()
			);

			// (v1, t)
			var efkey1 = new HbefaWarmEmissionFactorKey(higherVClassKey);
			efkey1.setTrafficSituation(t);

			Tuple<Double, Double> v1 = new Tuple<>(
				xHigherVClass,
				getEf(vehicleInformationTuple, efkey1).getSpeed()
			);

			// (v1, t-1)
			var efkey2 = new HbefaWarmEmissionFactorKey(higherVClassKey);
			efkey2.setTrafficSituation(t.getLower());

			Tuple<Double, Double> v2 = new Tuple<>(
				xHigherVClass,
				getEf(vehicleInformationTuple, efkey2).getSpeed()
			);

			// Check if triangle is valid
			List<Double> lambdas = getTriangleLambdas(List.of(v0, v1, v2), point);
			if(lambdas.stream().allMatch(l -> l > (-1e-6) && l < (1+1e-6))){
				return List.of(
					new Tuple<>(efkey0, lambdas.get(0)),
					new Tuple<>(efkey1, lambdas.get(1)),
					new Tuple<>(efkey2, lambdas.get(2))
				);
			}

			// Second triangle (reusing old vars, as they are not needed anymore)
			// (v0, t)
			efkey0 = new HbefaWarmEmissionFactorKey(lowerVClassKey);
			efkey0.setTrafficSituation(t);

			v0 = new Tuple<>(
				xLowerVClass,
				getEf(vehicleInformationTuple, efkey0).getSpeed()
			);

			// (v0, t-1)
			efkey1 = new HbefaWarmEmissionFactorKey(lowerVClassKey);
			efkey1.setTrafficSituation(t.getLower());

			v1 = new Tuple<>(
				xLowerVClass,
				getEf(vehicleInformationTuple, efkey1).getSpeed()
			);

			// (v1, t-1)
			efkey2 = new HbefaWarmEmissionFactorKey(higherVClassKey);
			efkey2.setTrafficSituation(t.getLower());

			v2 = new Tuple<>(
				xHigherVClass,
				getEf(vehicleInformationTuple, efkey2).getSpeed()
			);

			// Check if triangle is valid
			lambdas = getTriangleLambdas(List.of(v0, v1, v2), point);
			if(lambdas.stream().allMatch(l -> l > (-1e-6) && l < (1+1e-6))){
				return List.of(
					new Tuple<>(efkey0, lambdas.get(0)),
					new Tuple<>(efkey1, lambdas.get(1)),
					new Tuple<>(efkey2, lambdas.get(2))
				);
			}
		}

		throw new RuntimeException("Could not determine interpolation triangle!");
	}

	// TODO Replace list by a fixed sized structure
	private List<Double> getTriangleLambdas(List<Tuple<Double, Double>> vertices, Tuple<Double, Double> point){
		double det_T =
			((vertices.get(1).getSecond() - vertices.get(2).getSecond())*(vertices.get(0).getFirst() - vertices.get(2).getFirst())) +
			((vertices.get(2).getFirst() - vertices.get(1).getFirst())*(vertices.get(0).getSecond() - vertices.get(2).getSecond()));

		double lambda0 =
			(((vertices.get(1).getSecond() - vertices.get(2).getSecond())*(point.getFirst() - vertices.get(2).getFirst())) +
			((vertices.get(2).getFirst() - vertices.get(1).getFirst())*(point.getSecond() - vertices.get(2).getSecond()))) /
			det_T;

		double lambda1 =
			(((vertices.get(2).getSecond() - vertices.get(0).getSecond())*(point.getFirst() - vertices.get(2).getFirst())) +
			((vertices.get(0).getFirst() - vertices.get(2).getFirst())*(point.getSecond() - vertices.get(2).getSecond()))) /
			det_T;

		double lambda2 = 1 - lambda0 - lambda1;

		return List.of(lambda0, lambda1, lambda2);
	}

	private HbefaTrafficSituation getLowerTrafficSituation(HbefaWarmEmissionFactorKey efkey, HbefaTrafficSituation higher) {
		HbefaRoadVehicleCategoryKey hbefaRoadVehicleCategoryKey = new HbefaRoadVehicleCategoryKey(efkey);
		Map<HbefaTrafficSituation, Double> trafficSpeeds = this.hbefaRoadTrafficSpeeds.get(hbefaRoadVehicleCategoryKey);

		if(trafficSpeeds.size() != 5){
			throw new RuntimeException("HbefaTable seems to be incomplete or the version has been changed. " +
				"Expected 5 trafficSituations (HBEFA4.1 standard), but got " + trafficSpeeds.size() + ": " + trafficSpeeds.keySet());
		}

		int situation = -1;
		switch (higher){
			case FREEFLOW -> situation=5;
			case HEAVY -> situation=4;
			case SATURATED -> situation=3;
			case STOPANDGO -> situation=2;
			case STOPANDGO_HEAVY -> situation=1;
		}

		if (situation > 4 && trafficSpeeds.containsKey(HEAVY)) {
			return HEAVY;
		} if (situation > 3 && trafficSpeeds.containsKey(SATURATED)) {
			return SATURATED;
		} if (situation > 2 && trafficSpeeds.containsKey(STOPANDGO)) {
			return STOPANDGO;
		} if (situation >= 1 && trafficSpeeds.containsKey(STOPANDGO_HEAVY)) {
			return STOPANDGO_HEAVY;
		}
		throw new IllegalArgumentException("HbefaTrafficSituation " + higher + " seems to be unknown");
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

	private double getFractionInterpolation(HbefaTrafficSituation higher, HbefaTrafficSituation lower, double averageSpeed_kmh,
										Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple,
										HbefaWarmEmissionFactorKey efkey) {

		efkey.setTrafficSituation(higher);
		double highSpeedFromTable_kmh = getEf(vehicleInformationTuple, efkey).getSpeed();

		efkey.setTrafficSituation(lower);
		double lowSpeedFromTable_kmh = getEf(vehicleInformationTuple, efkey).getSpeed();

		double fractionInterpolation;

		if ((averageSpeed_kmh - highSpeedFromTable_kmh) >= -1.0) { // both speeds are assumed to be not very different > only highSpeed on link
			fractionInterpolation = 0.0;
		} else if ((averageSpeed_kmh - lowSpeedFromTable_kmh) <= 0.0) { // averageSpeed is less than lowSpeed > only lowSpeed on link
			fractionInterpolation = 1.0;
		} else {
			fractionInterpolation = lowSpeedFromTable_kmh * (highSpeedFromTable_kmh - averageSpeed_kmh) / (averageSpeed_kmh * (highSpeedFromTable_kmh - lowSpeedFromTable_kmh));
		}

		return fractionInterpolation;
	}

	/// Create a new key for HBEFA3 from a HBEFA4 key
	private HbefaWarmEmissionFactorKey getHbefa3fromHbefa4(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey){
		// Creates a new key, meant to be used in old HBEFA3 tables
		HbefaWarmEmissionFactorKey efkey2 = new HbefaWarmEmissionFactorKey( efkey );
		HbefaVehicleAttributes attribs2 = EmissionUtils.tryRewriteHbefa3toHbefa4( vehicleInformationTuple );
		// put this into a new key ...
		efkey2.setVehicleAttributes( attribs2 );
		return efkey2;
	}

	/// Tries to access the detailed HBEFA table (tries version 4 and 3)
	private HbefaWarmEmissionFactor tryDetailed(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey){
		// Entry Log message
		if ( detailedReadingInfoCnt <= 1 ) {
			logger.info( "Try reading detailed values from detailed warm HBEFA-table" );
			logger.info( Gbl.ONLYONCE );
			logger.info( Gbl.FUTURE_SUPPRESSED );
			detailedReadingInfoCnt++;
		}

		// Try HBEFA4 format
		if ( this.detailedHbefaWarmTable.get( efkey ) != null ) {
			HbefaWarmEmissionFactor ef = this.detailedHbefaWarmTable.get( efkey );
			logger.debug("Lookup result for {} is {}", efkey, ef.toString());
			return ef;
		}

		// HBEFA4 not found, now try HBEFA3 format
		if ( detailedTransformToHbefa4Cnt <= 1 ) {
			logger.info( "try to rewrite from HBEFA3 to HBEFA4 and lookup in detailed table again" );
			logger.info( Gbl.ONLYONCE );
			logger.info( Gbl.FUTURE_SUPPRESSED );
			detailedTransformToHbefa4Cnt++;
		}

		HbefaWarmEmissionFactorKey efkey_hbefa3 = getHbefa3fromHbefa4(vehicleInformationTuple, efkey);

		if ( this.detailedHbefaWarmTable.get( efkey_hbefa3 ) != null ) {
			HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey_hbefa3 );
			logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
			return ef2;
		}

		// If none of the approaches worked, return null to get back to calling function
		return null;
	}

	/// Tries to access the tech-average entry in the detailed HBEFA-table (tries version 4 and 3)
	private HbefaWarmEmissionFactor tryTechnologyAverage(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey){
		// Entry Log message for technology average
		if ( techAverageReadingInfoCnt <= 1 ) {
			logger.info( "Try reading technology-averaged values from detailed warm HBEFA-table using following vehicle attributes: '{}; average; average'", efkey.getVehicleAttributes().getHbefaTechnology());
			logger.info( Gbl.ONLYONCE );
			logger.info( Gbl.FUTURE_SUPPRESSED );
			techAverageReadingInfoCnt++;
		}

		HbefaWarmEmissionFactorKey efkey_hbefa3 = getHbefa3fromHbefa4(vehicleInformationTuple, efkey);

		// Tries "<technology>; average; average":
		if ( ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageElseAbort || ecg.getDetailedVsAverageLookupBehavior() == EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable ) {
			efkey_hbefa3.getVehicleAttributes().setHbefaSizeClass( "average" );
			efkey_hbefa3.getVehicleAttributes().setHbefaEmConcept( "average" );

			if ( this.detailedHbefaWarmTable.get( efkey_hbefa3 ) != null ) {
				HbefaWarmEmissionFactor ef2 = this.detailedHbefaWarmTable.get( efkey_hbefa3 );
				logger.debug("Lookup result for {} is {}", efkey, ef2.toString());
				return ef2;
			}
		}

		// If none of the approaches worked, return null to get back to calling function
		return null;
	}

	/// Tries to access the average HBEFA-table (tries version 4 and 3)
	private HbefaWarmEmissionFactor tryAverage(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey){
		if ( averageReadingInfoCnt <= 1 ) {
			logger.info( "Try reading average values from average warm HBEFA-table" );
			logger.info( Gbl.ONLYONCE );
			logger.info( Gbl.FUTURE_SUPPRESSED );
			averageReadingInfoCnt++;
		}

		//Set VehicleAttributes to '<average>, <average>, <average>'
		HbefaWarmEmissionFactorKey efkey_average = new HbefaWarmEmissionFactorKey(efkey);
		efkey_average.setVehicleAttributes( new HbefaVehicleAttributes() );
		if ( this.avgHbefaWarmTable.get( efkey_average ) != null ) {
			HbefaWarmEmissionFactor ef = this.avgHbefaWarmTable.get( efkey_average );
			logger.debug("Lookup result for {} is {}", efkey_average, ef.toString());
			Gbl.assertNotNull( ef );
			return ef;
		}

		// Average key was not found
		logger.warn("Did not find average emission factor for efkey={}", efkey_average);
		List<HbefaWarmEmissionFactorKey> list = new ArrayList<>( this.avgHbefaWarmTable.keySet() );
		list.sort( Comparator.comparing( HbefaWarmEmissionFactorKey::toString ) );
		for ( HbefaWarmEmissionFactorKey key : list ) {
			logger.warn( key.toString() );
		}

		// If none of the approaches worked, return null to get back to calling function
		return null;
	}

	private void fallbackTechAverageLogWarning(HbefaWarmEmissionFactorKey efkey) {
		if ( fallbackTechAverageWarnCnt <= 1 ) {
			logger.warn("Did not find detailed emission factor for warm efkey={}", efkey);
			logger.warn( "Now trying with technology-average: \"<technology>; average; average\"" );
			logger.warn( Gbl.ONLYONCE );
			logger.warn( Gbl.FUTURE_SUPPRESSED );
			fallbackTechAverageWarnCnt++;
		}
	}

	private void fallbackAverageLogWarning(HbefaWarmEmissionFactorKey efkey) {
		if ( fallbackAverageTableWarnCnt <= 1 ) {
			logger.warn("Did not find technology averaged emission factor for warm efkey={}", efkey);
			logger.warn( "Now trying with setting to vehicle attributes to \"average; average; average\" and try it in the average table" );
			logger.warn( Gbl.ONLYONCE );
			logger.warn( Gbl.FUTURE_SUPPRESSED );
			fallbackAverageTableWarnCnt++;
		}
	}

	private HbefaWarmEmissionFactor getEf(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey) {

		switch ( ecg.getDetailedVsAverageLookupBehavior() ) {
			case onlyTryDetailedElseAbort -> {
				HbefaWarmEmissionFactor ef;
				ef = tryDetailed(vehicleInformationTuple, efkey);
				if (ef != null) return ef;
			}
			case tryDetailedThenTechnologyAverageElseAbort -> {
				HbefaWarmEmissionFactor ef;
				ef = tryDetailed(vehicleInformationTuple, efkey);
				if (ef != null) return ef;

				fallbackTechAverageLogWarning(efkey);

				ef = tryTechnologyAverage(vehicleInformationTuple, efkey);
				if (ef != null) return ef;
			}
			case tryDetailedThenTechnologyAverageThenAverageTable -> {
				HbefaWarmEmissionFactor ef;
				ef = tryDetailed(vehicleInformationTuple, efkey);
				if (ef != null) return null;

				fallbackTechAverageLogWarning(efkey);

				ef = tryTechnologyAverage(vehicleInformationTuple, efkey);
				if (ef != null) return null;

				fallbackAverageLogWarning(efkey);

				ef = tryAverage(vehicleInformationTuple, efkey);
				if (ef != null) return null;

			}
			case directlyTryAverageTable -> {
				HbefaWarmEmissionFactor ef;
				ef = tryAverage(vehicleInformationTuple, efkey);
				if (ef != null) return ef;
			}
			default -> throw new IllegalStateException( "Unexpected value: " + ecg.getDetailedVsAverageLookupBehavior() );
		}

		// If this part of code is reached, the lookup was not successful. Terminate the simulation
		throw new RuntimeException("Was not able to lookup emissions factor in warm table. \n" +
			"Maybe you wanted to look up detailed values and did not specify this in the config \n " +
			"OR \n " +
			"you should use another fallback setting when using detailed calculation \n " +
			"OR \n" +
			"values are missing in your emissions table(s) either average or detailed \n" +
			"OR \n" +
			"... \n\n efkey: " + efkey.toString());
	}



	private HbefaWarmEmissionFactorKey getHigherVClass(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey, double freeFlowSpeed_kmh){
		// TODO This is a temporary implementation which is inefficient and unstable. The final solutions should not determine the keys using a testing-loop but rather use a table
		// TODO The keys also should not be determined from scratch each emission event but rather computed once and then get saved

		HbefaWarmEmissionFactorKey efKeyCopy = new HbefaWarmEmissionFactorKey(efkey);
		efKeyCopy.setTrafficSituation(FREEFLOW);

		Function<String, String> removeVClass = (s) -> s.substring(0, s.lastIndexOf("/"));
		Function<String, String> getVClass = (s) -> s.substring(s.lastIndexOf("/")+1);

		if ( !hbefaRoadVClasses.containsKey(efKeyCopy) ){
			Set<HbefaWarmEmissionFactorKey> vClasses = detailedHbefaWarmTable.keySet().stream().filter( k ->
				k.getVehicleCategory().equals(efKeyCopy.getVehicleCategory()) &&
				k.getVehicleAttributes().equals(efKeyCopy.getVehicleAttributes()) &&
				k.getComponent().equals(efKeyCopy.getComponent()) &&
				k.getTrafficSituation().equals(efKeyCopy.getTrafficSituation()) &&
				k.getRoadCategory().startsWith(removeVClass.apply(efKeyCopy.getRoadCategory()))
			).collect(Collectors.toSet());
//			System.out.println(efKeyCopy + ": " + vClasses + "\n\n"); // TODO DEBUG ONLY

			hbefaRoadVClasses.put(
				efKeyCopy,
				vClasses.stream().sorted(
					(k1, k2) -> {
						var k1VClass = getVClass.apply(k1.getRoadCategory());
						var k2VClass = getVClass.apply(k2.getRoadCategory());

						if(k1VClass.matches("[0-9]+") && k2VClass.matches("[0-9]+") ){
							return Integer.valueOf(k1VClass).compareTo(Integer.valueOf(k2VClass));
						} else {
							return k1.getRoadCategory().compareTo(k2.getRoadCategory());
						}
					}
				).toList()
			);
		}

		var keys = hbefaRoadVClasses.get(efKeyCopy);

		// Round to next higher v-class
		double lastVValue = Double.POSITIVE_INFINITY;

		for(final var k : keys){
			var ef = getEf(vehicleInformationTuple, k);
			if (lastVValue < freeFlowSpeed_kmh && freeFlowSpeed_kmh < ef.getSpeed()){
				return k;
			}
			lastVValue = ef.getSpeed();
		}

		// If we could not match the roadType accurately, we get the nearest possible data
		// Check left outer space
		if (freeFlowSpeed_kmh < getEf(vehicleInformationTuple, keys.getFirst()).getSpeed()){
			return keys.getFirst();
		}

		// Check right outer space
		if (getEf(vehicleInformationTuple, keys.getLast()).getSpeed() < freeFlowSpeed_kmh){
			return keys.getLast();
		}

		throw new RuntimeException("Could not find viable class for given RoadCategory " + efkey.getRoadCategory() + " that matches the freespeed " + freeFlowSpeed_kmh + "km/h");
	}

	private HbefaWarmEmissionFactorKey getLowerVClass(Tuple<HbefaVehicleCategory, HbefaVehicleAttributes> vehicleInformationTuple, HbefaWarmEmissionFactorKey efkey, double freeFlowSpeed_kmh){
		// TODO This is a temporary and inefficient implementation. The final solutions should not determine the keys each time but save them

		HbefaWarmEmissionFactorKey efKeyCopy = new HbefaWarmEmissionFactorKey(efkey);
		efKeyCopy.setTrafficSituation(FREEFLOW);

		var keys = hbefaRoadVClasses.get(efKeyCopy).reversed();

		// Round to next higher v-class
		double lastVValue = Double.NEGATIVE_INFINITY;

		for(final var k : keys){
			var ef = getEf(vehicleInformationTuple, k);
			if( ef != null){
				if (ef.getSpeed() < freeFlowSpeed_kmh && freeFlowSpeed_kmh < lastVValue){
					return k;
				}
				lastVValue = ef.getSpeed();
			}
		}

		// If we could not match the roadType accurately, we get the nearest possible data
		// Check left outer space
		if (freeFlowSpeed_kmh < getEf(vehicleInformationTuple, keys.getLast()).getSpeed()){
			return keys.getLast();
		}

		// Check right outer space
		if (getEf(vehicleInformationTuple, keys.getFirst()).getSpeed() < freeFlowSpeed_kmh){
			return keys.getFirst();
		}

		throw new RuntimeException("Could not find viable class for given RoadCategory " + efkey.getRoadCategory());
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
			if (Math.abs(averageSpeed_kmh - trafficSpeeds.get(FREEFLOW)) > 1e-6) { //handle case testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6
				trafficSituation = STOPANDGO;
			}
		}
		/*FIXME The following lines should be added to account for the HBEFA 4.1's additional traffic situation,
		   but it currently causes a test failure (jwj, Nov'20) */
		if (trafficSpeeds.containsKey(STOPANDGO_HEAVY) && averageSpeed_kmh <= trafficSpeeds.get(STOPANDGO_HEAVY)) {
			if (Math.abs(averageSpeed_kmh - trafficSpeeds.get(FREEFLOW)) > 1e-6) { //handle case testCheckVehicleInfoAndCalculateWarmEmissions_and_throwWarmEmissionEvent6
				trafficSituation = STOPANDGO_HEAVY;
			}
		}
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
