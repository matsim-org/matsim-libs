package org.matsim.contrib.emissions.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;

import java.util.*;
import java.util.stream.Collectors;

public class EmissionRaster {

    private final Map<Id<Link>, List<Cell>> linkMap = new HashMap<>();
    private final Map<Coord, Cell> coordMap = new HashMap<>();

    private QuadTree<Cell> spatialIndex;
    private int cellSize;
    private Map<Coord, Cell> cells = new HashMap<>();
    private double minX = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxY = Double.MIN_VALUE;

    public EmissionRaster(int cellSize, Network network) {
        this.cellSize = cellSize;
        Map<Coord, Cell> cells = rasterizeNetwork(network);

        // create a spatial index of cells so the raster can be queried later
        double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        spatialIndex = new QuadTree<>(bounds[0] - cellSize, bounds[1] - cellSize, bounds[2] + cellSize, bounds[3] + cellSize); //ugh
        for (Map.Entry<Coord, Cell> entry : cells.entrySet()) {
            spatialIndex.put(entry.getKey().getX(), entry.getKey().getY(), entry.getValue());
		}
	}

	public EmissionRaster() {
		// no args constructor for recreating the file structure
	}

	public int getCellSize() {
		return cellSize;
	}

	public Cell getCell(double x, double y) {

		return cells.get(new Coord(x, y));
	}

	public Collection<Cell> getCells() {
		return spatialIndex.values();
	}

	public void addEmissions(Id<Link> linkId, Map<Pollutant, Double> emissions) {

		List<Cell> cells = linkMap.get(linkId);
		for (Cell cell : cells) {
			cell.addEmissions(emissions, cells.size());
		}
	}

	public void addCell(Coord coord, Map<Pollutant, Double> pollution) {

        if (coord.getX() < maxX) maxX = coord.getX();
        if (coord.getX() > minX) minX = coord.getX();
        if (coord.getY() < maxY) maxY = coord.getY();
        if (coord.getY() > minY) minY = coord.getY();

        var cell = new Cell(coord);
        cell.addEmissions(pollution, 1);
        cells.put(coord, cell);
    }

	private Map<Coord, Cell> rasterizeNetwork(Network network) {

		Map<Coord, Cell> coordMap = new HashMap<>();
		for (Link link : network.getLinks().values()) {

			// this way, some cell objects are created, which aren't necessary, but rasterizeNetwork and rasterizeLink
			// are pure methods this way. Yay!
			List<Cell> indexedCells = rasterizeLink(link).stream()
					.map(cell -> coordMap.putIfAbsent(cell.getCoord(), cell))
					.collect(Collectors.toList());

			linkMap.put(link.getId(), indexedCells);
		}

		return coordMap;
	}

	/**
	 * Rasterizes links into squares. Uses Bresenham's line drawing algorithm, which is supposed to be fast
	 * Maybe the result is too chunky, but it'll do as a first try
	 *
	 * @param link Matsim network link
	 * @return all cells which are 'touched' by the link
	 */
	private List<Cell> rasterizeLink(Link link) {

		int x0 = (int) (link.getFromNode().getCoord().getX() / cellSize);
		int x1 = (int) (link.getToNode().getCoord().getX() / cellSize);
		int y0 = (int) (link.getFromNode().getCoord().getY() / cellSize);
		int y1 = (int) (link.getToNode().getCoord().getY() / cellSize);
		int dx = Math.abs(x1 - x0);
		int dy = -Math.abs(y1 - y0);
		int err = dx + dy, e2;

		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;

		List<Cell> result = new ArrayList<>();

		do {
			result.add(new Cell(new Coord(x0 * cellSize - (cellSize / 2), y0 * cellSize - cellSize / 2)));
			e2 = err + err;
			if (e2 >= dy) {
				err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        } while (x0 != x1 || y0 != y1);

        return result;
    }

    private void buildSpatialIndex() {
        // create a spatial index of cells so the raster can be queried later
        //double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
        //spatialIndex = new QuadTree<>(bounds[0] - cellSize, bounds[1] - cellSize, bounds[2] + cellSize, bounds[3] + cellSize); //ugh
        spatialIndex = new QuadTree<>(minX, minY, maxX, maxY);
        for (Map.Entry<Coord, Cell> entry : cells.entrySet()) {
            spatialIndex.put(entry.getKey().getX(), entry.getKey().getY(), entry.getValue());
        }
    }

    public static class Cell {

        private final Coord coord;
        private Map<Pollutant, Double> emissions = new HashMap<>();

        private Cell(Coord coord) {
            this.coord = coord;
        }

		public Coord getCoord() {
			return coord;
		}

		public Map<Pollutant, Double> getEmissions() {
			return emissions;
		}

		private void addEmissions(Map<Pollutant, Double> pollution, int divideBy) {
			for (var pollutant : pollution.entrySet()) {
				emissions.merge(pollutant.getKey(), pollutant.getValue() / divideBy, Double::sum);
			}
		}
	}
}
