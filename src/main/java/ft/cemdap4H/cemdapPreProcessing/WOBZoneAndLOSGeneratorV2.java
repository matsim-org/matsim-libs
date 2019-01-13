/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package ft.cemdap4H.cemdapPreProcessing;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.vsp.demandde.cemdap.LogToOutputSaver;

import java.io.*;
import java.util.*;

/**
 * @author dziemke
 */
public class WOBZoneAndLOSGeneratorV2 {
    private static final Logger LOG = Logger.getLogger(WOBZoneAndLOSGeneratorV2.class);


    // Storage objects
    private final Set<String> municipalities = new HashSet<>();
    private final Set<String> zones = new HashSet<>();
    private final Map<String, Geometry> zoneMap = new HashMap<>();
    private final Map<String, Map<String, Double>> zone2ZoneDistanceMap = new HashMap<>();
    private final Map<String, Map<String, Integer>> zone2ZoneAdjacencyMap = new HashMap<>();
    private final String outputBase;
    private final Map<String, String> new2oldZones = new HashMap<>();


    // Parameters
    private double defaultIntraZoneDistance = 1.72; // in miles; equals 2.76km.
    private double beelineDistanceFactor = 1.3;
    private double durantionDistanceOffPeakRatio_min_mile = 1.6; // based on computations in sample dataset; equals ca. 60km/h
    private double durantionDistancePeakRatio_min_mile = 1.9; // based on computations in sample dataset; equals ca. 50km/h
    private double costDistanceRatio_USD_mile = 0.072; // based on computations in sample dataset; equals 0.045USD/km


    public WOBZoneAndLOSGeneratorV2(String[] commuterFilesOutgoing, String shapeFile, String shapefile2, String outputBase) {
        LogToOutputSaver.setOutputDirectory(outputBase);

        this.outputBase = outputBase;

        readMunicipalities(commuterFilesOutgoing);
        readShape(shapeFile, "AGS");
        readShape(shapefile2, "NO");

        Geometry vwzone = this.zoneMap.get("350");
        this.zoneMap.put("9999990", vwzone);
        this.zoneMap.put("9999991", vwzone);
//		this.zoneMap.put("9999992", vwzone);
//		this.zoneMap.put("9999993", vwzone);
//		this.zoneMap.put("9999994", vwzone);
//		this.zoneMap.put("9999995", vwzone);
//		this.zoneMap.put("9999996", vwzone);
        this.zones.add("9999990");
        this.zones.add("9999991");
//		this.zones.add("9999992");
//		this.zones.add("9999993");
//		this.zones.add("9999994");
//		this.zones.add("9999995");
//		this.zones.add("9999996");


    }

    public String getConvertedZone(String zone0) {
        if (this.zones.contains(zone0)) {
            return zone0;
        } else if (this.new2oldZones.containsKey(zone0)) {
            return this.new2oldZones.get(zone0);
        } else throw new RuntimeException(zone0 + " does not exist");
    }


    public void generateSupply() {
        compareIdsInShapefileAndCommuterFiles();
        computeAndStoreZone2ZoneDistances();
        writeZone2ZoneFile();
        writeZonesFile();
        writeLOSOffPkAMFile();
        writeLOSPeakAMFile();
    }


    private void readMunicipalities(String[] commuterFilesOutgoing) {
        for (String commuterFileOutgoing : commuterFilesOutgoing) {
            WOBCommuterFileReaderV2 commuterFileReader = new WOBCommuterFileReaderV2(commuterFileOutgoing, "\t");

            Set<String> currentMunicipalities = commuterFileReader.getMunicipalities();
            this.municipalities.addAll(currentMunicipalities);
        }
    }


    private void readShape(String shapeFile, String featureKeyInShapeFile) {
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        for (SimpleFeature feature : features) {
            String id = feature.getAttribute(featureKeyInShapeFile).toString();
            if (featureKeyInShapeFile.equals("AGS")) {
                String newId = feature.getAttribute("RS_ALT").toString();
                this.new2oldZones.put(newId, id);
            }
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            this.zones.add(id);
            this.zoneMap.put(id, geometry);
        }
    }


