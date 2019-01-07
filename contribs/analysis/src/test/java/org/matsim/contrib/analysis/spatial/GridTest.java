package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class GridTest {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    public void initializeSquareGrid() {

        final String testValue = "Test-value";
        Grid<String> grid = new SquareGrid<>(2, () -> testValue, createRect(10, 10));

        Collection<Grid.Cell<String>> values = grid.getValues();

        assertEquals(25, values.size());
        values.forEach(value -> assertEquals(testValue, value.getValue()));
    }

    @Test
    public void initializeHexagonalGrid() {

        final String testValue = "test-value";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, createRect(10, 10));

        Collection<Grid.Cell<String>> values = grid.getValues();

        assertEquals(38, values.size());
        values.forEach(value -> assertEquals(testValue, value.getValue()));
    }

    @Test
    public void getValue_withExactCoord() {

        final String testValue = "initialValue";
        final Coordinate expectedCoordinate = new Coordinate(2, 2.5);
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, createRect(10, 10));

        Grid.Cell<String> result = grid.getValue(expectedCoordinate);

        assertEquals(expectedCoordinate, result.getCoordinate());
    }

    @Test
    public void getValue_closeCoordinate() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, createRect(10, 10));

        // retrieve a cell somewhere near (2, 2.5)
        Grid.Cell<String> result = grid.getValue(new Coordinate(1.1, 2.3));

        assertEquals(new Coordinate(2, 2.5), result.getCoordinate());
    }

    @Test
    public void getValue_coordOutsideOfGrid() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, createRect(10, 10));

        Grid.Cell<String> result = grid.getValue(new Coordinate(100, 100));

        assertEquals(new Coordinate(9, 10), result.getCoordinate());
    }

    @Test
    public void getValues() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, createRect(10, 10));

        Collection<Grid.Cell<String>> result = grid.getValues(createRect(5, 5));

        assertEquals(9, result.size());
    }

    private Geometry createRect(double maxX, double maxY) {
        return geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0, 0), new Coordinate(maxX, 0),
                new Coordinate(maxX, maxY), new Coordinate(0, maxY),
                new Coordinate(0, 0) // close the ring
        });
    }
}
