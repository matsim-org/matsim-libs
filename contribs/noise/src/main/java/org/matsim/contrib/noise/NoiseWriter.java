/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.noise;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.data.NoiseLink;
import org.matsim.contrib.noise.data.NoiseReceiverPoint;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * Contains all methods for writing noise-specific output.
 * 
 * @author ikaddoura
 *
 */
public class NoiseWriter {
	private static final Logger log = Logger.getLogger(NoiseWriter.class);

	public static void writeReceiverPoints(NoiseContext noiseContext, String outputPath, boolean useCompression) {

		// csv file
		HashMap<Id<ReceiverPoint>,Double> id2xCoord = new HashMap<>();
		HashMap<Id<ReceiverPoint>,Double> id2yCoord = new HashMap<>();
		int c = 0;
		for(Id<ReceiverPoint> id : noiseContext.getReceiverPoints().keySet()) {
			c++;
			if(c % 10000 == 0) {
				log.info("Writing out receiver point # "+ c);
			}
			id2xCoord.put(id, noiseContext.getReceiverPoints().get(id).getCoord().getX());
			id2yCoord.put(id, noiseContext.getReceiverPoints().get(id).getCoord().getY());
		}
		List<String> headers = new ArrayList<String>();
		headers.add("receiverPointId");
		headers.add("xCoord");
		headers.add("yCoord");
		
		List<HashMap<Id<ReceiverPoint>,Double>> values = new ArrayList<>();
		values.add(id2xCoord);
		values.add(id2yCoord);
		
		write(outputPath, 3, headers, values, useCompression);
		
		// shape file	
				
//		PointFeatureFactory factory = new PointFeatureFactory.Builder()
//		.setCrs(MGC.getCRS(noiseContext.getNoiseParams().getTransformationFactory()))
//		.setName("receiver point")
//		.addAttribute("Id", String.class)
//		.create();
//		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
//		
//		for (ReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
//					
//			SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(rp.getCoord()), new Object[] {rp.getId().toString()}, null);
//			features.add(feature);
//		}
//		
//		String filePath = outputPath;
//		File file = new File(filePath);
//		file.mkdirs();
//		
//		log.info("Writing receiver points to shapefile... ");
//		ShapeFileWriter.writeGeometries(features, filePath + "receiverPoints.shp");
//		log.info("Writing receiver points to shapefile... Done. ");
	}
	
	public static void write (String fileName , int columns , List<String> headers , List<HashMap<Id<ReceiverPoint>,Double>> values, boolean useCompression) {
		
		File file = new File(fileName);
		file.mkdirs();
		
		String file2 = fileName + "receiverPoints.csv" ;
		if ( useCompression ) {
			fileName += ".gz" ;
		}
			
		// For all maps, the number of keys should be the same
		try ( BufferedWriter bw = IOUtils.getBufferedWriter(file2) ) {
			bw.write(headers.get(0));
			for(int i = 1 ; i < columns ; i++) {
				bw.write(";"+headers.get(i));
			}
			bw.newLine();
			
			for(Id<ReceiverPoint> id : values.get(0).keySet()) {
				bw.write(id.toString());
				for(int i = 0 ; i < (columns-1) ; i++) {
					bw.write(";"+values.get(i).get(id));
				}
				bw.newLine();
			}
				
			bw.close();
				log.info("Receiver points written to " + fileName);
				
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeNoiseEmissionStatsPerHour(NoiseContext noiseContext, String outputPath, boolean useCompression) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathEmissions = outputPath + "emissions/";
		File dir = new File(outputPathEmissions);
		dir.mkdirs();
		
		String fileName = outputPathEmissions + "emission_" + timeInterval + ".csv";
		if ( useCompression ) {
			fileName += ".gz" ;
		}
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id;Demand (Car) " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) + ";Demand (HGV) " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) + ";vCar " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) + ";vHGV " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) + ";Noise Emission "  + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (Id<Link> linkId : noiseContext.getNoiseLinks().keySet()){
				
				int cars = 0;
				if (noiseContext.getNoiseLinks().containsKey(linkId)) {
					cars = noiseContext.getNoiseLinks().get(linkId).getCarAgentsEntering();
				}
				
				int hgv = 0;
				if (noiseContext.getNoiseLinks().containsKey(linkId)) {
					hgv = noiseContext.getNoiseLinks().get(linkId).getHgvAgentsEntering();
				}
				
				double vCar = noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed() * 3.6;
				if (noiseContext.getNoiseLinks().containsKey(linkId)) {
					double averageTravelTimeCar_sec = 0.;
					if (noiseContext.getNoiseLinks().get(linkId).getCarAgentsLeaving() > 0) {
						averageTravelTimeCar_sec = noiseContext.getNoiseLinks().get(linkId).getTravelTimeCar_sec() / noiseContext.getNoiseLinks().get(linkId).getCarAgentsLeaving();	
					}
					if (averageTravelTimeCar_sec > 0.) {
						vCar = 3.6 * (noiseContext.getScenario().getNetwork().getLinks().get(linkId).getLength() / averageTravelTimeCar_sec );
					}
				}
				
				double vHdv = vCar;
				if (noiseContext.getNoiseLinks().containsKey(linkId)) {
					double averageTravelTimeHGV_sec = 0.;
					if (noiseContext.getNoiseLinks().get(linkId).getCarAgentsLeaving() > 0) {
						averageTravelTimeHGV_sec = noiseContext.getNoiseLinks().get(linkId).getTravelTimeHGV_sec() / noiseContext.getNoiseLinks().get(linkId).getHgvAgentsLeaving();
					}
					if (averageTravelTimeHGV_sec > 0.) {
						vHdv = 3.6 * (noiseContext.getScenario().getNetwork().getLinks().get(linkId).getLength() / averageTravelTimeHGV_sec );
					}
				}
				
				bw.write(linkId.toString() + ";" + (cars * noiseContext.getNoiseParams().getScaleFactor()) + ";" + (hgv * noiseContext.getNoiseParams().getScaleFactor()) + ";" + vCar + ";" + vHdv + ";" + noiseContext.getNoiseLinks().get(linkId).getEmission());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\",\"Real\",\"Real\"\"Real\"\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	public static void writeNoiseImmissionStatsPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathImmissions = outputPath + "immissions/";
		File dir = new File(outputPathImmissions);
		dir.mkdirs();
		
		String fileName = outputPathImmissions + "immission_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Receiver Point Id;Immission " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) + ";x;y;t");
			bw.newLine();
			
