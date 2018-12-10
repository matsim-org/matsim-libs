/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accessibility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author dziemke
 */
public final class AccessibilityShutdownListenerV4 implements ShutdownListener {
	private static final Logger LOG = Logger.getLogger(AccessibilityShutdownListenerV4.class);

	private final AccessibilityCalculator accessibilityCalculator;
	private String outputDirectory;
	private AccessibilityConfigGroup acg;
	
	private AccessibilityAggregator accessibilityAggregator;
	private ActivityFacilities opportunities;

	
	public AccessibilityShutdownListenerV4(AccessibilityCalculator accessibilityCalculator, ActivityFacilities opportunities, 
			String outputDirectory, AccessibilityConfigGroup acg) {
		this.opportunities = opportunities;
		this.accessibilityCalculator = accessibilityCalculator;
		this.outputDirectory = outputDirectory;
		this.acg = acg;
	}

	private List<ActivityFacilities> additionalFacilityData = new ArrayList<>() ;

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		if (event.isUnexpected()) {
			return;
		}
		LOG.info("Initializing accessibility computation...");
		accessibilityAggregator = new AccessibilityAggregator();
		accessibilityCalculator.addFacilityDataExchangeListener(accessibilityAggregator);

		if (outputDirectory != null) {
			File file = new File(outputDirectory);
			file.mkdirs();
		}

		LOG.info("Start computing accessibilities.");
		accessibilityCalculator.computeAccessibilities(acg.getTimeOfDay(), opportunities);
		LOG.info("Finished computing accessibilities.");

		writeCSVFile(outputDirectory);
	}

	private void writeCSVFile(String adaptedOutputDirectory) {
		LOG.info("Start writing accessibility output to " + adaptedOutputDirectory + ".");

		Map<Tuple<ActivityFacility, Double>, Map<String,Double>> accessibilitiesMap = accessibilityAggregator.getAccessibilitiesMap();
		final CSVWriter writer = new CSVWriter(adaptedOutputDirectory + "/" + CSVWriter.FILE_NAME ) ;

		// Write header
		writer.writeField(Labels.X_COORDINATE);
		writer.writeField(Labels.Y_COORDINATE);
		writer.writeField(Labels.TIME);
		for (String mode : accessibilityCalculator.getModes() ) {
			writer.writeField(mode + "_accessibility");
		}
		for (ActivityFacilities additionalDataFacilities : this.additionalFacilityData) { // Iterate over all additional data collections
			String additionalDataName = additionalDataFacilities.getName();
			writer.writeField(additionalDataName);
		}
		writer.writeNewLine();

		// Write data
		for (Tuple<ActivityFacility, Double> tuple : accessibilitiesMap.keySet()) {
			ActivityFacility facility = tuple.getFirst();
			writer.writeField(facility.getCoord().getX());
			writer.writeField(facility.getCoord().getY());
			writer.writeField(tuple.getSecond());
			
			for (String mode : accessibilityCalculator.getModes() ) {
				final double value = accessibilitiesMap.get(tuple).get(mode);
				if (!Double.isNaN(value)) { 
					writer.writeField(value) ;
				} else {
					writer.writeField(Double.NaN) ;
				}
			}
			for (ActivityFacilities additionalDataFacilities : this.additionalFacilityData) { // Again: Iterate over all additional data collections
				String additionalDataName = additionalDataFacilities.getName();
				int value = (int) facility.getAttributes().getAttribute(additionalDataName);
				writer.writeField(value);
			}
			writer.writeNewLine();
		}
		writer.close() ;
		LOG.info("Finished writing accessibility output to " + adaptedOutputDirectory + ".");
	}
	
	public void addFacilityDataExchangeListener( FacilityDataExchangeInterface facilityDataExchangeListener ) {
		this.accessibilityCalculator.addFacilityDataExchangeListener(facilityDataExchangeListener);
	}
}