    private void compareIdsInShapefileAndCommuterFiles() {
        LOG.info("Municipality set has " + municipalities.size() + " elements.");
        LOG.info("Zones set has " + zones.size() + " elements.");
        for (String key : zones) {
            if (!this.municipalities.contains(key)) {
                LOG.warn("Zone from shapefile not in commuter relations; zone = " + key);
            }
        }
        for (String key : municipalities) {
            if (!this.zones.contains(key)) {
                LOG.warn("Zone from commuter relations not in shapes; zone = " + key);
            }
        }
    }


    private void computeAndStoreZone2ZoneDistances() {
        LOG.info("Start distance and adjacency computations.");
        LOG.info(this.zones.size() * this.zones.size() + " computations will be performed.");
        int counter = 0;
        for (String originId : this.zones) {
            Map<String, Double> toZoneDistanceMap = new HashMap<>();
            Map<String, Integer> toZoneAdjacencyMap = new HashMap<>();
            for (String destinationId : this.zones) {
                counter++;
                if (counter % 100000 == 0) {
                    LOG.info(counter + " relations computed.");
                }
                int adjacent;
                double distance_mi;
                double temp = 0.;

                if (originId.equals(destinationId)) { // internal traffic inside zone
                    distance_mi = defaultIntraZoneDistance * beelineDistanceFactor;
                    adjacent = 0;
                } else {
                    Geometry originGeometry = this.zoneMap.get(originId);
                    Coord originCoord = new Coord(originGeometry.getCentroid().getCoordinate().x, originGeometry.getCentroid().getCoordinate().y);

                    Geometry destinationGeometry = this.zoneMap.get(destinationId);
                    Coord destinationCoord = new Coord(destinationGeometry.getCentroid().getCoordinate().x, destinationGeometry.getCentroid().getCoordinate().y);


                    double distance_m = CoordUtils.calcEuclideanDistance(originCoord, destinationCoord);

                    distance_mi = distance_m / 1609.344 * beelineDistanceFactor; // Convert from meters to miles

                    if (originGeometry.touches(destinationGeometry)) {
                        adjacent = 1;
                    } else {
                        adjacent = 0;
                    }
                }
                temp = Math.round(distance_mi * 100); // Round to two decimal places
                double distanceRounded_mi = temp / 100;
                toZoneDistanceMap.put(destinationId, distanceRounded_mi);
                toZoneAdjacencyMap.put(destinationId, adjacent);
            }
            this.zone2ZoneDistanceMap.put(originId, toZoneDistanceMap);
            this.zone2ZoneAdjacencyMap.put(originId, toZoneAdjacencyMap);
        }
        LOG.info("Finished distance and adjacency computations.");
    }


