/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.smallScaleCommercialTrafficGeneration;

import com.google.common.base.Joiner;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetGenerationRates;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ricardo Ewert
 *
 */
public class TrafficVolumeGeneration {

	private static final Logger log = LogManager.getLogger(TrafficVolumeGeneration.class);
	private static final Joiner JOIN = Joiner.on("\t");

	private static Map<Integer, Map<String, Double>> generationRatesStart = new HashMap<>();
	private static Map<Integer, Map<String, Double>> generationRatesStop = new HashMap<>();
	private static Map<String, Map<String, Double>> commitmentRatesStart = new HashMap<>();
	private static Map<String, Map<String, Double>> commitmentRatesStop = new HashMap<>();

	public static class TrafficVolumeKey {
		private final String zone;
		private final String modeORvehType;

		public TrafficVolumeKey(String zone, String modeORvehType) {
			super();
			this.zone = zone;
			this.modeORvehType = modeORvehType;
		}

		public String getZone() {
			return zone;
		}

		public String getModeORvehType() {
			return modeORvehType;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((zone == null) ? 0 : zone.hashCode());
			result = prime * result + ((modeORvehType == null) ? 0 : modeORvehType.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TrafficVolumeKey other = (TrafficVolumeKey) obj;
			if (zone == null) {
				if (other.zone != null)
					return false;
			} else if (!zone.equals(other.zone))
				return false;
			if (modeORvehType == null) {
				return other.modeORvehType == null;
			} else return modeORvehType.equals(other.modeORvehType);
		}
	}

	static TrafficVolumeKey makeTrafficVolumeKey(String zone, String modeORvehType) {
		return new TrafficVolumeKey(zone, modeORvehType);
	}

	/**
	 * Creates the traffic volume (start) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 *
	 * @param resultingDataPerZone employee date for each zone
	 * @param output output path
	 * @param sample sample size
	 * @param modesORvehTypes selected mode or vehicleType
	 * @return trafficVolume_start
	 */
	static Map<TrafficVolumeKey, Object2DoubleMap<Integer>> createTrafficVolume_start(
			Map<String, Object2DoubleMap<String>> resultingDataPerZone, Path output, double sample,
			List<String> modesORvehTypes, String trafficType) throws MalformedURLException {

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_start = new HashMap<>();
		calculateTrafficVolumePerZone(trafficVolume_start, resultingDataPerZone, "start", sample, modesORvehTypes);
		String sampleName = SmallScaleCommercialTrafficUtils.getSampleNameOfOutputFolder(sample);
		Path outputFileStart = output.resolve("calculatedData")
				.resolve("TrafficVolume_" + trafficType + "_" + "startPerZone_" + sampleName + "pt.csv");
		writeCSVTrafficVolume(trafficVolume_start, outputFileStart);
		log.info("Write traffic volume for start trips per zone in CSV: {}", outputFileStart);
		return trafficVolume_start;
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 *
	 * @param resultingDataPerZone employee date for each zone
	 * @param output output path
	 * @param sample sample size
	 * @param modesORvehTypes selected mode or vehicleType
	 * @return trafficVolume_stop
	 */
	static Map<TrafficVolumeKey, Object2DoubleMap<Integer>> createTrafficVolume_stop(
			Map<String, Object2DoubleMap<String>> resultingDataPerZone, Path output, double sample,
			List<String> modesORvehTypes, String trafficType) throws MalformedURLException {

		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop = new HashMap<>();
		calculateTrafficVolumePerZone(trafficVolume_stop, resultingDataPerZone, "stop", sample, modesORvehTypes);
		String sampleName = SmallScaleCommercialTrafficUtils.getSampleNameOfOutputFolder(sample);
		Path outputFileStop = output.resolve("calculatedData")
				.resolve("TrafficVolume_" + trafficType + "_" + "stopPerZone_" + sampleName + "pt.csv");
		writeCSVTrafficVolume(trafficVolume_stop, outputFileStop);
		log.info("Write traffic volume for stop trips per zone in CSV: {}", outputFileStop);
		return trafficVolume_stop;
	}

	/**
	 * Calculates the traffic volume for each zone and purpose.
	 *
	 * @param trafficVolume traffic volumes
	 * @param resultingDataPerZone employee date for each zone
	 * @param volumeType start or stop rates
	 * @param modesORvehTypes selected mode or vehicleType
	 * @param sample sample size
	 */
	private static void calculateTrafficVolumePerZone(
			Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume,
			Map<String, Object2DoubleMap<String>> resultingDataPerZone, String volumeType, double sample,
			List<String> modesORvehTypes) {

		Map<Integer, Map<String, Double>> generationRates;
		Map<String, Map<String, Double>> commitmentRates;

		if (volumeType.equals("start")) {
			generationRates = generationRatesStart;
			commitmentRates = commitmentRatesStart;
		} else if (volumeType.equals("stop")) {
			generationRates = generationRatesStop;
			commitmentRates = commitmentRatesStop;
		} else
			throw new RuntimeException("No generation and commitment rates selected. Please check!");

		for (String zoneId : resultingDataPerZone.keySet()) {
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey key = makeTrafficVolumeKey(zoneId, modeORvehType);
				Object2DoubleMap<Integer> trafficValuesPerPurpose = new Object2DoubleOpenHashMap<>();
				for (Integer purpose : generationRates.keySet()) {

					if (resultingDataPerZone.get(zoneId).isEmpty())
						trafficValuesPerPurpose.merge(purpose, 0., Double::sum);
					else
						for (String category : resultingDataPerZone.get(zoneId).keySet()) {
							double commitmentFactor;
							if (modeORvehType.equals("total"))
								commitmentFactor = 1;
							else
								commitmentFactor = commitmentRates
										.get(purpose + "_" + modeORvehType.substring(modeORvehType.length() - 1))
										.get(category);
							double generationFactor = generationRates.get(purpose).get(category);
							double newValue = resultingDataPerZone.get(zoneId).getDouble(category) * generationFactor
									* commitmentFactor;
							trafficValuesPerPurpose.merge(purpose, newValue, Double::sum);
						}
					int sampledVolume = (int) Math.round(sample * trafficValuesPerPurpose.getDouble(purpose));
					trafficValuesPerPurpose.replace(purpose, sampledVolume);
				}
				trafficVolume.put(key, trafficValuesPerPurpose);
			}
		}
	}

