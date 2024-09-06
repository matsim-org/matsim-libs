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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;


public abstract class HbefaTables {

    private static final Logger logger = LogManager.getLogger(HbefaTables.class);

    static Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> loadAverageWarm(URL file) {

        return load(file, HbefaTables::createWarmKey, record -> {
            var factor = Double.parseDouble(record.get("EFA_weighted"));
            var speed = Double.parseDouble(record.get("V_weighted"));
            return new HbefaWarmEmissionFactor(factor, speed);
        });
    }

    static Map<HbefaWarmEmissionFactorKey, HbefaWarmEmissionFactor> loadDetailedWarm(URL file) {
        return load(file, record -> {
            var key = createWarmKey(record);
            setCommonDetailedParametersOnKey(key, record);
            return key;
        }, record -> new HbefaWarmEmissionFactor(Double.parseDouble(record.get("EFA")), Double.parseDouble(record.get("V"))));
    }

    static Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> loadAverageCold(URL file) {
        return load(file, HbefaTables::createColdKey, record -> new HbefaColdEmissionFactor(Double.parseDouble(record.get("EFA_weighted"))));
    }

    static Map<HbefaColdEmissionFactorKey, HbefaColdEmissionFactor> loadDetailedCold(URL file) {
        return load(file, record -> {
            var key = createColdKey(record);
            setCommonDetailedParametersOnKey(key, record);
            return key;
        }, record -> new HbefaColdEmissionFactor(Double.parseDouble(record.get("EFA"))));
    }

    private static <K extends HbefaEmissionFactorKey, V extends HbefaEmissionFactor> Map<K, V> load(URL file, Function<CSVRecord, K> createKey, Function<CSVRecord, V> createValue) {
        Map<K, V> result = new HashMap<>();

        try (var reader = IOUtils.getBufferedReader(file);
             var parser = CSVParser.parse(reader, CSVFormat.newFormat(';').withFirstRecordAsHeader())) {

            for (var record : parser) {

                var key = createKey.apply(record);
                var value = createValue.apply(record);
                result.put(key, value);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static HbefaWarmEmissionFactorKey createWarmKey(CSVRecord record) {
        var key = new HbefaWarmEmissionFactorKey();
        setCommonParametersOnKey(key, record);
        var trafficSit = record.get("TrafficSit");
        key.setRoadCategory(trafficSit.substring(0, trafficSit.lastIndexOf('/')));
        key.setTrafficSituation(mapString2HbefaTrafficSituation(trafficSit));
        key.setVehicleAttributes(new HbefaVehicleAttributes());
        return key;
    }

    private static HbefaColdEmissionFactorKey createColdKey(CSVRecord record) {
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
