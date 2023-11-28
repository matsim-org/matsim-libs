/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.contrib.drt.analysis.zonal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

public final class DrtZonalWaitTimesAnalyzer implements IterationEndsListener, ShutdownListener {

	private final DrtConfigGroup drtCfg;
	private final DrtEventSequenceCollector requestAnalyzer;
	private final DrtZonalSystem zones;
	private static final String zoneIdForOutsideOfZonalSystem = "outsideOfDrtZonalSystem";
	private static final String notAvailableString = "NaN";
	private static final Logger log = LogManager.getLogger(DrtZonalWaitTimesAnalyzer.class);

	public DrtZonalWaitTimesAnalyzer(DrtConfigGroup configGroup, DrtEventSequenceCollector requestAnalyzer,
			DrtZonalSystem zones) {
		this.drtCfg = configGroup;
		this.requestAnalyzer = requestAnalyzer;
		this.zones = zones;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getServices()
				.getControlerIO()
				.getIterationFilename(event.getIteration(), "waitStats" + "_" + drtCfg.getMode() + "_zonal.csv");
		write(fileName);
	}

	public void write(String fileName) {
		String delimiter = ";";
		Map<String, DescriptiveStatistics> zoneStats = createZonalStats();
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			DecimalFormat format = new DecimalFormat();
			format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
			format.setMinimumIntegerDigits(1);
			format.setMaximumFractionDigits(2);
			format.setGroupingUsed(false);
			bw.append("zone;centerX;centerY;nRequests;sumWaitTime;meanWaitTime;min;max;p95;p90;p80;p75;p50");
			// sorted output
			SortedSet<String> zoneIdsAndOutside = new TreeSet<>(zones.getZones().keySet());
			zoneIdsAndOutside.add(zoneIdForOutsideOfZonalSystem);

			for (String zoneId : zoneIdsAndOutside) {
				DrtZone drtZone = zones.getZones().get(zoneId);
				String centerX = drtZone != null ? String.valueOf(drtZone.getCentroid().getX()) : notAvailableString;
				String centerY = drtZone != null ? String.valueOf(drtZone.getCentroid().getY()) : notAvailableString;
				DescriptiveStatistics stats = zoneStats.get(zoneId);
				bw.newLine();
				bw.append(zoneId)
						.append(delimiter)
						.append(centerX)
						.append(delimiter)
						.append(centerY)
						.append(delimiter)
						.append(format.format(stats.getN()))
						.append(delimiter)
						.append(format.format(stats.getSum()))
						.append(delimiter)
						.append(String.valueOf(stats.getMean()))
						.append(delimiter)
						.append(String.valueOf(stats.getMin()))
						.append(delimiter)
						.append(String.valueOf(stats.getMax()))
						.append(delimiter)
						.append(String.valueOf(stats.getPercentile(95)))
						.append(delimiter)
						.append(String.valueOf(stats.getPercentile(90)))
						.append(delimiter)
						.append(String.valueOf(stats.getPercentile(80)))
						.append(delimiter)
						.append(String.valueOf(stats.getPercentile(75)))
						.append(delimiter)
						.append(String.valueOf(stats.getPercentile(50)));
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Map<String, DescriptiveStatistics> createZonalStats() {
		Map<String, DescriptiveStatistics> zoneStats = new HashMap<>();
		// prepare stats for all zones
		for (String zoneId : zones.getZones().keySet()) {
			zoneStats.put(zoneId, new DescriptiveStatistics());
		}
		zoneStats.put(zoneIdForOutsideOfZonalSystem, new DescriptiveStatistics());

		for (EventSequence seq : requestAnalyzer.getPerformedRequestSequences().values()) {
			for (Map.Entry<Id<Person>, EventSequence.PersonEvents> entry : seq.getPersonEvents().entrySet()) {
				if(entry.getValue().getPickedUp().isPresent()) {
					DrtZone zone = zones.getZoneForLinkId(seq.getSubmitted().getFromLinkId());
					final String zoneStr = zone != null ? zone.getId() : zoneIdForOutsideOfZonalSystem;
					double waitTime = entry.getValue().getPickedUp().get() .getTime() - seq.getSubmitted().getTime();
					zoneStats.get(zoneStr).addValue(waitTime);
				}
			}
		}
		return zoneStats;
	}

	/**
	 * Write shp file with last iteration's stats
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String crs = event.getServices().getConfig().global().getCoordinateSystem();
		Collection<SimpleFeature> features = convertGeometriesToSimpleFeatures(crs);
		String fileName = event.getServices()
				.getControlerIO()
				.getOutputFilename("drt_waitStats" + "_" + drtCfg.getMode() + "_zonal.shp");
		ShapeFileWriter.writeGeometries(features, fileName);
	}

	private Collection<SimpleFeature> convertGeometriesToSimpleFeatures(String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
		try {
			simpleFeatureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		} catch (IllegalArgumentException e) {
			log.warn("Coordinate reference system \""
					+ targetCoordinateSystem
					+ "\" is unknown. Please set a crs in config global. Will try to create drt_waitStats_"
					+ drtCfg.getMode()
					+ "_zonal.shp anyway.");
		}

		simpleFeatureBuilder.setName("drtZoneFeature");
		// note: column names may not be longer than 10 characters. Otherwise the name is cut after the 10th character and the avalue is NULL in QGis
		simpleFeatureBuilder.add("the_geom", Polygon.class);
		simpleFeatureBuilder.add("zoneIid", String.class);
		simpleFeatureBuilder.add("centerX", Double.class);
		simpleFeatureBuilder.add("centerY", Double.class);
		simpleFeatureBuilder.add("nRequests", Integer.class);
		simpleFeatureBuilder.add("sumWait", Double.class);
		simpleFeatureBuilder.add("meanWait", Double.class);
		simpleFeatureBuilder.add("min", Double.class);
		simpleFeatureBuilder.add("max", Double.class);
		simpleFeatureBuilder.add("p95", Double.class);
		simpleFeatureBuilder.add("p90", Double.class);
		simpleFeatureBuilder.add("p80", Double.class);
		simpleFeatureBuilder.add("p75", Double.class);
		simpleFeatureBuilder.add("p50", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureBuilder.buildFeatureType());

		Collection<SimpleFeature> features = new ArrayList<>();

		Map<String, DescriptiveStatistics> zoneStats = createZonalStats();

		for (DrtZone zone : zones.getZones().values()) {
			Object[] routeFeatureAttributes = new Object[14];
			Geometry geometry = zone.getPreparedGeometry() != null ? zone.getPreparedGeometry().getGeometry() : null;
			DescriptiveStatistics stats = zoneStats.get(zone.getId());
			routeFeatureAttributes[0] = geometry;
			routeFeatureAttributes[1] = zone.getId();
			routeFeatureAttributes[2] = zone.getCentroid().getX();
			routeFeatureAttributes[3] = zone.getCentroid().getY();
			routeFeatureAttributes[4] = stats.getN();
			routeFeatureAttributes[5] = stats.getSum();
			routeFeatureAttributes[6] = stats.getMean();
			routeFeatureAttributes[7] = stats.getMin();
			routeFeatureAttributes[8] = stats.getMax();
			routeFeatureAttributes[9] = stats.getPercentile(95);
			routeFeatureAttributes[10] = stats.getPercentile(90);
			routeFeatureAttributes[11] = stats.getPercentile(80);
			routeFeatureAttributes[12] = stats.getPercentile(75);
			routeFeatureAttributes[13] = stats.getPercentile(50);

			try {
				features.add(builder.buildFeature(zone.getId(), routeFeatureAttributes));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		return features;
	}
}
