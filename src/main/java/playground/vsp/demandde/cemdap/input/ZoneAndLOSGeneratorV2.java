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

package playground.vsp.demandde.cemdap.input;

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
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.demandde.cemdap.LogToOutputSaver;

/**
 * @author dziemke
 */
public class ZoneAndLOSGeneratorV2 {
	private static final Logger LOG = Logger.getLogger(ZoneAndLOSGeneratorV2.class);

	// Storage objects
	private final Set<String> municipalities = new HashSet<>();
	private final List<String> zones = new LinkedList<>();
	private final Map<String, Geometry> zoneMap = new HashMap<>();
	private final Map<String, Map<String, Double>> zone2ZoneDistanceMap = new HashMap<>();
	private final Map<String, Map<String, Integer>> zone2ZoneAdjacencyMap = new HashMap<>();
	private final String outputBase;
	
	// Parameters
	private double defaultIntraZoneDistance = 1.72; // in miles; equals 2.76km.
	private double beelineDistanceFactor = 1.3;
	private double durantionDistanceOffPeakRatio_min_mile = 1.6; // based on computations in sample dataset; equals ca. 60km/h
	private double durantionDistancePeakRatio_min_mile = 1.9; // based on computations in sample dataset; equals ca. 50km/h
	private double costDistanceRatio_USD_mile = 0.072; // based on computations in sample dataset; equals 0.045USD/km

	// spatial refinement. Amit Nov'17
	private List<String> municipalityIDsForSpatialRefinement;
	private double defaultIntraZoneDistanceForSpatialRefinement;
	
