package org.matsim.contrib.analysis.spatial;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class GridTest {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    @Test
    public void initializeSquareGrid() {

        final String testValue = "Test-value";
        Grid<String> grid = new SquareGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Collection<Grid.Cell<String>> values = grid.getCells();

        // a grid with an area of 10x10 and a cell diameter of 2 should have 25 cells 10 * 10 / 2 = 25
        assertEquals(25, values.size());
        values.forEach(value -> assertEquals(testValue, value.getValue()));
    }

    @Test
    public void initializeHexagonalGrid() {

        final String testValue = "test-value";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Collection<Grid.Cell<String>> values = grid.getCells();

        // a hexagonal grid with an area of 10x10 and diameter 2 will have 7 rows
        // there will be 4 rows with 5 cells and 3 rows with 5 cells
        // 4 * 5 + 3 * 6 = 38
        assertEquals(38, values.size());
        values.forEach(value -> assertEquals(testValue, value.getValue()));
    }

    @Test
    public void getArea_squareGrid() {

        final double horizontalDistance = 2;
        final double expected = 2 * 2; // area of square is d^2

        Grid<String> grid = new SquareGrid<>(horizontalDistance, () -> "value", SpatialTestUtils.createRect(10, 10));

        assertEquals(expected, grid.getCellArea(), 0.0001);
    }

    @Test
    public void getArea_HexagonalGrid() {

        final double horizontalDistance = 2;
        // area of hexagon is: 2 * sqrt(3*r^2)
        final double expected = 2 * Math.sqrt(3 * horizontalDistance * horizontalDistance / 4);

        Grid<String> grid = new HexagonalGrid<>(horizontalDistance, () -> "value", SpatialTestUtils.createRect(10, 10));

        assertEquals(expected, grid.getCellArea(), 0.0001);
    }

    @Test
    public void getValue_withExactCoord() {

        final String testValue = "initialValue";
        final Coordinate expectedCoordinate = new Coordinate(2, 2.5);
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Grid.Cell<String> result = grid.getCell(expectedCoordinate);

        assertEquals(expectedCoordinate, result.getCoordinate());
    }

    @Test
    public void getValue_closeCoordinate() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        // retrieve a cell somewhere near (2, 2.5)
        Grid.Cell<String> result = grid.getCell(new Coordinate(1.1, 2.3));

        assertEquals(new Coordinate(2, 2.5), result.getCoordinate());
    }

    @Test
    public void getValue_coordOutsideOfGrid() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Grid.Cell<String> result = grid.getCell(new Coordinate(100, 100));

        assertEquals(new Coordinate(9, 10), result.getCoordinate());
    }

    @Test
    public void getValues() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Collection<Grid.Cell<String>> result = grid.getCells(SpatialTestUtils.createRect(5, 5));

        assertEquals(9, result.size());
    }
}
