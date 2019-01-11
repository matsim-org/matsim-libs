package org.matsim.contrib.analysis.spatial;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.core.utils.collections.QuadTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Abstract class for (x,y,value) quadTree
 *
 * @param <T>
 */
abstract class Grid<T> {

    final double horizontalCentroidDistance;
    QuadTree<Cell<T>> quadTree;

    Grid(final double horizontalCentroidDistance, final Supplier<T> initialValueSupplier, final Geometry bounds) {
        this.horizontalCentroidDistance = horizontalCentroidDistance;
        generateGrid(initialValueSupplier, bounds);
    }

    public Cell<T> getValue(Coordinate coordinate) {

        return quadTree.getClosest(coordinate.x, coordinate.y);
    }

    public Collection<Cell<T>> getValues(Geometry bounds) {

        return quadTree.getRectangle(
                bounds.getEnvelopeInternal().getMinX(), bounds.getEnvelopeInternal().getMinY(),
                bounds.getEnvelopeInternal().getMaxX(), bounds.getEnvelopeInternal().getMaxY(),
                new ArrayList<>()
        );
    }

    public Collection<Cell<T>> getValues() {
        return quadTree.values();
    }

    abstract double getMinX(double forY);

    abstract double getMinY();

    abstract double getCentroidDistanceX();

    abstract double getCentroidDistanceY();

    abstract double getCellArea();

    private void generateGrid(Supplier<T> initialValueSupplier, final Geometry bounds) {

        Envelope envelope = bounds.getEnvelopeInternal();

        quadTree = new QuadTree<>(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
        generateAllRows(initialValueSupplier);
    }

    private void generateAllRows(final Supplier<T> initialValueSupplier) {

        for (double y = getMinY(); y <= quadTree.getMaxNorthing(); y += getCentroidDistanceY()) {
            generateRow(y, initialValueSupplier);
        }
    }

    private void generateRow(final double y, final Supplier<T> initialValueSupplier) {

        for (double x = getMinX(y); x <= quadTree.getMaxEasting(); x += getCentroidDistanceX()) {
            Coordinate coord = new Coordinate(x, y);
            quadTree.put(x, y, new Cell<>(coord, initialValueSupplier.get()));
        }
    }

    public static class Cell<T> {

        private Coordinate coordinate;
        private T value;

        Cell(Coordinate coordinate, T value) {
            this.coordinate = coordinate;
            this.value = value;
        }

        public Coordinate getCoordinate() {
            return coordinate;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }
}
