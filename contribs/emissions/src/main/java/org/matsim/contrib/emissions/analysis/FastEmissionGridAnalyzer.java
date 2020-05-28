package org.matsim.contrib.emissions.analysis;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FastEmissionGridAnalyzer {

    private static final Logger logger = Logger.getLogger(FastEmissionGridAnalyzer.class);

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
                    logger.info("Smoothing of: " + entry.getKey());
                    return Tuple.of(entry.getKey(), processLinkEmissions(entry.getValue(), network, cellSize, radius));
                })
                .collect(Collectors.toMap(Tuple::getFirst, Tuple::getSecond));
    }

    public static Raster processLinkEmissions(final TObjectDoubleMap<Id<Link>> emissions, final Network network, final double cellSize, final int radius) {

        var originalRaster = rasterizeNetwork(network, emissions, cellSize);
        return blur(originalRaster, radius);
    }

    public static Raster processLinkEmissions(final Map<Id<Link>, Double> emissions, final Network network, final double cellSize, final int radius) {

        var originalRaster = rasterizeNetwork(network, emissions, cellSize);
        return blur(originalRaster, radius);
    }

    static Raster blur(Raster raster, int radius) {

        logger.info("Creating Kernel with " + (radius * 2 + 1) + " taps");
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

        emissions.forEachEntry((linkId, value) -> {
            var link = network.getLinks().get(linkId);
            var numberOfCells = rasterizeLink(link, 0, raster);
            rasterizeLink(link, value / numberOfCells, raster);
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

        // rasterize network
        for (var entry : emissions.entrySet()) {

            var link = network.getLinks().get(entry.getKey());
            var value = entry.getValue();
            // first count number of cells
            var numberOfCells = rasterizeLink(link, 0, raster);
            // second pass for actually writing the emission values
            rasterizeLink(link, value / numberOfCells, raster);
        }
        return raster;
    }

    /**
     * Rasterizes links into squares. Uses Bresenham's line drawing algorithm, which is supposed to be fast
     * Maybe the result is too chunky, but it'll do as a first try
     *
     * @param link Matsim network link
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
            try {
                raster.adjustValueForIndex(x0, y0, value);
            } catch (Exception e) {
                logger.error(e);
            }
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
        } while (x0 <= x1 && y0 <= y1);

        return result;
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
