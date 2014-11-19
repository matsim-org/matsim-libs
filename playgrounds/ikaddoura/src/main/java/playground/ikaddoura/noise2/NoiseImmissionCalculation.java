/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;

/**
 * 
 * Collects the relevant information in order to compute the noise immission for each receiver point.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseImmissionCalculation {

	private static final Logger log = Logger.getLogger(NoiseImmissionCalculation.class);
	
	private NoiseInitialization spatialInfo;
	private NoiseParameters noiseParams;

	private NoiseEquations noiseImmissionCalculator;
		
	// from emission handler
	private Map<Id <Link>, Map<Double,Double>> linkId2timeInterval2noiseEmission;
	private Map<Id <Link>, Map<Double,List<Id<Vehicle>>>> linkId2timeInterval2linkEnterVehicleIDs;
	
	// optional information for a more detailed calculation of noise immission
	private final List<Id<Link>> tunnelLinks = new ArrayList<Id<Link>>();
	private final List<Id<Link>> noiseBarrierLinks = new ArrayList<Id<Link>>();
	
	// noise immissions
	private Map<Id<ReceiverPoint>,Map<Double,Double>> receiverPointId2timeInterval2noiseImmission = new HashMap<Id<ReceiverPoint>, Map<Double,Double>>();
	private Map<Id<ReceiverPoint>,Map<Double,Map<Id<Link>,Double>>> receiverPointIds2timeIntervals2noiseLinks2isolatedImmission = new HashMap<Id<ReceiverPoint>, Map<Double,Map<Id<Link>,Double>>>();
		
	public NoiseImmissionCalculation (NoiseInitialization spatialInfo, NoiseEmissionHandler noiseEmissionHandler, NoiseParameters noiseParams) {
		this.spatialInfo = spatialInfo;
		this.noiseParams = noiseParams;
		this.noiseImmissionCalculator = new NoiseEquations();
		
		this.linkId2timeInterval2noiseEmission = noiseEmissionHandler.getLinkId2timeInterval2noiseEmission();
		this.linkId2timeInterval2linkEnterVehicleIDs = noiseEmissionHandler.getLinkId2timeInterval2linkEnterVehicleIDs();
	}
	
	public void setTunnelLinks(ArrayList<Id<Link>> tunnelLinks) {
		if (tunnelLinks == null) {
			log.warn("No information on tunnels provided.");
		} else {
			this.tunnelLinks.addAll(tunnelLinks);
			log.warn("Consideration of tunnels not yet implemented.");
		}
	}
	
	public void setNoiseBarrierLinks(ArrayList<Id<Link>> noiseBarrierLinks) {
		if (noiseBarrierLinks == null) {
			log.warn("No information on noise barriers provided.");
		} else {
			this.noiseBarrierLinks.addAll(noiseBarrierLinks);
			log.warn("Consideration of noise barriers not yet implemented.");
		}
	}
	
	public void calculateNoiseImmission() {
		
		calculateImmissionSharesPerReceiverPointPerTimeInterval();
		calculateFinalNoiseImmissions();
	}

	private void calculateImmissionSharesPerReceiverPointPerTimeInterval() {
		
		for (Id<ReceiverPoint> coordId : spatialInfo.getReceiverPoints().keySet()) {
			Map<Double,Map<Id<Link>,Double>> timeIntervals2noiseLinks2isolatedImmission = new HashMap<Double, Map<Id<Link>,Double>>();
		
			for (double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval <= 30 * 3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()) {
			 	Map<Id<Link>,Double> noiseLinks2isolatedImmission = new HashMap<Id<Link>, Double>();
			
			 	for(Id<Link> linkId : spatialInfo.getReceiverPoints().get(coordId).getLinkId2distanceCorrection().keySet()) {
					double noiseEmission = linkId2timeInterval2noiseEmission.get(linkId).get(timeInterval);
					double noiseImmission = 0.;
					if (!(noiseEmission == 0.)) {
						noiseImmission = emission2immission(this.spatialInfo , linkId , noiseEmission , coordId);						
					}
					noiseLinks2isolatedImmission.put(linkId,noiseImmission);
				}
				timeIntervals2noiseLinks2isolatedImmission.put(timeInterval, noiseLinks2isolatedImmission);
			}
			receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.put(coordId, timeIntervals2noiseLinks2isolatedImmission);
		}
	}
	
	
	private double emission2immission(NoiseInitialization spatialInfo, Id<Link> linkId, double noiseEmission, Id<ReceiverPoint> rpId) {
		double noiseImmission = 0.;
			
		noiseImmission = noiseEmission
				+ spatialInfo.getReceiverPoints().get(rpId).getLinkId2distanceCorrection().get(linkId)
				+ spatialInfo.getReceiverPoints().get(rpId).getLinkId2angleCorrection().get(linkId);
		
		if (noiseImmission < 0.) {
			noiseImmission = 0.;
		}
		return noiseImmission;
	}

	private void calculateFinalNoiseImmissions() {
		for(Id<ReceiverPoint> rpId : spatialInfo.getReceiverPoints().keySet()) {
			Map<Double,Double> timeInterval2noiseImmission = new HashMap<Double, Double>();
			for(double timeInterval = noiseParams.getTimeBinSizeNoiseComputation() ; timeInterval<=30*3600 ; timeInterval = timeInterval + noiseParams.getTimeBinSizeNoiseComputation()) {
				List<Double> noiseImmissions = new ArrayList<Double>();
				if(!(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(rpId).get(timeInterval)==null)) {
					for(Id<Link> linkId : receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(rpId).get(timeInterval).keySet()) {
						if(!(linkId2timeInterval2linkEnterVehicleIDs.get(linkId).get(timeInterval).size() == 0.)) {
							noiseImmissions.add(receiverPointIds2timeIntervals2noiseLinks2isolatedImmission.get(rpId).get(timeInterval).get(linkId));
						}
					}	
					double resultingNoiseImmission = noiseImmissionCalculator.calculateResultingNoiseImmission(noiseImmissions);
					timeInterval2noiseImmission.put(timeInterval, resultingNoiseImmission);
				} else {
					// if no link has to to be considered for the calculation due to too long distances
					timeInterval2noiseImmission.put(timeInterval, 0.);
				}
			}
			receiverPointId2timeInterval2noiseImmission.put(rpId, timeInterval2noiseImmission);
		}
	}
	
	// write immission infos
	
	public void writeNoiseImmissionStats(String fileName) {
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
			
			for (Id<ReceiverPoint> rpId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
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
				
				for(double timeInterval : receiverPointId2timeInterval2noiseImmission.get(rpId).keySet()) {
					double noiseValue = receiverPointId2timeInterval2noiseImmission.get(rpId).get(timeInterval);
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
								
				bw.write(rpId + ";" + avgNoise + ";" + avgNoiseDay+";"+avgNoiseNight+";"+avgNoisePeak+";"+avgNoiseOffPeak);
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
	
	public void writeNoiseImmissionStatsPerHour(String fileName) {
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

			
			for (Id<ReceiverPoint> rpId : this.receiverPointId2timeInterval2noiseImmission.keySet()){
				bw.write(rpId.toString());
				for(int i = 0 ; i < 30 ; i++) {
					double timeInterval = (i+1) * noiseParams.getTimeBinSizeNoiseComputation();
					double noiseImmission = 0.;
					
					
					if (receiverPointId2timeInterval2noiseImmission.get(rpId).get(timeInterval) != null) {
						noiseImmission = receiverPointId2timeInterval2noiseImmission.get(rpId).get(timeInterval);
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
	
	public void writeNoiseImmissionStatsPerHourShapeFile(String fileName){
		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(MGC.getCRS(TransformationFactory.WGS84))
		.setName("receiver point")
		.addAttribute("Time", String.class)
		.addAttribute("immissions", Double.class)
		.create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for (Id<ReceiverPoint> receiverPoint : this.receiverPointId2timeInterval2noiseImmission.keySet()) {
			for (Double timeInterval : this.receiverPointId2timeInterval2noiseImmission.get(receiverPoint).keySet()) {
				if (timeInterval <= 23 * 3600.) {
					String dateTimeString = convertSeconds2dateTimeFormat(timeInterval);
					double result = this.receiverPointId2timeInterval2noiseImmission.get(receiverPoint).get(timeInterval);
					
					SimpleFeature feature = factory.createPoint(MGC.coord2Coordinate(this.spatialInfo.getReceiverPoints().get(receiverPoint).getCoord()), new Object[] {dateTimeString, result}, null);
					features.add(feature);
				}				
			}
		}
		
		ShapeFileWriter.writeGeometries(features, fileName);
		log.info("Shape file written to " + fileName);
	}
	
	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2000-01-01 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMMSS);
		String dateTimeString = date + time;
		return dateTimeString;
	}
	
	public Map<Id<ReceiverPoint>, Map<Double, Map<Id<Link>, Double>>> getReceiverPointIds2timeIntervals2noiseLinks2isolatedImmission() {
		return receiverPointIds2timeIntervals2noiseLinks2isolatedImmission;
	}

	public Map<Id<ReceiverPoint>, Map<Double, Double>> getReceiverPointId2timeInterval2noiseImmission() {
		return receiverPointId2timeInterval2noiseImmission;
	}	
}
