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
package playground.ikaddoura.noise2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

/**
 * 
 * Contains all methods for writing noise-specific output. TODO: Condense the code.
 * 
 * @author ikaddoura
 *
 */
public class NoiseWriter {
	private static final Logger log = Logger.getLogger(NoiseWriter.class);

	public static void writeReceiverPoints(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints, NoiseParameters noiseParameters, String outputPath) {

		// csv file
		HashMap<Id<ReceiverPoint>,Double> id2xCoord = new HashMap<>();
		HashMap<Id<ReceiverPoint>,Double> id2yCoord = new HashMap<>();
		int c = 0;
		for(Id<ReceiverPoint> id : receiverPoints.keySet()) {
			c++;
			if(c % 1000 == 0) {
				log.info("Writing out receiver point # "+ c);
			}
			id2xCoord.put(id, receiverPoints.get(id).getCoord().getX());
			id2yCoord.put(id, receiverPoints.get(id).getCoord().getY());
		}
		List<String> headers = new ArrayList<String>();
		headers.add("receiverPointId");
		headers.add("xCoord");
		headers.add("yCoord");
		
		List<HashMap<Id<ReceiverPoint>,Double>> values = new ArrayList<>();
		values.add(id2xCoord);
		values.add(id2yCoord);
		
		write(outputPath, 3, headers, values);
		
		// shape file	
				
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(MGC.getCRS(noiseParameters.getTransformationFactory()))
		.setName("receiver point")
		.addAttribute("Id", String.class)
		.create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (ReceiverPoint rp : receiverPoints.values()) {
					
			SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(rp.getCoord()), new Object[] {rp.getId().toString()}, null);
			features.add(feature);
		}
		
		String filePath = outputPath;
		File file = new File(filePath);
		file.mkdirs();
		
