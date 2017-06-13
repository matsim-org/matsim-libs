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

package playground.dziemke.cemdapMatsimCadyts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.dziemke.utils.LogToOutputSaver;
import playground.vsp.demandde.cemdapMATSimCadyts.CommuterFileReaderV2;

/**
 * @author dziemke
 */
public class ZoneAndLOSGeneratorV2 {
	private static final Logger LOG = Logger.getLogger(ZoneAndLOSGeneratorV2.class);

	// Input and output
	private static final String commuterFileBase = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/";
	private static final String commuterFileOutgoing1 = commuterFileBase + "Berlin_2009/B2009Ga.txt";
	private static final String commuterFileOutgoing2 = commuterFileBase + "Brandenburg_2009/Teil1BR2009Ga.txt";
	private static final String commuterFileOutgoing3 = commuterFileBase + "Brandenburg_2009/Teil2BR2009Ga.txt";
	private static final String commuterFileOutgoing4 = commuterFileBase + "Brandenburg_2009/Teil3BR2009Ga.txt";
	
	private static final String shapeFile = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/gemeindenLOR_DHDN_GK4.shp";
	
	private static final String outputBase = "../../../shared-svn/studies/countries/de/berlin_scenario_2016/cemdap_input/200/";
	private static final String outputFileZone2Zone = outputBase + "zone2zone.dat";
	private static final String outputFileZones = outputBase + "zones.dat";
	private static final String outputFileLosOffPkAM = outputBase + "losoffpkam.dat";
	private static final String outputFileLosPeakAM = outputBase + "lospeakam.dat";
	
	// Parameters
	private static final double defaultIntraZoneDistance = 1.72; // in miles; equals 2.76km.
	private static final double beelineDistanceFactor = 1.3;
	private static final double durantionDistanceOffPeakRatio_min_mile = 1.6; // based on computations in sample dataset; equals ca. 60km/h
	private static final double durantionDistancePeakRatio_min_mile = 1.9; // based on computations in sample dataset; equals ca. 50km/h
	private static final double costDistanceRatio_USD_mile = 0.072; // based on computations in sample dataset; equals 0.045USD/km
	
