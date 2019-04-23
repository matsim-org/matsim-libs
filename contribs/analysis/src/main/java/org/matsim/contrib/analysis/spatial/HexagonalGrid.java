package org.matsim.contrib.analysis.spatial;

import java.util.function.Supplier;

import org.locationtech.jts.geom.Geometry;

/**
 * Hexagonal Grid which holds values
 *
 * @param <T> value of each cell
 */
public final class HexagonalGrid<T> extends Grid<T> {

    /**
     * New Instance of HexagonalGrid
     *
     * @param horizontalCentroidDistance horizontal distance between cell centroids
     * @param initialValueSupplier       function to deliver a initial value when cells are created
     * @param bounds                     outer bounds of the grid
     */
    public HexagonalGrid(double horizontalCentroidDistance, Supplier<T> initialValueSupplier, Geometry bounds) {
        super(horizontalCentroidDistance, initialValueSupplier, bounds);
    }

    @Override
    double getMinX(double forY) {

        long factor = Math.round((forY - quadTree.getMinNorthing()) / getCentroidDistanceY());
        if ((factor % 2) == 0)
            return quadTree.getMinEasting();
        else
            return quadTree.getMinEasting() + horizontalCentroidDistance / 2;
    }

    @Override
    double getMinY() {
        return quadTree.getMinNorthing() + horizontalCentroidDistance / 2;
    }

    @Override
    double getCentroidDistanceX() {
        return horizontalCentroidDistance;
    }

    @Override
    double getCentroidDistanceY() {
        return horizontalCentroidDistance * 0.8660254;  // sin(30deg)
    }

    @Override
    public double getCellArea() {
        // as in https://en.wikipedia.org/wiki/Hexagon#Parameters
        return 2 * Math.sqrt(3 * (horizontalCentroidDistance * horizontalCentroidDistance / 4));
    }
}
