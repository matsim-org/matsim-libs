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

import org.matsim.api.core.v01.Coord;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * Raster holding (x,y, value) values
 * The data is stored within a double[]. The class only offers convenient methods to access this data by (x,y)-coordinates
 */
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
     * This iterates over the x and y index of the raster and supplies the corresponding value into the acceptor function
     * At the moment this iteration is done sequentially. But this may change in the future.
     *
     * @param consumer Accepts x and y index and the current value within the raster.
     */
    public void forEachIndex(IndexDoubleConsumer consumer) {
        IntStream.range(0, xLength).forEach(xi -> IntStream.range(0, yLength)
                .forEach(yi -> {
                    var value = getValueByIndex(xi, yi);
                    consumer.consume(xi, yi, value);
                }));
    }

    /**
     * This iterates over the x and y coordinates of the raster and supplies the corresponding value into the acceptor function
     * At the moment this iteration is done sequentially. But this may change in the future.
     *
     * @param consumer Accepts x and y coordinates and the current value within the raster.
     */
    public void forEachCoordinate(DoubleTriConsumer consumer) {

        IntStream.range(0, xLength).forEach(xi -> IntStream.range(0, yLength)
                .forEach(yi -> {
                    var value = getValueByIndex(xi, yi);
                    var x = xi * cellSize + bounds.minX;
                    var y = yi * cellSize + bounds.minY;
                    consumer.consume(x, y, value);
                }));
    }

    /**
     * get the bounds of the raster
     *
     * @return well, the bounds
     */
    public Bounds getBounds() {
        return this.bounds;
    }

    /**
     * Get the cell size of the raster, which determines the transformation between x,y-coodinates and index
     */
    public double getCellSize() {
        return cellSize;
    }

    /**
     * Items in x-direction
     */
    public int getXLength() {
        return xLength;
    }

    /**
     * Items in -direction
     */
    public int getYLength() {
        return yLength;
    }

    /**
     * Transform x-coordinate into index
     *
     * @param x x-value of a coordinate
     * @return x-index
     */
    public int getXIndex(double x) {
        return (int) ((x - bounds.minX) / cellSize);
    }

    /**
     * Transform y-coordinate into index
     *
     * @param y y-value of a coordinate
     * @return y-index
     */
    public int getYIndex(double y) {
        return (int) ((y - bounds.minY) / cellSize);
    }

    /**
     * Returns the value in the raster at specific grid
     * @param xi x index between 0 and {@code #getXLength} - 1
     * @param yi y index between 0 and {@code #getYLength} - 1
     * @return value in the raster
     */
    public double getValueByIndex(int xi, int yi) {
        var index = getIndex(xi, yi);
        return data[index];
    }

    /**
     * Convert index back to a coordinate.
     */
    public Coord getCoordForIndex(int xi, int yi) {
        return new Coord(xi * cellSize + bounds.minX, yi * cellSize + bounds.minY);
    }

    int getIndexForCoord(double x, double y) {
        var xi = getXIndex(x);
        var yi = getYIndex(y);

        return getIndex(xi, yi);
    }

    /**
     * This iterates over the x and y index of the raster. The iteration is done in parallel. The result of the valueSupplier
     * will be set on the corresponding pixel of the raster. This manipulates the state of the raster. Make sure to not alter
     * the state during the execution of this method from outside.
     *
     * @param valueSupplier Function which takes an x and a y index and supplies a double value which is written into
     *                      The corresponding pixel of the raster
     */
    void setValueForEachIndex(IndexToDoubleFunction valueSupplier) {

        IntStream.range(0, xLength).parallel().forEach(xi ->
                IntStream.range(0, yLength).parallel().forEach(yi -> {
                    var value = valueSupplier.applyAsDouble(xi, yi);
                    adjustValueForIndex(xi, yi, value);
                }));
    }

    int getIndex(int xi, int yi) {
        return yi * xLength + xi;
    }

    double getValueByCoord(double x, double y) {
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

    @FunctionalInterface
    public interface IndexDoubleConsumer {
        void consume(int xi, int yi, double value);
    }

    @FunctionalInterface
    public interface DoubleTriConsumer {
        void consume(double x, double y, double value);
    }

    @FunctionalInterface
    public interface IndexToDoubleFunction {
        double applyAsDouble(int xi, int yi);
    }

    public static class Bounds {
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
}
