package org.matsim.contrib.emissions.analysis;

import org.matsim.api.core.v01.Coord;

import java.util.Set;
import java.util.stream.IntStream;

public class Raster {

    private final Bounds bounds;
    private final double cellSize;
    private final double[] data;
    private final int xLength;
    private final int yLength;

    Raster(Bounds bounds, double cellSize) {

        this.bounds = bounds;
        this.cellSize = cellSize;
        this.xLength = getXIndex(bounds.maxX) + 1;
        this.yLength = getYIndex(bounds.maxY) + 1;
        this.data = new double[xLength * yLength];
    }

    /**
     * This iterates over the x and y index of the raster. The iteration is done in parallel. The result of the valueSupplier
     * will be set on the corresponding pixel of the raster. This manipulates the state of the raster. Make sure to not alter
     * the state during the execution of this method from outside.
     *
     * @param valueSupplier Function which takes an x and a y index and supplies a double value which is written into
     *                      The corresponding pixel of the raster
     */
    void setValueForEachIndex(IndexSupplier valueSupplier) {

        IntStream.range(0, xLength).parallel().forEach(xi ->
                IntStream.range(0, yLength).parallel().forEach(yi -> {
                    var value = valueSupplier.supply(xi, yi);
                    adjustValueForIndex(xi, yi, value);
                }));
    }

    /**
     * This iterates over the x and y index of the raster and supplies the corresponding value into the acceptor function
     * At the moment this iteration is done sequentially. But this may change in the future.
     *
     * @param acceptor Accepts x and y index and the current value within the raster.
     */
    void forEachIndex(IndexAcceptor acceptor) {
        IntStream.range(0, xLength).forEach(xi -> IntStream.range(0, yLength)
                .forEach(yi -> {
                    var value = getValueByIndex(xi, yi);
                    acceptor.accept(xi, yi, value);
                }));
    }

    Bounds getBounds() {
        return this.bounds;
    }

    double getCellSize() {
        return cellSize;
    }

    int getXLength() {
        return xLength;
    }

    int getYLength() {
        return yLength;
    }

    int getXIndex(double x) {
        return (int) ((x - bounds.minX) / cellSize);
    }

    int getYIndex(double y) {
        return (int) ((y - bounds.minY) / cellSize);
    }

    int getIndexForCoord(double x, double y) {
        var xi = getXIndex(x);
        var yi = getYIndex(y);

        return getIndex(xi, yi);
    }

    int getIndex(int xi, int yi) {
        return yi * xLength + xi;
    }

    double getValueByIndex(int xi, int yi) {

        var index = getIndex(xi, yi);
        return data[index];
    }

    double getValue(double x, double y) {
        var index = getIndexForCoord(x, y);
        return data[index];
    }

    double adjustValueForCoord(double x, double y, double value) {

        var index = getIndexForCoord(x, y);
        return data[index] += value;
    }

    double adjustValueForIndex(int xi, int yi, double value) {
        var index = getIndex(xi, yi);
        return data[index] += value;
    }

    static class Bounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;

        Bounds(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        Bounds(Set<Coord> coords) {
            for (Coord coord : coords) {
                if (coord.getX() < minX) minX = coord.getX();
                if (coord.getY() < minY) minY = coord.getY();
                if (coord.getX() > maxX) maxX = coord.getX();
                if (coord.getY() > maxY) maxY = coord.getY();
            }
        }

        public double getMinX() {
            return minX;
        }

        public double getMinY() {
            return minY;
        }

        public double getMaxX() {
            return maxX;
        }

        public double getMaxY() {
            return maxY;
        }
    }

    @FunctionalInterface
    interface IndexSupplier {

        double supply(int xi, int yi);
    }

    @FunctionalInterface
    interface IndexAcceptor {
        void accept(int xi, int yi, double value);
    }
}
