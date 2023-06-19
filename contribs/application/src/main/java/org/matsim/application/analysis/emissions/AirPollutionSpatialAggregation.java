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
package org.matsim.application.analysis.emissions;

import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @deprecated Use {@link AirPollutionAnalysis}
 */
@CommandLine.Command(
		name = "air-pollution-spatial-aggregation",
		description = "Aggregate emissions on a spatial grid",
		mixinStandardHelpOptions = true,
		showDefaultValues = true
)
@Deprecated
public class AirPollutionSpatialAggregation implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(AirPollutionSpatialAggregation.class);

	@CommandLine.Parameters(arity = "1", paramLabel = "INPUT", description = "Path to emission event file")
	private Path events;

	@CommandLine.Option(names = "--network", description = "Path to network file")
	private Path network;

	@CommandLine.Option(names = "--output", description = "Output csv")
	private Path output;

	@CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "100")
	private double gridSize;

	@CommandLine.Option(names = "--radius", description = "Smoothing radius", defaultValue = "20")
	private double radius;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	@CommandLine.Mixin
	private CsvOptions csv;

	@Override
	public Integer call() throws Exception {

		Network network = NetworkUtils.readNetwork(this.network.toString());

		ShpOptions.Index index = shp.getShapeFile() != null ? shp.createIndex(ProjectionUtils.getCRS(network), "_") : null;

		var filteredNetwork = network.getLinks().values().parallelStream()
				.filter(link -> index == null || index.contains(link.getCoord()))
				.collect(NetworkUtils.getCollector());

		Map<Pollutant, Raster> rasterMap = FastEmissionGridAnalyzer.processEventsFile(events.toString(), filteredNetwork, gridSize, 20);

		Set<Pollutant> poll = rasterMap.keySet();

		List<Integer> xLength = rasterMap.values().stream().map(Raster::getXLength).distinct().collect(Collectors.toList());
		List<Integer> yLength = rasterMap.values().stream().map(Raster::getYLength).distinct().collect(Collectors.toList());

		if (xLength.size() != 1 || yLength.size() != 1) {

			log.error("Rasters have different sizes: {} x {}", xLength, yLength);

			return 2;
		}

		Raster raster = rasterMap.values().stream().findFirst().orElseThrow();

		try (CSVPrinter printer = csv.createPrinter(output)) {

			// print header
			printer.print("x");
			printer.print("y");

			for (Pollutant p : poll) {
				printer.print(p.name());
			}


			printer.println();

			for (int xi = 0; xi < xLength.get(0); xi++) {
				for (int yi = 0; yi < yLength.get(0); yi++) {

					Coord coord = raster.getCoordForIndex(xi, yi);

					printer.print(coord.getX());
					printer.print(coord.getY());

					for (Pollutant p : poll) {

						double value = rasterMap.get(p).getValueByIndex(xi, yi);
						printer.print(value);
					}

					printer.println();
				}
			}

		} catch (IOException e) {
			log.error("Error writing results", e);
			return 1;
		}

		log.info("Written results to {}", output);

		return 0;
	}
}
