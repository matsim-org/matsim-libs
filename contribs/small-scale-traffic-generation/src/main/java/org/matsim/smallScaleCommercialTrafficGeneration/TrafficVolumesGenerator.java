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
import org.jspecify.annotations.NonNull;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.smallScaleCommercialTrafficGeneration.data.GetGenerationRates;
import org.matsim.smallScaleCommercialTrafficGeneration.SmallScaleCommercialTrafficUtils.ZoneAttribute;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static org.matsim.smallScaleCommercialTrafficGeneration.GenerateSmallScaleCommercialTrafficDemand.*;

/**
 * @author Ricardo Ewert
 *
 */
class TrafficVolumesGenerator{

	private static final Logger log = LogManager.getLogger( TrafficVolumesGenerator.class );
	private static final Joiner JOIN = Joiner.on("\t");

	private Map<Integer, Map<ZoneAttribute, Double>> generationRatesStart = new HashMap<>();
	private Map<Integer, Map<ZoneAttribute, Double>> generationRatesStop = new HashMap<>();
	private Map<String, Map<ZoneAttribute, Double>> commitmentRatesStart = new HashMap<>();
	private Map<String, Map<ZoneAttribute, Double>> commitmentRatesStop = new HashMap<>();

	record TrafficVolumeKey(String zone, String modeORvehType) {}

	static TrafficVolumeKey makeTrafficVolumeKey(String zone, String modeORvehType) {
		return new TrafficVolumeKey(zone, modeORvehType);
	}

	TrafficVolumesGenerator( SmallScaleCommercialTrafficSegment smallScaleCommercialTrafficSegment ) {
		// Set generation rates for start potentials
		generationRatesStart = GetGenerationRates.setGenerationRates( smallScaleCommercialTrafficSegment, "start" );

		// Set generation rates for stop potentials
		generationRatesStop = GetGenerationRates.setGenerationRates( smallScaleCommercialTrafficSegment, "stop" );

		// Set commitment rates for start potentials
		commitmentRatesStart = GetGenerationRates.setCommitmentRates( smallScaleCommercialTrafficSegment, "start" );

		// Set commitment rates for stop potentials
		commitmentRatesStop = GetGenerationRates.setCommitmentRates( smallScaleCommercialTrafficSegment, "stop" );
	}

	/**
	 * Creates the traffic volume (stop) for each zone separated in the
	 * modesORvehTypes and the purposes.
	 *
	 * @param attributesByZone employee data for each zone
	 * @param outputPath output path
	 * @param sample sample size
	 * @param modesORvehTypes selected mode or vehicleType
	 * @return trafficVolume
	 */
	@NonNull Map<TrafficVolumeKey, Object2DoubleMap<Integer>> createTrafficVolumes(
		Map<String, Object2DoubleMap<ZoneAttribute>> attributesByZone, Path outputPath, double sample, List<String> modesORvehTypes,
		SmallScaleCommercialTrafficSegment segment, String startOrStop ) throws MalformedURLException
	{
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume_stop = new HashMap<>();
		calculateTrafficVolumePerZone(trafficVolume_stop, attributesByZone, startOrStop, sample, modesORvehTypes );
		String sampleName = SmallScaleCommercialTrafficUtils.getSampleNameOfOutputFolder(sample);
		Path outputFile = outputPath.resolve("calculatedData").resolve("TrafficVolume_" + segment + "_" + startOrStop + "PerZone_"+ sampleName + "pt.csv" ); // this should presumably be "pct" instead of "pt"
		writeCSVTrafficVolume(trafficVolume_stop, outputFile);
		log.info( "Write traffic volume for {} trips per zone in CSV: {}", startOrStop, outputFile );
		return trafficVolume_stop;
	}

