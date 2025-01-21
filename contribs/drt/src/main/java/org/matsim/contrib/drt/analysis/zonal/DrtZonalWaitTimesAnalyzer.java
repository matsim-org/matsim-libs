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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector.EventSequence;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileWriter;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public final class DrtZonalWaitTimesAnalyzer implements IterationEndsListener, ShutdownListener {

	private final DrtConfigGroup drtCfg;
	private final DrtEventSequenceCollector requestAnalyzer;
	private final ZoneSystem zones;
	private static final Id<Zone> zoneIdForOutsideOfZonalSystem = Id.create("outsideOfDrtZonalSystem", Zone.class);
	private static final String notAvailableString = "NaN";

	private final String delimiter;
	private static final Logger log = LogManager.getLogger(DrtZonalWaitTimesAnalyzer.class);

	public DrtZonalWaitTimesAnalyzer(DrtConfigGroup configGroup, DrtEventSequenceCollector requestAnalyzer,
			ZoneSystem zones, String delimiter) {
		this.drtCfg = configGroup;
		this.requestAnalyzer = requestAnalyzer;
		this.zones = zones;
		this.delimiter = delimiter;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getServices()
				.getControlerIO()
				.getIterationFilename(event.getIteration(), "waitStats" + "_" + drtCfg.getMode() + "_zonal.csv");
		write(fileName);
	}

	public void write(String fileName) {
		Map<Id<Zone>, ZonalStatistics> zoneStats = createZonalStats();
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			DecimalFormat format = new DecimalFormat();
			format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
			format.setMinimumIntegerDigits(1);
			format.setMaximumFractionDigits(2);
			format.setGroupingUsed(false);
			String header = new StringJoiner(delimiter)
					.add("zone").add("centerX").add("centerY").add("nRequests")
					.add("sumWaitTime").add("meanWaitTime").add("min").add("max")
					.add("p95").add("p90").add("p80").add("p75").add("p50")
					.add("rejections").add("rejectionRate").toString();
			bw.append(header);
			// sorted output
			SortedSet<Id<Zone>> zoneIdsAndOutside = new TreeSet<>(zones.getZones().keySet());
			zoneIdsAndOutside.add(zoneIdForOutsideOfZonalSystem);

			for (Id<Zone> zoneId : zoneIdsAndOutside) {
				Zone drtZone = zones.getZones().get(zoneId);
				String centerX = drtZone != null ? String.valueOf(drtZone.getCentroid().getX()) : notAvailableString;
				String centerY = drtZone != null ? String.valueOf(drtZone.getCentroid().getY()) : notAvailableString;
				DescriptiveStatistics stats = zoneStats.get(zoneId).waitStats;
				Set<Id<Request>> rejections = zoneStats.get(zoneId).rejections;
				bw.newLine();
				bw.append(
						new StringJoiner(delimiter)
						.add(zoneId.toString())
						.add(centerX)
						.add(centerY)
						.add(format.format(stats.getN()))
						.add(format.format(stats.getSum()))
						.add(String.valueOf(stats.getMean()))
						.add(String.valueOf(stats.getMin()))
						.add(String.valueOf(stats.getMax()))
						.add(String.valueOf(stats.getPercentile(95)))
						.add(String.valueOf(stats.getPercentile(90)))
						.add(String.valueOf(stats.getPercentile(80)))
						.add(String.valueOf(stats.getPercentile(75)))
						.add(String.valueOf(stats.getPercentile(50)))
						.add(String.valueOf(rejections.size()))
						.add(String.valueOf(rejections.size() / (double) (rejections.size() + stats.getN())))
						.toString()
				);
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	record ZonalStatistics(DescriptiveStatistics waitStats, Set<Id<Request>> rejections){}

	private Map<Id<Zone>, ZonalStatistics> createZonalStats() {
		Map<Id<Zone>, ZonalStatistics> zoneStats = new IdMap<>(Zone.class);
		// prepare stats for all zones
		for (Id<Zone> zoneId : zones.getZones().keySet()) {
			zoneStats.put(zoneId, new ZonalStatistics(new DescriptiveStatistics(), new HashSet<>()));
		}
		zoneStats.put(zoneIdForOutsideOfZonalSystem, new ZonalStatistics(new DescriptiveStatistics(), new HashSet<>()));

		for (EventSequence seq : requestAnalyzer.getPerformedRequestSequences().values()) {
			for (Map.Entry<Id<Person>, EventSequence.PersonEvents> entry : seq.getPersonEvents().entrySet()) {
				if(entry.getValue().getPickedUp().isPresent()) {
					Id<Zone> zone = zones.getZoneForLinkId(seq.getSubmitted().getFromLinkId())
						.map(Identifiable::getId).orElse(zoneIdForOutsideOfZonalSystem);
					double waitTime = entry.getValue().getPickedUp().get() .getTime() - seq.getSubmitted().getTime();
					zoneStats.get(zone).waitStats.addValue(waitTime);
				}
			}
		}

		for (EventSequence seq : requestAnalyzer.getRejectedRequestSequences().values()) {
			Id<Zone> zone = zones.getZoneForLinkId(seq.getSubmitted().getFromLinkId())
					.map(Identifiable::getId).orElse(zoneIdForOutsideOfZonalSystem);
			zoneStats.get(zone).rejections.add(seq.getSubmitted().getRequestId());
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
		if(!features.isEmpty()) {
			String fileName = event.getServices()
				.getControlerIO()
				.getOutputFilename("drt_waitStats" + "_" + drtCfg.getMode() + "_zonal.gpkg");
			GeoFileWriter.writeGeometries(features, fileName);
		}
	}

	private Collection<SimpleFeature> convertGeometriesToSimpleFeatures(String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
		try {
			simpleFeatureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		} catch (IllegalArgumentException e) {
			log.warn("Coordinate reference system \""
					+ targetCoordinateSystem
					+ "\" is unknown. Please set a crs in config global. Will not create drt_waitStats_"
					+ drtCfg.getMode()
					+ "_zonal.gpkg.");
			return Collections.emptyList();
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
		simpleFeatureBuilder.add("rejections", Double.class);
		simpleFeatureBuilder.add("rejectRate", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureBuilder.buildFeatureType());

		Collection<SimpleFeature> features = new ArrayList<>();

		Map<Id<Zone>, ZonalStatistics> zoneStats = createZonalStats();

		for (Zone zone : zones.getZones().values()) {
			Object[] routeFeatureAttributes = new Object[16];
			Geometry geometry = zone.getPreparedGeometry() != null ? zone.getPreparedGeometry().getGeometry() : null;
			DescriptiveStatistics stats = zoneStats.get(zone.getId()).waitStats;
			Set<Id<Request>> rejections = zoneStats.get(zone.getId()).rejections;
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
			routeFeatureAttributes[14] = rejections.size();
			routeFeatureAttributes[15] = rejections.size() / (double) (rejections.size() + stats.getN());

			try {
				features.add(builder.buildFeature(zone.getId().toString(), routeFeatureAttributes));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		return features;
	}
}
