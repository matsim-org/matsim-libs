package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Geometry;

import java.util.function.Supplier;

public final class HexagonalGrid<T> extends Grid<T> {

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
        return horizontalCentroidDistance * 0.75;
    }

    @Override
    double getCellArea() {
        // as in https://en.wikipedia.org/wiki/Hexagon#Parameters
        return 2 * Math.sqrt(3 * (horizontalCentroidDistance * horizontalCentroidDistance / 4));
    }
}
