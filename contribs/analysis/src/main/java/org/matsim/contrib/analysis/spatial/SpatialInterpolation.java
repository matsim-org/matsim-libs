package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.math3.special.Erf;

import java.util.Collection;

public class SpatialInterpolation {

    private final Grid<CellValue> grid;
    private final double smoothingRadius;

    private SpatialInterpolation(double gridCellSize, GridType gridType, Geometry bounds, double smoothingRadius) {

        if (gridType == GridType.Hexagonal)
            this.grid = new HexagonalGrid<>(gridCellSize, CellValue::new, bounds);
        else
            this.grid = new SquareGrid<>(gridCellSize, CellValue::new, bounds);

        this.smoothingRadius = smoothingRadius;
    }

    public Collection<Grid.Cell<CellValue>> getCells() {

        return grid.getValues();
    }

    /**
     * This uses a gaussinan distance weighting to calculate the impact of link based emissions onto the centroid of a
     * grid cell. The level of emission is assumed to be linear over the link.
     *
     * @param from         Link from coordinate
     * @param to           Link to coordinate
     * @param cellCentroid centroid of the impacted cell
     * @return weight factor by which the emission value should be multiplied to calculate the impact of the cell
     */
    public static double calculateWeightFromLine(final Coordinate from, final Coordinate to, final Coordinate cellCentroid, final double smoothingRadius) {

        double a = from.distance(cellCentroid) * from.distance(cellCentroid);
        double b = (to.x - from.x) * (from.x - cellCentroid.x) + (to.y - from.y) * (from.y - cellCentroid.y);
        double linkLength = from.distance(to);

        double c = (smoothingRadius * Math.sqrt(Math.PI) / (linkLength * 2)) * Math.exp(-(a - (b * b / (linkLength * linkLength))) / (smoothingRadius * smoothingRadius));

        double upperLimit = linkLength + b / linkLength;
        double lowerLimit = b / linkLength;
        double integrationUpperLimit = Erf.erf(upperLimit / smoothingRadius);
        double integrationLowerLimit = Erf.erf(lowerLimit / smoothingRadius);
        double weight = c * (integrationUpperLimit - integrationLowerLimit);

        if (weight < 0)
            throw new RuntimeException("Weight may not be negative! Value: " + weight);
        return weight;
    }

    public void processPoint(final Coordinate point, final double intensityOnPoint) {

        grid.getValues().forEach(cell -> {

            double smoothingArea = Math.PI * smoothingRadius * smoothingRadius;
            double normalizationFactor = grid.getCellArea() / smoothingArea;
            double weight = calculateWeightFromPoint(point, cell.getCoordinate());

            cell.getValue().value += intensityOnPoint * weight * normalizationFactor;
        });
    }

    public void processLine(final Coordinate from, final Coordinate to, final double intensityOnLine) {

        grid.getValues().forEach(cell -> {
            double weight = calculateWeightFromLine(from, to, cell.getCoordinate(), smoothingRadius);
            cell.getValue().value += intensityOnLine * weight;
        });
    }

    /**
     * This uses an exponential distance weighting to calculate the impact of point based emissions onto the centroid of
     * a grid cell.
     *
     * @param emissionSource Centroid of the link
     * @param cellCentroid   Centroid of the impacted cell
     * @return weight factor by which the emission value should be multiplied to calculate the impact of the cell
     */
    public double calculateWeightFromPoint(final Coordinate emissionSource, final Coordinate cellCentroid) {

        double dist = emissionSource.distance(cellCentroid);
        return Math.exp((-dist * dist) / (smoothingRadius * smoothingRadius));
    }

    public enum GridType {Square, Hexagonal}

    /**
     * Grid.Cell requires a mutable data structure as value. Hence we need to wrap the weight value
     * of each cell
     */
    public static class CellValue {
        private double value = 0;

        public double getDoubleValue() {
            return value;
        }
    }

    public static class Builder {

        private GridType gridType = GridType.Square;
        private double smoothingRadius = 0;
        private double gridCellSize = 0;
        private Geometry bounds;

        public Builder() {
        }

        public Builder withGridType(GridType type) {
            this.gridType = type;
            return this;
        }

        public Builder withSmoothingRadius(double radius) {
            this.smoothingRadius = radius;
            return this;
        }

        public Builder withGridCellSize(double size) {
            gridCellSize = size;
            return this;
        }

        public Builder withBounds(Geometry bounds) {
            this.bounds = bounds;
            return this;
        }

        public SpatialInterpolation build() {

            if (isNotValid())
                throw new IllegalArgumentException("grid cell size must be > 0 and bounds must be set!");
            return new SpatialInterpolation(gridCellSize, gridType, bounds, smoothingRadius);
        }

        private boolean isNotValid() {
            return gridCellSize == 0 || bounds == null;
        }
    }
}
