/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.emissions.analysis;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class provides functions for blurring emissions. It does the same thing as {@link EmissionGridAnalyzer} but is much faster
 * The most convenient way to use it is the {@link FastEmissionGridAnalyzer#processEventsFile(String, Network, double, int)} method. If an emissions
 * Events file is already parsed, one may also use the {@link FastEmissionGridAnalyzer#processLinkEmissions(Map, Network, double, int)} method
 */
public abstract class FastEmissionGridAnalyzer {

	private static final Logger logger = LogManager.getLogger(FastEmissionGridAnalyzer.class);

	/**
	 * Processes an events file with emissions and renders emissions in three steps:
	 * <p>
	 * 1. All emissions are summed up by link id if the link id was found within the supplied network.
	 * 2. The aggregated emissions for each link are rastered onto all the raster-cells covered by the link.
	 * 3. In the smoothing step the emissions are blurred onto the surrounding raster-cells.
	 * <p>
	 * The blurring algorithm is a gaussian blur <a href="https://en.wikipedia.org/wiki/Gaussian_blur">...</a>
	 * <p>
	 * If only a certain area of the scenario is of interest for the analysis. The supplied network must be filtered beforehand.
	 * The resulting raster's size depends on the bounding box of the supplied network.
	 * <p>
	 * Note: The algorithm is not accurate at the edges of the raster. The kernel is cut of the edges meaning that emissions
	 * are underestimated at the edges of the raster. I didn't bother to implement this correctly. Otherwise, the overall
	 * amount of emissions doesn't change.
	 *
	 * @param eventsFile The events file which contains the emission events
	 * @param network    The network those emissions occurred on. The size of the resulting rater depends on the bounding box of the network
	 * @param cellSize   size of a cell. This determines how many 'pixels' the resulting raster will have. Smaller cellSize means
	 *                   higher pixel-density
	 * @param radius     smoothing radius which determines the strength of the blur. The radius describes onto how many cells
	 *                   the emissions of a single cell are distributed in one direction. A radius of 0 means no blurring. A radius of ~20
	 *                   is probably a good guess for real-world scenarios.
	 *                   The resulting smoothing kernel will have radius * 2 + 1 entries.
	 * @return A raster containing emission values for each (x,y)-cell within the bounds of the network
	 */
	public static Map<Pollutant, Raster> processEventsFile(final String eventsFile, final Network network, final double cellSize, final int radius) {

		logger.info("Start parsing events file.");

		Map<Pollutant, TObjectDoubleHashMap<Id<Link>>> linkEmissionsByPollutant = new HashMap<>();

		new RawEmissionEventsReader((time, linkId, vehicleId, pollutant, value) -> {

			var id = Id.createLinkId(linkId);
			if (network.getLinks().containsKey(id)) {

				var linkMap = linkEmissionsByPollutant.computeIfAbsent(pollutant, key -> new TObjectDoubleHashMap<>());
				linkMap.adjustOrPutValue(id, value, value);
			}
		}).readFile(eventsFile);

		logger.info("Start smoothing pollution.");
		return linkEmissionsByPollutant.entrySet().stream()
			.map(entry -> {
				logger.info("Smoothing of: {}", entry.getKey());
				return Tuple.of(entry.getKey(), processLinkEmissions(entry.getValue(), network, cellSize, radius));
			})
			.collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
	}


	/**
	 * Processes emissions that have been read by the {@link EmissionsOnLinkEventHandler}.
	 */
	public static Map<Pollutant, Raster> processHandlerEmissions(Map<Id<Link>, Map<Pollutant, Double>> link2pollutants, Network network, double cellSize, int radius) {

		Map<Pollutant, TObjectDoubleHashMap<Id<Link>>> linkEmissionsByPollutant = new HashMap<>();

		// Transpose the map
		for (Map.Entry<Id<Link>, Map<Pollutant, Double>> perLink : link2pollutants.entrySet()) {
			for (Map.Entry<Pollutant, Double> e : perLink.getValue().entrySet()) {
				var linkMap = linkEmissionsByPollutant.computeIfAbsent(e.getKey(), key -> new TObjectDoubleHashMap<>());
				linkMap.put(perLink.getKey(), e.getValue());
			}
		}

		return linkEmissionsByPollutant.entrySet().stream()
			.map(entry -> {
				logger.info("Smoothing of: {}", entry.getKey());
				return Tuple.of(entry.getKey(), processLinkEmissions(entry.getValue(), network, cellSize, radius));
			})
			.collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
	}

	/**
	 * Processes emissions that have been read by the {@link EmissionsOnLinkEventHandler}.
	 */
	public static TimeBinMap<Map<Pollutant, Raster>> processHandlerEmissionsPerTimeBin(TimeBinMap<Map<Id<Link>, EmissionsByPollutant>> timeBinMap, Network network, double cellSize, int radius) {

		Map<Double, Map<Pollutant, TObjectDoubleHashMap<Id<Link>>>> linkEmissionsByPollutantAndTime = new HashMap<>();

		for (TimeBinMap.TimeBin<Map<Id<Link>, EmissionsByPollutant>> perLink : timeBinMap.getTimeBins()) {
			Double time = perLink.getStartTime();
			Map<Pollutant, TObjectDoubleHashMap<Id<Link>>> emissions = new HashMap<>();

			for (Map.Entry<Id<Link>, EmissionsByPollutant> emissionsByPollutantEntry : perLink.getValue().entrySet()) {
				// Added linkID if not exists
				Id<Link> linkId = emissionsByPollutantEntry.getKey();
				for (Map.Entry<Pollutant, Double> e : emissionsByPollutantEntry.getValue().getEmissions().entrySet()) {

					if (!emissions.containsKey(e.getKey()))
						emissions.put(e.getKey(), new TObjectDoubleHashMap<>());

					emissions.get(e.getKey()).put(linkId, e.getValue());
				}
			}

			linkEmissionsByPollutantAndTime.put(time, emissions);
		}

		TimeBinMap<Map<Pollutant, Raster>> result = new TimeBinMap<>(timeBinMap.getBinSize());

		// Transpose the map
		for (Map.Entry<Double, Map<Pollutant, TObjectDoubleHashMap<Id<Link>>>> timeSlice : linkEmissionsByPollutantAndTime.entrySet()) {
			Map<Pollutant, TObjectDoubleHashMap<Id<Link>>> pollutants = timeSlice.getValue();
			Map<Pollutant, Raster> rasterMap = new HashMap<>();

			for (Map.Entry<Pollutant, TObjectDoubleHashMap<Id<Link>>> e : pollutants.entrySet()) {

				Pollutant pollutant = e.getKey();
				TObjectDoubleHashMap<Id<Link>> emissions = e.getValue();

				Raster raster = processLinkEmissions(emissions, network, cellSize, radius);
				rasterMap.put(pollutant, raster);
			}

			result.getTimeBin(timeSlice.getKey()).setValue(rasterMap);
		}

		return result;
	}

	/**
	 * Works as {@link FastEmissionGridAnalyzer#processEventsFile(String, Network, double, int)} but without events parsing
	 * The emissions per link have to be supplied.
	 */
	public static Raster processLinkEmissions(final TObjectDoubleMap<Id<Link>> emissions, final Network network, final double cellSize, final int radius) {

		var originalRaster = rasterizeNetwork(network, emissions, cellSize);
		return blur(originalRaster, radius);
	}

	/**
	 * Works as {@link FastEmissionGridAnalyzer#processEventsFile(String, Network, double, int)} but without events parsing
	 * The emissions per link have to be supplied.
	 */
	public static Raster processLinkEmissions(final Map<Id<Link>, Double> emissions, final Network network, final double cellSize, final int radius) {

		var originalRaster = rasterizeNetwork(network, emissions, cellSize);
		return blur(originalRaster, radius);
	}

	static Raster blur(Raster raster, int radius) {

		logger.info("Creating Kernel with {} taps", radius * 2 + 1);
		var kernel = createKernel(radius * 2 + 1);

		var result = new Raster(raster.getBounds(), raster.getCellSize());

		var firstPassRaster = new Raster(raster.getBounds(), raster.getCellSize());

		// smooth horizontally
		firstPassRaster.setValueForEachIndex((x, y) ->
			calculateBlurredValue(y, x, firstPassRaster.getXLength(), kernel, (yf, xv) -> raster.getValueByIndex(xv, yf))
		);

		// smooth vertically
		result.setValueForEachIndex((x, y) ->
			calculateBlurredValue(x, y, result.getYLength(), kernel, firstPassRaster::getValueByIndex)
		);

		return result;
	}

	private static double calculateBlurredValue(int fixedIndex, int volatileIndex, int volatileLength, double[] kernel, GetValue getValue) {

		var halfKernelLength = kernel.length / 2;
		var value = 0.;
		var startIndex = (volatileIndex - halfKernelLength < 0) ? halfKernelLength - volatileIndex : 0;
		var endIndex = (volatileIndex + halfKernelLength >= volatileLength) ? volatileLength - 1 - volatileIndex + halfKernelLength : kernel.length;

		for (var ki = startIndex; ki < endIndex; ki++) {
			var kernelValue = kernel[ki];
			var originalValue = getValue.forIndex(fixedIndex, volatileIndex + ki - halfKernelLength);
			value += originalValue * kernelValue;
		}
		return value;
	}

	static Raster rasterizeNetwork(final Network network, final TObjectDoubleMap<Id<Link>> emissions, final double cellSize) {

		var coords = network.getNodes().values().stream()
			.map(BasicLocation::getCoord)
			.collect(Collectors.toSet());

		var bounds = new Raster.Bounds(coords);
		var raster = new Raster(bounds, cellSize);
		var cellArea = cellSize * cellSize; // assume square cells at the moment

		emissions.forEachEntry((linkId, value) -> {
			var link = network.getLinks().get(linkId);
			// If the link does not exist in the network, we ignore it
			if (link != null) {
				var numberOfCells = rasterizeLink(link, 0, raster);
				rasterizeLink(link, value / numberOfCells / cellArea, raster);
			}
			return true;
		});
		return raster;
	}

	static Raster rasterizeNetwork(Network network, Map<Id<Link>, Double> emissions, double cellSize) {

		var coords = network.getNodes().values().stream()
			.map(BasicLocation::getCoord)
			.collect(Collectors.toSet());

		var bounds = new Raster.Bounds(coords);
		var raster = new Raster(bounds, cellSize);
		var cellArea = cellSize * cellSize; // assume square cells at the moment

		// rasterize network
		for (var entry : emissions.entrySet()) {

			var link = network.getLinks().get(entry.getKey());
			// If the link does not exist in the network, we ignore it
			if (link != null) {
				var value = entry.getValue();
				// first count number of cells
				var numberOfCells = rasterizeLink(link, 0, raster);
				// second pass for actually writing the emission values
				rasterizeLink(link, value / numberOfCells / cellArea, raster);
			}
		}
		return raster;
	}

	/**
	 * Rasterizes links into squares. Uses Bresenham's line drawing algorithm, which is supposed to be fast
	 * Maybe the result is too chunky, but it'll do as a first try
	 *
	 * @param link MATSim network link
	 * @return number of cells the link is rastered to
	 */
	private static int rasterizeLink(Link link, double value, Raster raster) {


		int x0 = raster.getXIndex(link.getFromNode().getCoord().getX());
		int x1 = raster.getXIndex(link.getToNode().getCoord().getX());
		int y0 = raster.getYIndex(link.getFromNode().getCoord().getY());
		int y1 = raster.getYIndex(link.getToNode().getCoord().getY());
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int err = dx + dy, e2;

		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;

		int result = 0;

		if (dx == 0 && dy == 0) {
			// the algorithm doesn't really support lines shorter than the cell size.
			// do avoid complicated computation within the loop, catch this case here
			raster.adjustValueForIndex(x0, y0, value);
			return 1;
		}

		do {
			raster.adjustValueForIndex(x0, y0, value);
			result++;

			e2 = err + err;
			if (e2 >= dy) {
				err += dy;
				x0 += sx;
			}
			if (e2 <= dx) {
				err += dx;
				y0 += sy;
			}
			// have this condition in separate method because we want to get one more cell than the original algorithm
			// but then the direction of the line requires different conditions
		} while (keepRasterizing(x0, x1, sx) && keepRasterizing(y0, y1, sy));

		return result;
	}

	private static boolean keepRasterizing(int value, int endCondition, int direction) {

		if (direction > 0) return value <= endCondition;
		else return value >= endCondition;
	}

	/**
	 * It might make sense to cut the edges of the distribution if we have a larger number of taps
	 *
	 * @param taps Length of the kernel
	 * @return Gaussian Kernel
	 */
	private static double[] createKernel(int taps) {

		var result = new double[taps];
		var binomialIndex = taps - 1;
		var sum = Math.pow(2, binomialIndex);

		for (var i = 0; i < taps; i++) {
			var coefficient = CombinatoricsUtils.binomialCoefficient(binomialIndex, i);
			result[i] = coefficient / sum;
		}
		return result;
	}

	@FunctionalInterface
	private interface GetValue {
		double forIndex(int fixedIndex, int volatileIndex);
	}
}
