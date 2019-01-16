package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Geometry;

import java.util.function.Supplier;

/**
 * Square grid which holds values
 *
 * @param <T> value of each cell
 */
public final class SquareGrid<T> extends Grid<T> {


    /**
     * New Instance of SquareGrid
     *
     * @param centroidDistance     distance between cell centroids
     * @param initialValueSupplier function to deliver a initial value when cells are created
     * @param bounds               outer bounds of the grid
     */
    public SquareGrid(final double centroidDistance, final Supplier<T> initialValueSupplier, final Geometry bounds) {
        super(centroidDistance, initialValueSupplier, bounds);
    }

    @Override
    double getMinX(double forY) {

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
        return horizontalCentroidDistance;
    }

    @Override
    public double getCellArea() {
        return horizontalCentroidDistance * horizontalCentroidDistance;
    }
}
