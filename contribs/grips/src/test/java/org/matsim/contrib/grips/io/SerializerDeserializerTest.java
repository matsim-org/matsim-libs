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

package org.matsim.contrib.grips.io;

import org.junit.Test;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DepartureTimeDistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.DistributionType;
import org.matsim.contrib.grips.io.jaxb.gripsconfig.ObjectFactory;
import org.matsim.contrib.grips.model.config.GripsConfigModule;
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
			DistributionType distrType = DistributionType.LOG_NORMAL;
			double mu = 0;
			double sigma = .25;
			double sampleSize = 0.787;

			double earliest = MatsimRandom.getRandom().nextDouble();
			double latest = MatsimRandom.getRandom().nextDouble()+1;
			
			GripsConfigModule gcm = new GripsConfigModule("grips");
			gcm.setEvacuationAreaFileName(evacuationAreaFile);
			gcm.setNetworkFileName(networkFile);
			gcm.setOutputDir(outputDir);
			gcm.setPopulationFileName(populationFile);
			gcm.setSampleSize(Double.toString(sampleSize));
			gcm.setMainTrafficType(mainTrafficType);
			
			
			ObjectFactory fac = new ObjectFactory();
			DepartureTimeDistributionType departureTimeDistribution = fac.createDepartureTimeDistributionType();
			departureTimeDistribution.setDistribution(distrType );
			departureTimeDistribution.setMu(mu);
			departureTimeDistribution.setSigma(sigma);
			departureTimeDistribution.setEarliest(earliest);
			departureTimeDistribution.setLatest(latest);
			
			gcm.setDepartureTimeDistribution(departureTimeDistribution );
			
			GripsConfigSerializer serializer = new GripsConfigSerializer(gcm);
			serializer.serialize(outputFile);
			
			GripsConfigModule gcm2 = new GripsConfigModule("grips");
			GripsConfigDeserializer deserializer = new GripsConfigDeserializer(gcm2,false);
			deserializer.readFile(outputFile);
			
			assertEquals(evacuationAreaFile, gcm2.getEvacuationAreaFileName());
			assertEquals(networkFile, gcm2.getNetworkFileName());
			assertEquals(outputDir, gcm2.getOutputDir());
			assertEquals(populationFile, gcm2.getPopulationFileName());
			assertEquals(sampleSize, gcm2.getSampleSize());
			assertEquals(distrType.value(), gcm2.getDepartureTimeDistribution().getDistribution().value());
			assertEquals(sigma, gcm2.getDepartureTimeDistribution().getSigma());
			assertEquals(mu, gcm2.getDepartureTimeDistribution().getMu());
			assertEquals(earliest, gcm2.getDepartureTimeDistribution().getEarliest());
			assertEquals(latest, gcm2.getDepartureTimeDistribution().getLatest());
			assertEquals(mainTrafficType, gcm2.getMainTrafficType());
		}

}
