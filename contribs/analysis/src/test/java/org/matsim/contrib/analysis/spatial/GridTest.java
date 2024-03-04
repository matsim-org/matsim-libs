package org.matsim.contrib.analysis.spatial;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

public class GridTest {

	@Test
	void initializeSquareGrid() {

        final String testValue = "Test-value";
        Grid<String> grid = new SquareGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Collection<Grid.Cell<String>> values = grid.getCells();

        // a grid with an area of 10x10 and a cell diameter of 2 should have 25 cells 10 * 10 / 2 = 25
        assertEquals(25, values.size());
        values.forEach(value -> assertEquals(testValue, value.getValue()));
    }

	@Test
	void initializeHexagonalGrid() {

        final String testValue = "test-value";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Collection<Grid.Cell<String>> values = grid.getCells();

        // a hexagonal grid with an area of 10x10 and diameter 2 will have 7 rows
        // there will be 3 rows with 5 cells and 3 rows with 4 cells
        // 3 * 5 + 3 * 4 = 27
        assertEquals(27, values.size());
        values.forEach(value -> assertEquals(testValue, value.getValue()));
    }

	@Test
	void getArea_squareGrid() {

        final double horizontalDistance = 2;
        final double expected = 2 * 2; // area of square is d^2

        Grid<String> grid = new SquareGrid<>(horizontalDistance, () -> "value", SpatialTestUtils.createRect(10, 10));

        assertEquals(expected, grid.getCellArea(), 0.0001);
    }

	@Test
	void getArea_HexagonalGrid() {

        // actually area of hexagon is r^2 * sqrt(3)/2
        final double horizontalDistance = 2;
        final double expected = Math.sqrt(3) / 2 * horizontalDistance * horizontalDistance;
        Grid<String> grid = new HexagonalGrid<>(horizontalDistance, () -> "value", SpatialTestUtils.createRect(10, 10));

        final double horizontalDistance5 = 5;
        final double expected5 = Math.sqrt(3) / 2 * horizontalDistance5 * horizontalDistance5;
        Grid<String> grid5 = new HexagonalGrid<>(horizontalDistance5, () -> "value", SpatialTestUtils.createRect(10, 10));

        assertEquals(expected, grid.getCellArea(), 0.0001);
        assertEquals(expected5, grid5.getCellArea(), 0.0001);
    }

	@Test
	void getValue_withExactCoord() {

        final String testValue = "initialValue";
        final Coordinate expectedCoordinate = new Coordinate(2, 1+Math.sqrt(3));
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Grid.Cell<String> result = grid.getCell(expectedCoordinate);

        assertEquals(expectedCoordinate, result.getCoordinate());
    }

	@Test
	void getValue_closeCoordinate() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        // retrieve a cell somewhere near (2, 2.5)
        Grid.Cell<String> result = grid.getCell(new Coordinate(2, 2.5));

        assertEquals(new Coordinate(2, 1+Math.sqrt(3)), result.getCoordinate());
    }

	@Test
	void getValue_coordOutsideOfGrid() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Grid.Cell<String> result = grid.getCell(new Coordinate(100, 100));

        assertEquals(new Coordinate(8, 1 + 5*Math.sqrt(3)), result.getCoordinate());
    }

	@Test
	void getValues() {

        final String testValue = "initialValue";
        Grid<String> grid = new HexagonalGrid<>(2, () -> testValue, SpatialTestUtils.createRect(10, 10));

        Collection<Grid.Cell<String>> result = grid.getCells(SpatialTestUtils.createRect(5, 5));

        assertEquals(8, result.size());
    }
}
