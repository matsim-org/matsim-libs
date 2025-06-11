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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 * Contains all methods for writing noise-specific output.
 *
 * @author ikaddoura
 *
 */
final class NoiseWriter {
	private static final Logger log = LogManager.getLogger(NoiseWriter.class);

	public static void writeReceiverPoints(NoiseContext noiseContext, String outputPath, boolean useCompression) {

		// csv file
		Map<Id<ReceiverPoint>,Double> id2xCoord = new HashMap<>();
		Map<Id<ReceiverPoint>,Double> id2yCoord = new HashMap<>();
		int c = 0;
		for(Id<ReceiverPoint> id : noiseContext.getReceiverPoints().keySet()) {
			c++;
			if(c % 10000 == 0) {
				log.info("Writing out receiver point # "+ c);
			}
			id2xCoord.put(id, noiseContext.getReceiverPoints().get(id).getCoord().getX());
			id2yCoord.put(id, noiseContext.getReceiverPoints().get(id).getCoord().getY());
		}
		List<String> headers = new ArrayList<>();
		headers.add("receiverPointId");
		headers.add("xCoord");
		headers.add("yCoord");

		List<Map<Id<ReceiverPoint>,Double>> values = new ArrayList<>();
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

	public static void write (String fileName , int columns , List<String> headers , List<Map<Id<ReceiverPoint>,Double>> values, boolean useCompression) {

		File file = new File(fileName);
		file.mkdirs();

		String file2 = fileName + "receiverPoints.csv" ;
		if ( useCompression ) {
			file2 += ".gz" ;
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

	static void writeNoiseEmissionStatsPerHour(NoiseContext noiseContext, String outputPath, boolean useCompression,
											   Set<NoiseVehicleType> vehicleTypes) {
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

			StringJoiner joiner = new StringJoiner(";");
			joiner.add("Link Id");
			for(NoiseVehicleType vehicleType: vehicleTypes) {
				joiner.add("Demand (" + vehicleType.getId().toString().toUpperCase() + ") "+ Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			}
			for(NoiseVehicleType vehicleType: vehicleTypes) {
				joiner.add("v" + vehicleType.getId().toString().toUpperCase() + " "+ Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			}
			joiner.add("Noise Emission " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));

			bw.write(joiner.toString());
			bw.newLine();

			for (Id<Link> linkId : noiseContext.getNoiseLinks().keySet()){

				joiner = new StringJoiner(";");
				joiner.add(linkId.toString());
				for(NoiseVehicleType vehicleType: vehicleTypes) {
					int vehicles = 0;
					if (noiseContext.getNoiseLinks().containsKey(linkId)) {
						vehicles = noiseContext.getNoiseLinks().get(linkId).getAgentsEntering(vehicleType);
					}
					joiner.add(String.valueOf(vehicles * noiseContext.getNoiseParams().getScaleFactor()));
				}
				for(NoiseVehicleType vehicleType: vehicleTypes) {
					double v = noiseContext.getScenario().getNetwork().getLinks().get(linkId).getFreespeed() * 3.6;
					if (noiseContext.getNoiseLinks().containsKey(linkId)) {
						double averageTravelTime_sec = 0.;
						if (noiseContext.getNoiseLinks().get(linkId).getAgentsLeaving(vehicleType) > 0) {
							averageTravelTime_sec = noiseContext.getNoiseLinks().get(linkId).getTravelTime_sec(vehicleType) / noiseContext.getNoiseLinks().get(linkId).getAgentsLeaving(vehicleType);
						}
						if (averageTravelTime_sec > 0.) {
							v = 3.6 * (noiseContext.getScenario().getNetwork().getLinks().get(linkId).getLength() / averageTravelTime_sec );
						}
					}
					joiner.add(String.valueOf(v));
				}
				joiner.add(String.valueOf(noiseContext.getNoiseLinks().get(linkId).getEmission()));
				bw.write(joiner.toString());
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

	static void writeNoiseImmissionStatsPerHour(NoiseContext noiseContext, String outputPath) {
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

				bw.write(rp.getId() + ";" + rp.getCurrentImmission() + ";" + rp.getCoord().getX() + ";" + rp.getCoord().getY() + ";" + timeInterval );
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

	static void writePersonActivityInfoPerHour( NoiseContext noiseContext , String outputPath ) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();

		String outputPathActivityInfo = outputPath + "consideredAgentUnits/";
		File dir = new File(outputPathActivityInfo);
		dir.mkdirs();

		String fileName = outputPathActivityInfo + "consideredAgentUnits_" + timeInterval + ".csv";

		log.warn("writing consideredAgentUnits for timeInterval=" + timeInterval ) ;

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
			bw.write("Receiver Point Id;Damages " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS) + ";x;y;t");
			bw.newLine();

			for (NoiseReceiverPoint rp : noiseContext.getReceiverPoints().values()) {
				bw.write(rp.getId() + ";" + rp.getDamageCosts() + ";" + rp.getCoord().getX() + ";" + rp.getCoord().getY() + ";" + timeInterval );
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

	static void writeLinkAvgDamagePerVehicleTypeInfoPerHour(NoiseContext noiseContext, String outputPath,
															NoiseVehicleType vehicleType) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();

		String outputPathDamages = outputPath + "average_damages_link_"+vehicleType.getId()+"/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();

		String fileName = outputPathDamages + "average_damages_link_"+vehicleType.getId()+"_" + timeInterval + ".csv";

		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("Link Id;Average damages per "+vehicleType.getId()+" " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();

			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {

				bw.write(link.getId() + ";" + link.getAverageDamageCostPerVehicle(vehicleType));
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

	public static void writeLinkMarginalVehicleDamageInfoPerHour(NoiseContext noiseContext, String outputPath,
																 NoiseVehicleType vehicleType) {
		double timeInterval = noiseContext.getCurrentTimeBinEndTime();

		String outputPathDamages = outputPath + "marginal_damages_link_"+vehicleType.getId()+"/";
		File dir = new File(outputPathDamages);
		dir.mkdirs();

		String fileName = outputPathDamages + "marginal_damages_link_"+vehicleType.getId()+"_" + timeInterval + ".csv";

		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("Link Id;Marginal damages per "+vehicleType.getId()+" " + Time.writeTime(timeInterval, Time.TIMEFORMAT_HHMMSS));
			bw.newLine();

			for (NoiseLink link : noiseContext.getNoiseLinks().values()) {

				bw.write(link.getId() + ";" + link.getMarginalDamageCostPerVehicle(vehicleType));
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