		log.info("Writing receiver points to shapefile... ");
		ShapeFileWriter.writeGeometries(features, filePath + "receiverPoints.shp");
		log.info("Writing receiver points to shapefile... Done. ");
	}
	
	private static void write (String fileName , int columns , List<String> headers , List<HashMap<Id<ReceiverPoint>,Double>> values) {
		
		File file = new File(fileName);
		file.mkdirs();
		
		File file2 = new File(fileName + "receiverPoints.csv");
			
		// For all maps, the number of keys should be the same
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
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


	public static void writeNoiseEmissionsStats(NoiseEmissionHandler noiseEmissionHandler, NoiseParameters noiseParams, String fileName) {
		
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;avg noiseEmission;avg noiseEmission (day);avg noiseEmission (night);avg noiseEmission (peak);avg noiseEmission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6 * 3600 + noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 22 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7 * 3600 + noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 9 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15 * 3600 + noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 18 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (Id<Link> linkId : noiseEmissionHandler.getLinkId2timeInterval2noiseEmission().keySet()){
				double avgNoise = 0.;
				double avgNoiseDay = 0.;
				double avgNoiseNight = 0.;
				double avgNoisePeak = 0.;
				double avgNoiseOffPeak = 0.;
				
				double sumAvgNoise = 0.;
				int counterAvgNoise = 0;
				double sumAvgNoiseDay = 0.;
				int counterAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0.;
				int counterAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0.;
				int counterAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0.;
				int counterAvgNoiseOffPeak = 0;
				
				for(double timeInterval : noiseEmissionHandler.getLinkId2timeInterval2noiseEmission().get(linkId).keySet()) {
					double noiseValue = noiseEmissionHandler.getLinkId2timeInterval2noiseEmission().get(linkId).get(timeInterval);
					double termToAdd = Math.pow(10., noiseValue/10.);
					
					if(timeInterval < 30 * 3600) {
						sumAvgNoise = sumAvgNoise + termToAdd;
						counterAvgNoise++;
					}
					
					if(day.contains(timeInterval)) {
						sumAvgNoiseDay = sumAvgNoiseDay + termToAdd;
						counterAvgNoiseDay++;
					}
					
					if(night.contains(timeInterval)) {
						sumAvgNoiseNight = sumAvgNoiseNight + termToAdd;
						counterAvgNoiseNight++;
					}
				
					if(peak.contains(timeInterval)) {
						sumAvgNoisePeak = sumAvgNoisePeak + termToAdd;
						counterAvgNoisePeak++;
					}
					
					if(offPeak.contains(timeInterval)) {
						sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + termToAdd;
						counterAvgNoiseOffPeak++;
					}	
				}
				
				avgNoise = 10 * Math.log10(sumAvgNoise / (counterAvgNoise));
				avgNoiseDay = 10 * Math.log10(sumAvgNoiseDay / counterAvgNoiseDay);
				avgNoiseNight = 10 * Math.log10(sumAvgNoiseNight / counterAvgNoiseNight);
				avgNoisePeak = 10 * Math.log10(sumAvgNoisePeak / counterAvgNoisePeak);
				avgNoiseOffPeak = 10 * Math.log10(sumAvgNoiseOffPeak / counterAvgNoiseOffPeak);
								
				bw.write(linkId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
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
			bw.write("\"String\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\"");
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}


	public static void writeNoiseEmissionStatsPerHour(NoiseEmissionHandler noiseEmissionHandler, NoiseParameters noiseParams, String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("link");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * noiseParams.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";demand " + time + ";noise emission " + time);
			}
			bw.newLine();
			
			for (Id<Link> linkId : noiseEmissionHandler.getLinkId2timeInterval2noiseEmission().keySet()){
				bw.write(linkId.toString()); 
				for(int i = 0 ; i < 30 ; i++) {
					bw.write(";"+ ((noiseEmissionHandler.getLinkId2timeInterval2linkEnterVehicleIDs().get(linkId).get((i+1) * noiseParams.getTimeBinSizeNoiseComputation()).size()) * noiseParams.getScaleFactor()) + ";" + noiseEmissionHandler.getLinkId2timeInterval2noiseEmission().get(linkId).get((i+1) * noiseParams.getTimeBinSizeNoiseComputation()));	
				}
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
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	public static void writeNoiseImmissionStats(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints, NoiseParameters noiseParams, String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("receiver point Id;avg noise immission;avg noise immission (day);avg noise immission (night);avg noise immission (peak);avg noise immission (off-peak)");
			bw.newLine();
			
			List<Double> day = new ArrayList<Double>();
			for(double timeInterval = 6 * 3600 + noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 22 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				day.add(timeInterval);
			}
			List<Double> night = new ArrayList<Double>();
			for(double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				if(!(day.contains(timeInterval))) {
					night.add(timeInterval);
				}
			}
			
			List<Double> peak = new ArrayList<Double>();
			for(double timeInterval = 7 * 3600 + noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 9 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			for(double timeInterval = 15 * 3600 + noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 18 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				peak.add(timeInterval);
			}
			List<Double> offPeak = new ArrayList<Double>();
			for(double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 24 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()){
				if(!(peak.contains(timeInterval))) {
					offPeak.add(timeInterval);
				}
			}
			
			for (ReceiverPoint rp : receiverPoints.values()){
				double avgNoise = 0.;
				double avgNoiseDay = 0.;
				double avgNoiseNight = 0.;
				double avgNoisePeak = 0.;
				double avgNoiseOffPeak = 0.;
				
				double sumAvgNoise = 0.;
				int counterAvgNoise = 0;
				double sumAvgNoiseDay = 0.;
				int counterAvgNoiseDay = 0;
				double sumAvgNoiseNight = 0.;
				int counterAvgNoiseNight = 0;
				double sumAvgNoisePeak = 0.;
				int counterAvgNoisePeak = 0;
				double sumAvgNoiseOffPeak = 0.;
				int counterAvgNoiseOffPeak = 0;
				
				for(double timeInterval : rp.getTimeInterval2immission().keySet()) {
					double noiseValue = rp.getTimeInterval2immission().get(timeInterval);
					double termToAdd = Math.pow(10., noiseValue/10.);
					
					if(timeInterval < 30 * 3600) {
						sumAvgNoise = sumAvgNoise + termToAdd;
						counterAvgNoise++;
					}
					
					if(day.contains(timeInterval)) {
						sumAvgNoiseDay = sumAvgNoiseDay + termToAdd;
						counterAvgNoiseDay++;
					}
					
					if(night.contains(timeInterval)) {
						sumAvgNoiseNight = sumAvgNoiseNight + termToAdd;
						counterAvgNoiseNight++;
					}
				
					if(peak.contains(timeInterval)) {
						sumAvgNoisePeak = sumAvgNoisePeak + termToAdd;
						counterAvgNoisePeak++;
					}
					
					if(offPeak.contains(timeInterval)) {
						sumAvgNoiseOffPeak = sumAvgNoiseOffPeak + termToAdd;
						counterAvgNoiseOffPeak++;
					}	
				}
				
				avgNoise = 10 * Math.log10(sumAvgNoise / (counterAvgNoise));
				avgNoiseDay = 10 * Math.log10(sumAvgNoiseDay / counterAvgNoiseDay);
				avgNoiseNight = 10 * Math.log10(sumAvgNoiseNight / counterAvgNoiseNight);
				avgNoisePeak = 10 * Math.log10(sumAvgNoisePeak / counterAvgNoisePeak);
				avgNoiseOffPeak = 10 * Math.log10(sumAvgNoiseOffPeak / counterAvgNoiseOffPeak);
								
				bw.write(rp.getId() + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
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
			bw.write("\"String\",\"Real\",\"Real\",\"Real\",\"Real\",\"Real\"");
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void writeNoiseImmissionStatsPerHour(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints, NoiseParameters noiseParams, String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("receiver point");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * noiseParams.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";noise immission " + time);
			}
			bw.newLine();

			
			for (ReceiverPoint rp : receiverPoints.values()){
				bw.write(rp.getId().toString());
				for(int i = 0 ; i < 30 ; i++) {
					double timeInterval = (i+1) * noiseParams.getTimeBinSizeNoiseComputation();
					double noiseImmission = 0.;
					
					
					if (rp.getTimeInterval2immission().get(timeInterval) != null) {
						noiseImmission = rp.getTimeInterval2immission().get(timeInterval);
					}
					
					bw.write(";"+ noiseImmission);	
				}
				
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
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
		
	}
	
	public static void writeNoiseImmissionStatsPerHourShapeFile(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints, NoiseParameters noiseParameters, String fileName){
		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(MGC.getCRS(noiseParameters.getTransformationFactory()))
		.setName("receiver point")
		.addAttribute("Time", String.class)
		.addAttribute("immissions", Double.class)
		.create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (ReceiverPoint rp : receiverPoints.values()) {
			for (Double timeInterval : rp.getTimeInterval2immission().keySet()) {
				if (timeInterval <= 23 * 3600.) {
					String dateTimeString = convertSeconds2dateTimeFormat(timeInterval);
					double result = rp.getTimeInterval2immission().get(timeInterval);
					
					SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(rp.getCoord()), new Object[] {dateTimeString, result}, null);
					features.add(feature);
				}				
			}
		}
		
		ShapeFileWriter.writeGeometries(features, fileName);
		log.info("Shape file written to " + fileName);
	}
	
	private static String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2000-01-01 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMMSS);
		String dateTimeString = date + time;
		return dateTimeString;
	}	
	
	public static void writePersonActivityInfoPerHour(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints, NoiseParameters noiseParams, String fileName) {
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("receiver point");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * noiseParams.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";agent units " + time);
			}
			bw.newLine();

			
			for (ReceiverPoint rp : receiverPoints.values()) {
				bw.write(rp.getId().toString());
				for(int i = 0 ; i < 30 ; i++) {
					double timeInterval = (i+1) * noiseParams.getTimeBinSizeNoiseComputation();
					double affectedAgentUnits = 0.;
					
					if (rp.getTimeInterval2affectedAgentUnits() != null) {
						if (rp.getTimeInterval2affectedAgentUnits().get(timeInterval) != null) {
							affectedAgentUnits = rp.getTimeInterval2affectedAgentUnits().get(timeInterval);
						}
					}					
					bw.write(";"+ affectedAgentUnits);	
				}
				
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
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static void writeDamageInfoPerHour(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints, NoiseParameters noiseParams, String fileName) {
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("receiver point");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * noiseParams.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";damage costs (EUR) " + time);
			}
			bw.newLine();

			
			for (ReceiverPoint rp : receiverPoints.values()) {
				bw.write(rp.getId().toString());
				for(int i = 0 ; i < 30 ; i++) {
					double timeInterval = (i+1) * noiseParams.getTimeBinSizeNoiseComputation();
					double damage = 0.;
					
					if (rp.getTimeInterval2damageCosts() != null) {
						if (rp.getTimeInterval2damageCosts().get(timeInterval) != null) {
							damage = rp.getTimeInterval2damageCosts().get(timeInterval);
						}
					}					
					bw.write(";"+ damage);	
				}
				
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
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}
