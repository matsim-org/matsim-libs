package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.*;

public class SpatialInterpolationTest {

    private static final String EMISSION_KEY = "emmission-value";

    private static Network createRandomNetworkWithRandomEmissionValue(int numberOfLinks, double maxX, double maxY) {

        Network network = NetworkUtils.createNetwork();

        for (long i = 0; i < numberOfLinks; i++) {

            Link link = SpatialTestUtils.createRandomLink(network.getFactory(), maxX, maxY);
            network.addNode(link.getFromNode());
            network.addNode(link.getToNode());
            network.addLink(link);
            link.getAttributes().putAttribute(EMISSION_KEY, Math.random());
        }
        return network;
    }

    private static Collection<Grid.Cell<Double>> createRandomPointsWithValues(int numberOfPoints, double maxX, double maxY) {

        List<Grid.Cell<Double>> cells = new ArrayList<>();

        for (int i = 0; i < numberOfPoints; i++) {

            Coordinate coordinate = new Coordinate(SpatialTestUtils.getRandomValue(maxX), SpatialTestUtils.getRandomValue(maxY));
            Grid.Cell<Double> cell = new Grid.Cell<>(coordinate, SpatialTestUtils.getRandomValue(1));
            cells.add(cell);
        }

        return cells;
    }

    private static Coordinate transformToCoodinate(Coord coord) {
        return new Coordinate(coord.getX(), coord.getY(), coord.getY());
    }

    @Test
    public void initialize() {

        SpatialInterpolation result = new SpatialInterpolation.Builder()
                .withBounds(SpatialTestUtils.createRect(1000, 1000))
                .withGridCellSize(200)
                .withGridType(SpatialInterpolation.GridType.Hexagonal)
                .withSmoothingRadius(500)
                .build();

        assertNotNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialize_noGridCellSize() {

        new SpatialInterpolation.Builder()
                .withBounds(SpatialTestUtils.createRect(1000, 1000))
                .withGridType(SpatialInterpolation.GridType.Hexagonal)
                .withSmoothingRadius(500)
                .build();

        fail("missing parameters should cause exception");
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialize_noBounds() {

        new SpatialInterpolation.Builder()
                .withGridCellSize(200)
                .withGridType(SpatialInterpolation.GridType.Hexagonal)
                .withSmoothingRadius(500)
                .build();

        fail("missing parameters should cause exception");
    }

    @Test
    public void processLine() {

        final int numberOfLinks = 1000;
        final double maxX = 1000;
        final double maxY = 1000;
        Network network = createRandomNetworkWithRandomEmissionValue(numberOfLinks, maxX, maxY);

        SpatialInterpolation interpolation = new SpatialInterpolation.Builder()
                .withBounds(SpatialTestUtils.createRect(1000, 1000))
                .withGridCellSize(200)
                .withGridType(SpatialInterpolation.GridType.Square)
                .withSmoothingRadius(500)
                .build();

        network.getLinks().values().forEach(link -> interpolation.processLine(
                transformToCoodinate(link.getFromNode().getCoord()),
                transformToCoodinate(link.getToNode().getCoord()),
                (double) link.getAttributes().getAttribute(EMISSION_KEY)
        ));

        Collection<Grid.Cell<SpatialInterpolation.CellValue>> cells = interpolation.getCells();
        cells.forEach(cell -> assertTrue(cell.getValue().getDoubleValue() > 0));
    }

    @Test
    public void processPoint() {

        final int numberOfPoints = 1000;
        final double maxX = 1000;
        final double maxY = 1000;

        Collection<Grid.Cell<Double>> points = createRandomPointsWithValues(numberOfPoints, maxX, maxY);

        SpatialInterpolation interpolation = new SpatialInterpolation.Builder()
                .withBounds(SpatialTestUtils.createRect(1000, 1000))
                .withGridCellSize(200)
                .withGridType(SpatialInterpolation.GridType.Square)
                .withSmoothingRadius(500)
                .build();

        points.forEach(point -> interpolation.processPoint(point.getCoordinate(), point.getValue()));

        Collection<Grid.Cell<SpatialInterpolation.CellValue>> cells = interpolation.getCells();
        cells.forEach(cell -> assertTrue(cell.getValue().getDoubleValue() > 0));
    }
}