			for (NoiseReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
				
				bw.write(rp.getId() + ";" + rp.getFinalImmission() + ";" + rp.getCoord().getX() + ";" + rp.getCoord().getY() + ";" + timeInterval );
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}
	
	public static void writePersonActivityInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathActivityInfo = outputPath + "consideredAgentUnits/";
		File dir = new File(outputPathActivityInfo);
		dir.mkdirs();
		
		String fileName = outputPathActivityInfo + "consideredAgentUnits_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Receiver Point Id;Considered Agent Units " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
				
				bw.write(rp.getId() + ";" + rp.getAffectedAgentUnits());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void writeDamageInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
	
		String outputPathDamages = outputPath + "damages_receiverPoint/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();
		
		String fileName = outputPathDamages + "damages_receiverPoint_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Receiver Point Id;Damages " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
				
				bw.write(rp.getId() + ";" + rp.getDamageCosts());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public static void writeLinkDamageInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathDamages = outputPath + "average_damages_link/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();
		
		String fileName = outputPathDamages + "average_damages_link_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id;Damages " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {
				
				bw.write(link.getId() + ";" + link.getDamageCost());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public static void writeLinkAvgCarDamageInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathDamages = outputPath + "average_damages_link_car/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();
		
		String fileName = outputPathDamages + "average_damages_link_car_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id;Average damages per car " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {
				
				bw.write(link.getId() + ";" + link.getAverageDamageCostPerCar());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void writeLinkAvgHgvDamageInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathDamages = outputPath + "average_damages_link_hgv/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();
		
		String fileName = outputPathDamages + "average_damages_link_hgv_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id; Average damages per HGV " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {
				
				bw.write(link.getId() + ";" + link.getAverageDamageCostPerHgv());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public static void writeLinkMarginalCarDamageInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathDamages = outputPath + "marginal_damages_link_car/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();
		
		String fileName = outputPathDamages + "marginal_damages_link_car_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id;Marginal damages per car " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {
				
				bw.write(link.getId() + ";" + link.getMarginalDamageCostPerCar());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	public static void writeLinkMarginalHgvDamageInfoPerHour(NoiseContext noiseContext, String outputPath) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();
		
		String outputPathDamages = outputPath + "marginal_damages_link_hgv/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();
		
		String fileName = outputPathDamages + "marginal_damages_link_hgv_" + timeInterval + ".csv";
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			bw.write("Link Id; Marginal damages per HGV " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();
			
			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {
				
				bw.write(link.getId() + ";" + link.getMarginalDamageCostPerHgv());
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\",\"Real\"");
						
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
