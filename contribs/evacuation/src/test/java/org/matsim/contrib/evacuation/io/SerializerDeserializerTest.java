/* *********************************************************************** *
 * project: org.matsim.*
 * SerializerDeserializerTest.java
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

package org.matsim.contrib.evacuation.io;

import org.junit.Test;
import org.matsim.contrib.evacuation.io.DepartureTimeDistribution;
import org.matsim.contrib.evacuation.io.EvacuationConfigReader;
import org.matsim.contrib.evacuation.io.EvacuationConfigWriter;
import org.matsim.contrib.evacuation.model.config.EvacuationConfigModule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.testcases.MatsimTestCase;

public class SerializerDeserializerTest extends MatsimTestCase {
		
		@Test
		public void testSerializerDeserializer() {
			String outputFile = getOutputDirectory() +"/test.xml";
			
			String evacuationAreaFile = "evacuation-area-dummy-file-name";
			String networkFile = "network-dummy-file-name";
			String outputDir = "dummy-output-dir-name";
			String populationFile = "population-dummy-file-name";
			String mainTrafficType = "mixed";
			String distrType = DepartureTimeDistribution.LOG_NORMAL;
			double mu = 0;
			double sigma = .25;
			double sampleSize = 0.787;

			double earliest = MatsimRandom.getRandom().nextDouble();
			double latest = MatsimRandom.getRandom().nextDouble()+1;
			
			EvacuationConfigModule gcm = new EvacuationConfigModule("grips");
			gcm.setEvacuationAreaFileName(evacuationAreaFile);
			gcm.setNetworkFileName(networkFile);
			gcm.setOutputDir(outputDir);
			gcm.setPopulationFileName(populationFile);
			gcm.setSampleSize(Double.toString(sampleSize));
			gcm.setMainTrafficType(mainTrafficType);
			
			
			DepartureTimeDistribution departureTimeDistribution = new DepartureTimeDistribution();
			departureTimeDistribution.setDistribution(distrType );
			departureTimeDistribution.setMu(mu);
			departureTimeDistribution.setSigma(sigma);
			departureTimeDistribution.setEarliest(earliest);
			departureTimeDistribution.setLatest(latest);
			
			gcm.setDepartureTimeDistribution(departureTimeDistribution );
			
			EvacuationConfigWriter writer = new EvacuationConfigWriter(gcm);
			writer.write(outputFile);
			
			EvacuationConfigModule gcm2 = new EvacuationConfigModule("grips");
			EvacuationConfigReader reader = new EvacuationConfigReader(gcm2);
			reader.parse(outputFile);
			
			assertEquals(evacuationAreaFile, gcm2.getEvacuationAreaFileName());
			assertEquals(networkFile, gcm2.getNetworkFileName());
			assertEquals(outputDir, gcm2.getOutputDir());
			assertEquals(populationFile, gcm2.getPopulationFileName());
			assertEquals(sampleSize, gcm2.getSampleSize());
			assertEquals(distrType, gcm2.getDepartureTimeDistribution().getDistribution());
			assertEquals(sigma, gcm2.getDepartureTimeDistribution().getSigma());
			assertEquals(mu, gcm2.getDepartureTimeDistribution().getMu());
			assertEquals(earliest, gcm2.getDepartureTimeDistribution().getEarliest());
			assertEquals(latest, gcm2.getDepartureTimeDistribution().getLatest());
			assertEquals(mainTrafficType, gcm2.getMainTrafficType());
		}

}