	/**
	 * Calculates the traffic volume for each zone and purpose.
	 *
	 * @param trafficVolume traffic volumes
	 * @param zoneAttributes employee date for each zone // really only employees?
	 * @param startOrStop start or stop rates
	 * @param modesORvehTypes selected mode or vehicleType
	 * @param sample sample size
	 */
	private void calculateTrafficVolumePerZone(
		Map<TrafficVolumeKey, Object2DoubleMap<Integer>> trafficVolume,
		Map<String, Object2DoubleMap<ZoneAttribute>> zoneAttributes,
		String startOrStop,
		double sample,
		List<String> modesORvehTypes)
	{
		// what is called "volume" here is "Aufkommen" (Stärke der Quellen und Senken) in German.  ChatGPT schlägt in der Tat "volume" vor.

		Map<Integer, Map<ZoneAttribute, Double>> generationRates;
		Map<String, Map<ZoneAttribute, Double>> commitmentRates;

		if (startOrStop.equals("start")) {
			generationRates = generationRatesStart;
			commitmentRates = commitmentRatesStart; // "Bindungsraten" in german.  Presumably to "car"??
		} else if (startOrStop.equals("stop")) {
			generationRates = generationRatesStop;
			commitmentRates = commitmentRatesStop;
		} else{
			throw new RuntimeException( "No generation and commitment rates selected. Please check!" );
		}

		for (String zoneId : zoneAttributes.keySet()) {
			for (String modeORvehType : modesORvehTypes) {
				TrafficVolumeKey trafficVolumeKey = makeTrafficVolumeKey(zoneId, modeORvehType);
				// the "modeOrVehType" has to do with the fact that IVV has the "Bindungsraten" per mode for
				// commercial person traffic, and per vehicle type for commercial goods traffic.
				// yyyy does the present code really have different modes for commercial person traffic? kai, jul'26

				Object2DoubleMap<Integer> trafficValuesPerPurpose = new Object2DoubleOpenHashMap<>();

				for (Integer purpose : generationRates.keySet()) {
					// yy iterate over the entrySet instead of over the keySet

					// This feels the wrong way round ... I would have expected the zone first, and everything
					// related to the zone inside.  (Might also be faster.)  But when reading the IVV source,
					// one can see why it ended up the other way round here.

					// this is going over the IVV trip purposes.  yy translate to enum so this becomes readable

					if (zoneAttributes.get(zoneId).isEmpty()){
						// no zone attributes for this zone available.  I am not sure what this means or why
						// this could happen; presumably, input data is allowed to be empty here.

						trafficValuesPerPurpose.merge( purpose, 0., Double::sum );
						// yy why do we "merge" and not just "set"?
					} else{
						for( ZoneAttribute category : zoneAttributes.get( zoneId ).keySet() ){
							if( !generationRates.get( purpose ).containsKey( category ) ){
								// yy if we would iterate over the entrySet, this would just be entry.getValue().containsKey(
								// the "purpose" will be there, but possibly not the category (e.g., no #(employees)).
								// yy not sure under which circumstances this is possible.  We
								// should not hedge against things which should not happen.

								continue;
							}
							double commitmentFactor;
							if( modeORvehType.equals( "total" ) ){
								commitmentFactor = 1;
							} else{
								final String lookupString = purpose + "_" + modeORvehType.substring( modeORvehType.length() - 1 );
								// (what the heck is this?? (1) this should be encapsulated; (2) why the substring operation?)

								if( !commitmentRates.get( lookupString ).containsKey( category ) ){
									commitmentFactor = 0;
								} else{
									commitmentFactor = commitmentRates.get( lookupString ).get( category );
									// ("Bindungsrate" for given mode or vehicle type)
								}
							}

							double generationFactor = generationRates.get( purpose ).get( category );
							// (same lookup as for the commitment factor except that it does not have to hedge)

							double newValue = zoneAttributes.get( zoneId ).getDouble( category ) * generationFactor * commitmentFactor;
							// (according to ivv source at end of section 7.2.1.    IVV also has a "Nutzungsfaktor"; this is not included here (why?).

							trafficValuesPerPurpose.merge( purpose, newValue, Double::sum );
							// (yy I can't say under which circumstances the "sum" will really be used; as far as I can tell, each purposes is called at most once.)
						}
					}
					// rounding to integers here (yy I do not find it obvious that this here is the best place to do that):
					int sampledVolume = (int) Math.round(sample * trafficValuesPerPurpose.getDouble(purpose));
					trafficValuesPerPurpose.replace(purpose, sampledVolume);
				}
				trafficVolume.put(trafficVolumeKey, trafficValuesPerPurpose);
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
			// Sort the entries by zone and mode/vehType to ensure a consistent order in the output file
			List<Map.Entry<TrafficVolumeKey, Object2DoubleMap<Integer>>> sortedEntries =
				trafficVolume.entrySet()
					.stream()
					.sorted(
						Comparator.comparing((Map.Entry<TrafficVolumeKey, Object2DoubleMap<Integer>> entry) -> entry.getKey().zone())
							.thenComparing(entry -> entry.getKey().modeORvehType())
					)
					.toList();
			for (Map.Entry<TrafficVolumeKey, Object2DoubleMap<Integer>> entry : sortedEntries) {
				TrafficVolumeKey trafficVolumeKey = entry.getKey();
				List<String> row = new ArrayList<>();
				row.add(trafficVolumeKey.zone());
				row.add(trafficVolumeKey.modeORvehType());
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

}