    private void writeZone2ZoneFile() {
        BufferedWriter bufferedWriterZone2Zone = null;
        try {
            File zone2ZoneFile = new File(this.outputBase + "zone2zone.dat");
            FileWriter fileWriterZone2Zone = new FileWriter(zone2ZoneFile);
            bufferedWriterZone2Zone = new BufferedWriter(fileWriterZone2Zone);

            for (String originId : this.zones) {
                for (String destinationId : this.zones) {
                    double distance_mi = this.zone2ZoneDistanceMap.get(originId).get(destinationId);
                    int adjacent = this.zone2ZoneAdjacencyMap.get(originId).get(destinationId);

                    // 4 columns
                    bufferedWriterZone2Zone.write(originId + "\t" + destinationId + "\t" + adjacent + "\t" + distance_mi);
                    bufferedWriterZone2Zone.newLine();
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriterZone2Zone != null) {
                    bufferedWriterZone2Zone.flush();
                    bufferedWriterZone2Zone.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Zone2Zone file written.");
    }


    private void writeZonesFile() {
        BufferedWriter bufferedWriterZones = null;
        try {
            File zonesFile = new File(this.outputBase + "zones.dat");
            FileWriter fileWriterZones = new FileWriter(zonesFile);
            bufferedWriterZones = new BufferedWriter(fileWriterZones);

            for (String zoneId : this.zones) {
                // 45 columns
                bufferedWriterZones.write(zoneId + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
                        + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
                        + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
                        + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
                        + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
                        + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0);
                bufferedWriterZones.newLine();
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriterZones != null) {
                    bufferedWriterZones.flush();
                    bufferedWriterZones.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("Zones file written.");
    }


    private void writeLOSOffPkAMFile() {
        BufferedWriter bufferedWriterLos = null;
        try {
            File losFile = new File(this.outputBase + "losoffpkam.dat");
            FileWriter fileWriterLos = new FileWriter(losFile);
            bufferedWriterLos = new BufferedWriter(fileWriterLos);

            double temp = 0.0;

            for (String originId : this.zones) {
                for (String destinationId : this.zones) {

                    int inSameZone = 0;
                    if (originId.equals(destinationId)) {
                        inSameZone = 1;
                    }

                    double distance_mi = this.zone2ZoneDistanceMap.get(originId).get(destinationId);
                    int adjacent = this.zone2ZoneAdjacencyMap.get(originId).get(destinationId);

                    double driveAloneIVTT_min = distance_mi * durantionDistanceOffPeakRatio_min_mile;
                    temp = Math.round(driveAloneIVTT_min * 100); // Round to two decimal places
                    driveAloneIVTT_min = temp / 100;

                    double driveAloneCost_USD = distance_mi * costDistanceRatio_USD_mile;
                    temp = Math.round(driveAloneCost_USD * 100); // Round to two decimal places
                    driveAloneCost_USD = temp / 100;

                    // 14 columns
                    bufferedWriterLos.write(originId + "\t" + destinationId +
                            "\t" + inSameZone + "\t" + adjacent + "\t" + distance_mi + "\t" + driveAloneIVTT_min
                            + "\t" + 3.1 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + driveAloneCost_USD
                            + "\t" + driveAloneIVTT_min + "\t" + driveAloneCost_USD);
                    bufferedWriterLos.newLine();
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriterLos != null) {
                    bufferedWriterLos.flush();
                    bufferedWriterLos.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("LOSOffPkAM file written.");
    }


    private void writeLOSPeakAMFile() {
        BufferedWriter bufferedWriterLos = null;
        try {
            File losFile = new File(this.outputBase + "lospeakam.dat");
            FileWriter fileWriterLos = new FileWriter(losFile);
            bufferedWriterLos = new BufferedWriter(fileWriterLos);

            double temp = 0.0;

            for (String originId : this.zones) {
                for (String destinationId : this.zones) {

                    int inSameZone = 0;
                    if (originId.equals(destinationId)) {
                        inSameZone = 1;
                    }

                    double distance_mi = this.zone2ZoneDistanceMap.get(originId).get(destinationId);
                    int adjacent = this.zone2ZoneAdjacencyMap.get(originId).get(destinationId);

                    double driveAloneIVTT_min = distance_mi * durantionDistancePeakRatio_min_mile;
                    temp = Math.round(driveAloneIVTT_min * 100); // Round to two decimal places
                    driveAloneIVTT_min = temp / 100;

                    double driveAloneCost_USD = distance_mi * costDistanceRatio_USD_mile;
                    temp = Math.round(driveAloneCost_USD * 100); // Round to two decimal places
                    driveAloneCost_USD = temp / 100;

                    // 14 columns
                    bufferedWriterLos.write(originId + "\t" + destinationId + "\t" + inSameZone
                            + "\t" + adjacent + "\t" + distance_mi + "\t" + driveAloneIVTT_min + "\t" + 3.1
                            + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + driveAloneCost_USD
                            + "\t" + driveAloneIVTT_min + "\t" + driveAloneCost_USD);
                    bufferedWriterLos.newLine();
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bufferedWriterLos != null) {
                    bufferedWriterLos.flush();
                    bufferedWriterLos.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("LOSPeakAM file written.");
    }


    public void setDefaultIntraZoneDistance(double defaultIntraZoneDistance) {
        this.defaultIntraZoneDistance = defaultIntraZoneDistance;
    }


    public void setBeelineDistanceFactor(double beelineDistanceFactor) {
        this.beelineDistanceFactor = beelineDistanceFactor;
    }


    public void setDurantionDistanceOffPeakRatio_min_mile(double durantionDistanceOffPeakRatio_min_mile) {
        this.durantionDistanceOffPeakRatio_min_mile = durantionDistanceOffPeakRatio_min_mile;
    }


    public void setDurantionDistancePeakRatio_min_mile(double durantionDistancePeakRatio_min_mile) {
        this.durantionDistancePeakRatio_min_mile = durantionDistancePeakRatio_min_mile;
    }


    public void setCostDistanceRatio_USD_mile(double costDistanceRatio_USD_mile) {
        this.costDistanceRatio_USD_mile = costDistanceRatio_USD_mile;
    }
}