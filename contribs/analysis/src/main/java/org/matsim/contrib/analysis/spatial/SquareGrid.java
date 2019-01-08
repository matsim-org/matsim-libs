package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Geometry;

import java.util.function.Supplier;

public class SquareGrid<T> extends Grid<T> {


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
    double getCellArea() {
        return horizontalCentroidDistance * horizontalCentroidDistance;
    }
}