	public static void main(String[] args) {
//		// Input and output
//		String commuterFileBase = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/pendlerstatistik_2009/";
//		String commuterFileOutgoing1 = commuterFileBase + "Berlin_2009/B2009Ga.txt";
//		String commuterFileOutgoing2 = commuterFileBase + "Brandenburg_2009/Teil1BR2009Ga.txt";
//		String commuterFileOutgoing3 = commuterFileBase + "Brandenburg_2009/Teil2BR2009Ga.txt";
//		String commuterFileOutgoing4 = commuterFileBase + "Brandenburg_2009/Teil3BR2009Ga.txt";
//		String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4};
//		String shapeFile = "../../shared-svn/studies/countries/de/berlin_scenario_2016/input/shapefiles/2013/gemeindenLOR_DHDN_GK4.shp";
//		String outputBase = "../../shared-svn/studies/countries/de/berlin_scenario_2016/cemdap_input/200/";
//
//		// Parameters
//		String featureKeyInShapeFile = "NR";
		
		// Input and output
		String commuterFileBase = "../../shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/pendlerstatistik/";
		String commuterFileOutgoing1 = commuterFileBase + "051NRW2009Ga.txt";
		String commuterFileOutgoing2 = commuterFileBase + "053NRW2009Ga.txt";
		String commuterFileOutgoing3 = commuterFileBase + "055NRW2009Ga.txt";
		String commuterFileOutgoing4 = commuterFileBase + "057NRW2009Ga.txt";
		String commuterFileOutgoing5 = commuterFileBase + "059NRW2009Ga.txt";
		String[] commuterFilesOutgoing = {commuterFileOutgoing1, commuterFileOutgoing2, commuterFileOutgoing3, commuterFileOutgoing4, commuterFileOutgoing5};
		String shapeFile = "../../shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/sourceShape_NRW/modified/dvg2gem_nw_mod.shp";
		String outputBase = "../../shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/100_neu/";

		// Parameters
		String featureKeyInShapeFile = "KN";
		
		ZoneAndLOSGeneratorV2 zoneAndLOSGeneratorV2 = new ZoneAndLOSGeneratorV2(commuterFilesOutgoing, shapeFile, outputBase, featureKeyInShapeFile);
		zoneAndLOSGeneratorV2.generateSupply();
	}

	
	public ZoneAndLOSGeneratorV2(String[] commuterFilesOutgoing, String shapeFile, String outputBase, String featureKeyInShapeFile) {
		LogToOutputSaver.setOutputDirectory(outputBase);

		this.outputBase = outputBase;

		readMunicipalities(commuterFilesOutgoing);
		readShape(shapeFile, featureKeyInShapeFile);
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
			CommuterFileReaderV2 commuterFileReader = new CommuterFileReaderV2(commuterFileOutgoing, "\t");
			Set<String> currentMunicipalities = commuterFileReader.getMunicipalities();
			this.municipalities.addAll(currentMunicipalities);
		}
	}

	
	private void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection <SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = (String) feature.getAttribute(featureKeyInShapeFile);
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
			Map<String,Double> toZoneDistanceMap = new HashMap<>();
			Map<String,Integer> toZoneAdjacencyMap = new HashMap<>();
			for (String destinationId : this.zones) {
				counter++;
				if (counter%1000 == 0) {
					LOG.info(counter + " relations computed.");
				}
				int adjacent;
				double distance_mi;
				double temp = 0.;
				
				if (originId.equals(destinationId)) { // internal traffic inside zone
					distance_mi = getIntraZonalDistance(originId) * beelineDistanceFactor;
					adjacent = 0;
				} else {
					Geometry originGeometry = this.zoneMap.get(originId);
					Coord originCoord = new Coord(originGeometry.getCentroid().getCoordinate().x, originGeometry.getCentroid().getCoordinate().y);
							
					Geometry destinationGeometry = this.zoneMap.get(destinationId);
					Coord destinationCoord = new Coord(destinationGeometry.getCentroid().getCoordinate().x, destinationGeometry.getCentroid().getCoordinate().y);
					
					double distanceX_m = Math.abs(originCoord.getX() - destinationCoord.getX());
					double distanceY_m = Math.abs(originCoord.getY() - destinationCoord.getY());
					double distance_m = Math.sqrt(distanceX_m * distanceX_m + distanceY_m * distanceY_m);
					
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
		LOG.info("Finised distance and adjacency computations.");
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
    			bufferedWriterZones.write(Integer.parseInt(zoneId) + "\t" + 0 + "\t" + 0  + "\t" + 0 + "\t" + 0
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
    				bufferedWriterLos.write(Integer.parseInt(originId) + "\t" + Integer.parseInt(destinationId) + 
    						"\t" + inSameZone  + "\t" + adjacent + "\t" + distance_mi + "\t" + driveAloneIVTT_min
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
    				bufferedWriterLos.write(Integer.parseInt(originId) + "\t" + destinationId + "\t" + inSameZone
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

	private double getIntraZonalDistance(String municipalityId){
		if (this.municipalityIDsForSpatialRefinement!=null && this.municipalityIDsForSpatialRefinement.contains(municipalityId)) {
			return this.defaultIntraZoneDistanceForSpatialRefinement;
		} else {
			return this.defaultIntraZoneDistance;
		}
	}

	/**
	 * @param defaultIntraZoneDistance will be used for all intra zonal trips. A zone could be Municipality of LOR (for berlin) or Bezirke (PLZ).
	 */
	public void setDefaultIntraZoneDistance(double defaultIntraZoneDistance) {
    	this.defaultIntraZoneDistance = defaultIntraZoneDistance;
    }

	/**
	 * @param defaultIntraZoneDistanceForSpatialRefinement default intra zonal distance for given list of Municipalities.
	 */
	public void setDefaultIntraZoneDistanceForSpatialRefinement(double defaultIntraZoneDistanceForSpatialRefinement) {
		this.defaultIntraZoneDistanceForSpatialRefinement = defaultIntraZoneDistanceForSpatialRefinement;
	}

	public void setMunicipalityIDsForSpatialRefinement(List<String> municipalityIDsForSpatialRefinement) {
		this.municipalityIDsForSpatialRefinement = municipalityIDsForSpatialRefinement;
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
