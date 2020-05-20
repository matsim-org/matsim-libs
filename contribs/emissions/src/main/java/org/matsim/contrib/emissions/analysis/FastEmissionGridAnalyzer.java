package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.Map;
import java.util.stream.Collectors;

public class FastEmissionGridAnalyzer {


    static Raster calculate(Network network, Map<Id<Link>, Double> emissions, double cellSize) {


        // create a kernel
        // this is a kernel for sigma = 1. TODO calculate kernel based on smoothing radius
        var kernel = new double[]{0.006, 0.061, 0.242, 0.383, 0.242, 0.061, 0.006};
        var originalRaster = rasterNetwork(network, emissions, cellSize);
        var firstPassRaster = new Raster(originalRaster.getBounds(), cellSize);
        var finalPassRaster = new Raster(originalRaster.getBounds(), cellSize);

        // smooth horizontally
    /*    for (var x = bounds.minX; x < bounds.maxX; x += cellSize) {
            for (var y = bounds.minY; y < bounds.maxY; y += cellSize) {

                // TODO creating a new coord each time is expensice I guess
                var pollution = pollutionCells.computeIfAbsent(new Coord(x, y), c -> 0.);

                var averageValue = 0.;
                // get the values from the cells within the kernel
                for (var ki = -3; ki <= 3; ki++) {

                    var kernelValue = kernel[ki + 3];
                    var otherPollution = pollutionCells.computeIfAbsent(new Coord(x + cellSize * ki, y), c -> 0.);
                    averageValue += otherPollution;
                }

                averageValue = averageValue / kernel.length;

            }
        }
                // if cells not already present create one
                // calculate the value for the cell

     */

        return finalPassRaster;
    }

    static Raster rasterNetwork(Network network, Map<Id<Link>, Double> emissions, double cellSize) {

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
            var numberOfCells = rasterizeLink(link, cellSize, 0, raster);
            // second pass for actually writing the emission values
            rasterizeLink(link, cellSize, value / numberOfCells, raster);
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
    private static int rasterizeLink(Link link, double cellSize, double value, Raster raster) {

        int x0 = (int) (link.getFromNode().getCoord().getX() / cellSize);
        int x1 = (int) (link.getToNode().getCoord().getX() / cellSize);
        int y0 = (int) (link.getFromNode().getCoord().getY() / cellSize);
        int y1 = (int) (link.getToNode().getCoord().getY() / cellSize);
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int err = dx + dy, e2;

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;

        int result = 0;

        if (dx == 0 && dy == 0) {
            // the algorithm doesn't really support lines shorter than the cell size.
            // do avoid complicated computation within the loop, catch this case here
            raster.adjustValue(x0 * cellSize, y0 * cellSize, value);
            return 1;
        }

        do {
            raster.adjustValue(x0 * cellSize, y0 * cellSize, value);
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
        } while (x0 != x1 || y0 != y1);

        return result;
    }
}
