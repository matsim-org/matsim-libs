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

    void forEachIndex(IndexFunction valueSupplier) {

        IntStream.range(0, xLength)
                .forEach(xi ->
                        IntStream.range(0, yLength)
                                .forEach(yi -> {
                                    var value = valueSupplier.supply(xi, yi);
                                    var index = yi * xLength + xi;
                                    data[index] = value;
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
    interface IndexFunction {

        double supply(int xi, int yi);
    }
}
