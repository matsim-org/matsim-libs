/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public abstract class HbefaTables {

    private static final Logger logger = LogManager.getLogger(HbefaTables.class);

	private static int getterErrorCnt = 0;

	private static final TriFunction<String, CSVRecord, String, String> safeGetLambda = (key, record, defaultValue) -> {
		try {
			return record.get(key);
		} catch (IllegalArgumentException e){
			if (getterErrorCnt < 1){
				logger.warn(e);
				getterErrorCnt++;
			}
		}
		return defaultValue;
	};

    static Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> loadAverageWarm(URL file, EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments) {
        return load(
			file,
			duplicateSubsegments,
			HbefaTables::createWarmKey,
			record -> new HbefaWarmEmissionFactor(Double.parseDouble(record.get("EFA_weighted")), Double.parseDouble(record.get("V_weighted"))),
			record -> Double.parseDouble(safeGetLambda.apply("%OfSubsegment", record, "0.0")),
			HbefaTables::aggregateWarmKeys
		);
    }

    static Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> loadDetailedWarm(URL file, EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments) {
        return load(
			file,
			duplicateSubsegments,
			record -> {
				var key = createWarmKey(record);
				setCommonDetailedParametersOnKey(key, record);
				return key;
        	},
			record -> new HbefaWarmEmissionFactor(Double.parseDouble(record.get("EFA")), Double.parseDouble(record.get("V"))),
			record -> Double.parseDouble(safeGetLambda.apply("%OfSubsegment", record, "0.0")),
			HbefaTables::aggregateWarmKeys
		);
    }

    static Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> loadAverageCold(URL file, EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments) {
		// TODO Check if %OfSubsegment exists for cold-hbefa tables

        return load(
			file,
			duplicateSubsegments,
			HbefaTables::createColdKey,
			record -> new HbefaColdEmissionFactor(Double.parseDouble(record.get("EFA_weighted"))),
			record -> Double.parseDouble(safeGetLambda.apply("%OfSubsegment", record, "0.0")),
			HbefaTables::aggregateColdKeys
		);
    }

    static Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> loadDetailedCold(URL file, EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments) {
		// TODO Check if %OfSubsegment exists for cold-hbefa tables

        return load(file,
			duplicateSubsegments,
			record -> {
				var key = createColdKey(record);
				setCommonDetailedParametersOnKey(key, record);
				return key;
        	},
			record -> new HbefaColdEmissionFactor(Double.parseDouble(record.get("EFA"))),
			record -> Double.parseDouble(safeGetLambda.apply("%OfSubsegment", record, "0.0")),
			HbefaTables::aggregateColdKeys
		);
    }

    private static <K extends HbefaEmissionFactorKey, V extends HbefaEmissionFactor> Map<K, V> load(
			URL file,
			EmissionsConfigGroup.DuplicateSubsegments duplicateSubsegments,
			Function<CSVRecord, K> createKey,
			Function<CSVRecord, V> createValue,
			Function<CSVRecord, Double> createFleetComposition,
			TriFunction<V, V, Double, V> aggregateHbefaEmissionFactors) {

        Map<K, V> result = new HashMap<>();
		Map<K, Double> totalEntryWeight = new HashMap<>();

        try (var reader = IOUtils.getBufferedReader(file);
             var parser = CSVParser.parse(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader())) {

            for (var record : parser) {
                var key = createKey.apply(record);
                var value = createValue.apply(record);
				var fleetCompositionWeight = createFleetComposition.apply(record);

				// Check if this key was already set -> Check for duplicate
				if (result.containsKey(key)){
					// We have a duplicate, Check for the current duplicateSubsegment setting in the emissions config group

					switch(duplicateSubsegments){
						case crashIfDuplicateExists -> {
							logger.error("""
								Tried to overwrite an already existing emission-key! This will make emission-results useless, as the original entry will be lost!\s
								This error usually occurs when the Subsegment columns contains additional information, that is not integrated into the EmConcept column. MATSim will then treat both entries as identical.\s
								The duplicate key:  {}
								The current record: {}
								To temporarily bypass this problem you can set the duplicateSubsegments in the EmissionsConfigGroup to useFirstDuplicate or overwriteOldDuplicates.\s
								WARNING: using the first or last entry of the subsegment will cause wrong emission results!
								To run an actual simulation you should use the aggregateByFleetComposition setting.""", key, record);
							throw new RuntimeException("Terminating due to crashIfDuplicateExists rule being active");
						}
						case useFirstDuplicate -> {
							logger.warn("""
								Tried to overwrite an already existing emission-key! The procedure for duplicates is currently set to useFirstDuplicate. This is meant for debugging or testing only!\s
								If you want to carry out a simulation, fix the hbefa-table or change the duplicateSubsegments setting to aggregateByFleetComposition using EmissionsConfigGroup.setDuplicateSubsegments()\s
								The duplicate key:  {}
								The current record: {}
								""", key, record);
							continue;
						}
						case overwriteOldDuplicates -> logger.warn("""
							Tried to overwrite an already existing emission-key! The procedure for duplicates is currently set to overwriteOldDuplicates. This is meant for debugging or testing only!\s
							If you want to carry out a simulation, fix the hbefa-table or change the duplicateSubsegments setting to aggregateByFleetComposition using EmissionsConfigGroup.setDuplicateSubsegments()
							The duplicate key:  {}
							The current record: {}
							""", key, record);
						case aggregateByFleetComposition ->
							result.put(key, aggregateHbefaEmissionFactors.apply(result.get(key), value, (fleetCompositionWeight/(totalEntryWeight.get(key) + fleetCompositionWeight)) ));
					}
				}

                result.put(key, value);
				totalEntryWeight.putIfAbsent(key, fleetCompositionWeight);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

	static HbefaWarmEmissionFactor aggregateWarmKeys(HbefaWarmEmissionFactor v1, HbefaWarmEmissionFactor v2, double newValueWeight){
		double newFactor = (1 - newValueWeight) * v1.getFactor() + newValueWeight * v2.getFactor();
		double newSpeed = (1 - newValueWeight) * v1.getSpeed() + newValueWeight * v2.getSpeed();

		return new HbefaWarmEmissionFactor(newFactor, newSpeed);
	}

	static HbefaColdEmissionFactor aggregateColdKeys(HbefaColdEmissionFactor v1, HbefaColdEmissionFactor v2, double newValueWeight){
		double newFactor = (1 - newValueWeight) * v1.getFactor() + newValueWeight * v2.getFactor();

		return new HbefaColdEmissionFactor(newFactor);
	}

    static HbefaWarmEmissionFactorKey createWarmKey(CSVRecord record) {
        var key = new HbefaWarmEmissionFactorKey();
        setCommonParametersOnKey(key, record);
        var trafficSit = record.get("TrafficSit");
        key.setRoadCategory(trafficSit.substring(0, trafficSit.lastIndexOf('/')));
        key.setRoadGradient(mapString2HbefaRoadGradient(record.get("Gradient")));
        key.setTrafficSituation(mapString2HbefaTrafficSituation(trafficSit));
        key.setVehicleAttributes(new HbefaVehicleAttributes());
        return key;
    }

	static HbefaColdEmissionFactorKey createColdKey(CSVRecord record) {
        var key = new HbefaColdEmissionFactorKey();
        setCommonParametersOnKey(key, record);
        key.setParkingTime(mapAmbientCondPattern2ParkingTime(record.get("AmbientCondPattern")));
        key.setDistance(mapAmbientCondPattern2Distance(record.get("AmbientCondPattern")));
        key.setVehicleAttributes(new HbefaVehicleAttributes());
        return key;
    }

    private static <K extends HbefaEmissionFactorKey> void setCommonParametersOnKey(K key, CSVRecord record) {

        key.setComponent(EmissionUtils.getPollutant(record.get("Component")));
        key.setVehicleCategory(EmissionUtils.mapString2HbefaVehicleCategory(record.get("VehCat")));
    }

    private static <K extends HbefaEmissionFactorKey> void setCommonDetailedParametersOnKey(K key, CSVRecord record) {
        key.getVehicleAttributes().setHbefaTechnology(record.get("Technology"));
        key.getVehicleAttributes().setHbefaEmConcept(record.get("EmConcept"));
        key.getVehicleAttributes().setHbefaSizeClass(record.get("SizeClasse"));
    }

    private static HbefaTrafficSituation mapString2HbefaTrafficSituation(String string) {

        if (string.endsWith("Freeflow")) return HbefaTrafficSituation.FREEFLOW;
        else if (string.endsWith("Heavy")) return HbefaTrafficSituation.HEAVY;
        else if (string.endsWith("Satur.")) return HbefaTrafficSituation.SATURATED;
        else if (string.endsWith("St+Go")) return HbefaTrafficSituation.STOPANDGO;
        else if (string.endsWith("St+Go2")) return HbefaTrafficSituation.STOPANDGO_HEAVY;
        else {
            logger.warn("Could not map String {} to any HbefaTrafficSituation; please check syntax in hbefa input file.", string);
            throw new RuntimeException();
        }
    }

	private static HbefaRoadGradient mapString2HbefaRoadGradient(String string) {
		return switch (string){
			case "-6%" -> HbefaRoadGradient.MINUS_6;
			case "-4%" -> HbefaRoadGradient.MINUS_4;
			case "-2%" -> HbefaRoadGradient.MINUS_2;
			case "0%" -> HbefaRoadGradient.ZERO;
			case "+2%" -> HbefaRoadGradient.PLUS_2;
			case "+4%" -> HbefaRoadGradient.PLUS_4;
			case "+6%" -> HbefaRoadGradient.PLUS_6;
			case "+/-2%" -> HbefaRoadGradient.PLUS_MINUS_2;
			case "+/-4%" -> HbefaRoadGradient.PLUS_MINUS_4;
			case "+/-6%" -> HbefaRoadGradient.PLUS_MINUS_6;

			default -> {
				logger.warn("Could not map String {} to any HbefaRoadGradient; please check syntax in hbefa input file.", string);
				throw new IllegalArgumentException("Unknown road gradient identifier while parsing HBEFA table: " + string);
			}
		};
	}

    private static int mapAmbientCondPattern2Distance(String string) {
        String distanceString = string.split(",")[2];
        String upperbound = distanceString.split("-")[1];
        return Integer.parseInt(upperbound.split("k")[0]);
    }

    private static int mapAmbientCondPattern2ParkingTime(String string) {

        String parkingTimeString = string.split(",")[1];
        if (parkingTimeString.equals(">12h")) {
            return 13;
        } else {
            String upperbound = parkingTimeString.split("-")[1];
            return Integer.parseInt(upperbound.split("h")[0]);
        }
    }
}