	// Storage objects
	private final String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4};
	private final Set<String> municipalities = new HashSet<>();
	private final List<Integer> zones = new LinkedList<>();
	private final Map<Integer, Geometry> zoneMap = new HashMap<>();
	private final Map<Integer, Map<Integer, Double>> zone2ZoneDistanceMap = new HashMap<>();
	private final Map<Integer, Map<Integer, Integer>> zone2ZoneAdjacencyMap = new HashMap<>();

	
	public static void main(String[] args) {
		new ZoneAndLOSGeneratorV2();
	}

	
	public ZoneAndLOSGeneratorV2() {
		LogToOutputSaver.setOutputDirectory(outputBase);
		readMunicipalities();
		readShape();
		compareIdsInShapefileAndCommuterFiles();
		computeAndStoreZone2ZoneDistances();
		writeZone2ZoneFile();
		writeZonesFile();
		writeLOSOffPkAMFile();
		writeLOSPeakAMFile();
	}
	
	
	private void readMunicipalities() {
		for (String commuterFileOutgoing : commuterFilesOutgoing) {
			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, "\t");
			Set<String> currentMunicipalities = commuterFileReader.getMunicipalities();
			municipalities.addAll(currentMunicipalities);
		}
	}

	
	private void readShape() {
		Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			Integer id = Integer.parseInt((String) feature.getAttribute("NR"));
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}
	
	
	private void compareIdsInShapefileAndCommuterFiles() {
		LOG.info("Municipality set has " + municipalities.size() + " elements.");
		LOG.info("Zones set has " + zones.size() + " elements.");
		for (Integer key : zones) {
			if (!this.municipalities.contains(key.toString())) {
				LOG.info("Zone from shapefile not in commuter relations; zone = " + key);
			}
		}
		for (String key : municipalities) {
			if (!zones.contains(Integer.parseInt(key))) {
				LOG.info("Zone from commuter relations not in shapes; zone = " + key);
			}
		}
	}
	
	
	private void computeAndStoreZone2ZoneDistances() {
		LOG.info("Start distance and adjacency computations.");
		LOG.info(zones.size() * zones.size() + " computations will be performed.");
		int counter = 0;
		for (int originId : zones) {
			Map<Integer,Double> toZoneDistanceMap = new HashMap<>();
			Map<Integer,Integer> toZoneAdjacencyMap = new HashMap<>();
			for (int destinationId : zones) {
				counter++;
				if (counter%1000 == 0) {
					LOG.info(counter + " relations computed.");
				}
				int adjacent;
				double distance_mi;
				double temp = 0.;
				
				if (originId == destinationId) { // internal traffic inside zone
					distance_mi = defaultIntraZoneDistance * beelineDistanceFactor;
					adjacent = 0;
				} else {
					Geometry originGeometry = zoneMap.get(originId);
					Coord originCoord = new Coord(originGeometry.getCentroid().getCoordinate().x, originGeometry.getCentroid().getCoordinate().y);
							
					Geometry destinationGeometry = zoneMap.get(destinationId);
					Coord destinationCoord = new Coord(destinationGeometry.getCentroid().getCoordinate().x, destinationGeometry.getCentroid().getCoordinate().y);
					
					double distanceX_m = Math.abs(originCoord.getX() - destinationCoord.getX());
					double distanceY_m = Math.abs(originCoord.getY() - destinationCoord.getY());
					double distance_m = Math.sqrt(distanceX_m * distanceX_m + distanceY_m * distanceY_m);
					
					// Convert from meters to miles
					distance_mi = distance_m / 1609.344 * beelineDistanceFactor;
    				
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
			zone2ZoneDistanceMap.put(originId, toZoneDistanceMap);
			zone2ZoneAdjacencyMap.put(originId, toZoneAdjacencyMap);
		}
		LOG.info("Finised distance and adjacency computations.");
	}
		
	
	public void writeZone2ZoneFile() {
		BufferedWriter bufferedWriterZone2Zone = null;
		
		try {
            File zone2ZoneFile = new File(outputFileZone2Zone);
    		FileWriter fileWriterZone2Zone = new FileWriter(zone2ZoneFile);
    		bufferedWriterZone2Zone = new BufferedWriter(fileWriterZone2Zone);
    		
    		for (int originId : zones) {
    			for (int destinationId : zones) {
    				double distance_mi = zone2ZoneDistanceMap.get(originId).get(destinationId);
    				int adjacent = zone2ZoneAdjacencyMap.get(originId).get(destinationId);
    				
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
	
	
	public void writeZonesFile() {
		BufferedWriter bufferedWriterZones = null;
		
		try {
            File zonesFile = new File(outputFileZones);
    		FileWriter fileWriterZones = new FileWriter(zonesFile);
    		bufferedWriterZones = new BufferedWriter(fileWriterZones);
    		
    		for (int zoneId : zones) {
    			// 45 columns
    			bufferedWriterZones.write(zoneId + "\t" + 0 + "\t" + 0  + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0
    					+ "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0 + "\t" + 0);
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
	
	
	public void writeLOSOffPkAMFile() {
		BufferedWriter bufferedWriterLos = null;
		
		try {
            File losFile = new File(outputFileLosOffPkAM);
            FileWriter fileWriterLos = new FileWriter(losFile);
    		bufferedWriterLos = new BufferedWriter(fileWriterLos);
    		
    		double temp = 0.0;
    		
    		for (int originId : zones) {
    			for (int destinationId : zones) {
    				
    				int inSameZone = 0;
    				if (originId == destinationId) {
    					inSameZone = 1;
    				}
    				
    				double distance_mi = zone2ZoneDistanceMap.get(originId).get(destinationId);
    				int adjacent = zone2ZoneAdjacencyMap.get(originId).get(destinationId);
    				
    				double driveAloneIVTT_min = distance_mi * durantionDistanceOffPeakRatio_min_mile;
    				temp = Math.round(driveAloneIVTT_min * 100); // Round to two decimal places
    				driveAloneIVTT_min = temp / 100;
    				
    				double driveAloneCost_USD = distance_mi * costDistanceRatio_USD_mile;
    				temp = Math.round(driveAloneCost_USD * 100); // Round to two decimal places
    				driveAloneCost_USD = temp / 100;
    				
    				// 14 columns
    				bufferedWriterLos.write(originId + "\t" + destinationId + "\t" + inSameZone  + "\t" + adjacent
    						+ "\t" + distance_mi + "\t" + driveAloneIVTT_min + "\t" + 3.1
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
		System.out.println("LOSOffPkAM file written.");
	}
	
	
	public void writeLOSPeakAMFile() {
		BufferedWriter bufferedWriterLos = null;
		
		try {
            File losFile = new File(outputFileLosPeakAM);
            FileWriter fileWriterLos = new FileWriter(losFile);
    		bufferedWriterLos = new BufferedWriter(fileWriterLos);
    		
    		double temp = 0.0;
    		
    		for (int originId : zones) {
    			for (int destinationId : zones) {
    				
    				int inSameZone = 0;
    				if (originId == destinationId) {
    					inSameZone = 1;
    				}
    				
    				double distance_mi = zone2ZoneDistanceMap.get(originId).get(destinationId);
    				int adjacent = zone2ZoneAdjacencyMap.get(originId).get(destinationId);
    				
    				double driveAloneIVTT_min = distance_mi * durantionDistancePeakRatio_min_mile;
    				temp = Math.round(driveAloneIVTT_min * 100); // Round to two decimal places
    				driveAloneIVTT_min = temp / 100;
    				
    				double driveAloneCost_USD = distance_mi * costDistanceRatio_USD_mile;
    				temp = Math.round(driveAloneCost_USD * 100); // Round to two decimal places
    				driveAloneCost_USD = temp / 100;
    				
    				// 14 columns
    				bufferedWriterLos.write(originId + "\t" + destinationId + "\t" + inSameZone  + "\t" + adjacent
    						+ "\t" + distance_mi + "\t" + driveAloneIVTT_min + "\t" + 3.1
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
}