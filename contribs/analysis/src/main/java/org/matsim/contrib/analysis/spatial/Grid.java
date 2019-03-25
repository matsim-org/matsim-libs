package org.matsim.contrib.analysis.spatial;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.core.utils.collections.QuadTree;

/**
 * Abstract class for regular Grids
 *
 * @param <T> Value of each grid cell
 */
public abstract class Grid<T> {

    private static final GeometryFactory geometryFactory = new GeometryFactory();
    final double horizontalCentroidDistance;
    QuadTree<Cell<T>> quadTree;

    public Grid(final double horizontalCentroidDistance, final Supplier<T> initialValueSupplier, final Geometry bounds) {
        this.horizontalCentroidDistance = horizontalCentroidDistance;
        generateGrid(initialValueSupplier, bounds);
    }

    /**
     * retrieve cell for a given coordinate. Cell with closest centroid will be returne
     *
     * @param coordinate coordinate within a cell
     * @return Cell with closest centroid.
     */
    public Cell<T> getCell(Coordinate coordinate) {

        return quadTree.getClosest(coordinate.x, coordinate.y);
    }

    /**
     * retrieve cells within bounds
     *
     * @param bounds bounds
     * @return all cells which have their centroid withing the given bounds
     */
    public Collection<Cell<T>> getCells(Geometry bounds) {

        return quadTree.getRectangle(
                bounds.getEnvelopeInternal().getMinX(), bounds.getEnvelopeInternal().getMinY(),
                bounds.getEnvelopeInternal().getMaxX(), bounds.getEnvelopeInternal().getMaxY(),
                new ArrayList<>()
        );
    }

    /**
     * retrieve all cells
     * @return all cells of the grid
     */
    public Collection<Cell<T>> getCells() {
        return quadTree.values();
    }

    /**
     * Returns the x-value for the first centroid of each line for a given y
     * @param forY y-value to calculate x for
     * @return x-value
     */
    abstract double getMinX(double forY);

    /**
     * Returns the y-value for the first centroid of each column
     * @return y-value
     */
    abstract double getMinY();

    /**
     * @return horizontal distance between each centroid
     */
    abstract double getCentroidDistanceX();

    /**
     * @return vertical distance between each centroid
     */
    abstract double getCentroidDistanceY();

    /**
     * @return area of one cell (assumes that all cells have the same area)
     */
    public abstract double getCellArea();

    private void generateGrid(Supplier<T> initialValueSupplier, final Geometry bounds) {

        Envelope envelope = bounds.getEnvelopeInternal();

        quadTree = new QuadTree<>(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
        generateAllRows(initialValueSupplier, bounds);
    }

    private void generateAllRows(final Supplier<T> initialValueSupplier, final Geometry bounds) {

        for (double y = getMinY(); y <= quadTree.getMaxNorthing(); y += getCentroidDistanceY()) {
            generateRow(y, initialValueSupplier, bounds);
        }
    }

    private void generateRow(final double y, final Supplier<T> initialValueSupplier, final Geometry bounds) {

        for (double x = getMinX(y); x <= quadTree.getMaxEasting(); x += getCentroidDistanceX()) {
            Coordinate coord = new Coordinate(x, y);
            if (bounds.contains(geometryFactory.createPoint(coord)))
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