	/**
	 * Writes the traffic volume.
	 *
	 * @param trafficVolume traffic volumes for each combination
	 * @param outputFileInInputFolder location of written output
	 */
	private static void writeCSVTrafficVolume(Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume,
			Path outputFileInInputFolder) throws MalformedURLException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFileInInputFolder.toUri().toURL(),
				StandardCharsets.UTF_8, true);
		try {
			String[] header = new String[] { "zoneID", "mode/vehType", "1", "2", "3", "4", "5" };
			JOIN.appendTo(writer, header);
			writer.write("\n");
			for (TrafficVolumeKey trafficVolumeKey : trafficVolume.keySet()) {
				List<String> row = new ArrayList<>();
				row.add(trafficVolumeKey.getZone());
				row.add(trafficVolumeKey.getModeORvehType());
				int count = 1;
				while (count < 6) {
					row.add(String.valueOf((int) trafficVolume.get(trafficVolumeKey).getDouble(count)));
					count++;
				}
				JOIN.appendTo(writer, row);
				writer.write("\n");
			}
			writer.close();

		} catch (IOException e) {
			log.error("Problem writing traffic volume file.", e);
		}
	}

	/**
	 * Loads the input data based on the selected smallScaleCommercialTrafficType.
	 *
	 * @param smallScaleCommercialTrafficType used smallScaleCommercialTrafficType (freight or business traffic)
	 */
	static void setInputParameters(String smallScaleCommercialTrafficType) {

		// Set generation rates for start potentials
		generationRatesStart = GetGenerationRates.setGenerationRates(smallScaleCommercialTrafficType, "start");

		// Set generation rates for stop potentials
		generationRatesStop = GetGenerationRates.setGenerationRates(smallScaleCommercialTrafficType, "stop");

		// Set commitment rates for start potentials
		commitmentRatesStart = GetGenerationRates.setCommitmentRates(smallScaleCommercialTrafficType, "start");

		// Set commitment rates for stop potentials
		commitmentRatesStop = GetGenerationRates.setCommitmentRates(smallScaleCommercialTrafficType, "stop");
	}

}
