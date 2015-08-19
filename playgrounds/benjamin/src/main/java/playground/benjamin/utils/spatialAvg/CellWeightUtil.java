/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.benjamin.utils.spatialAvg;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class CellWeightUtil implements LinkWeightUtil {

	private Map<Link, Cell> links2Cells;
	private SpatialGrid grid;
	final private Double dist0factor = 0.216;
	final private Double dist1factor = 0.132;
	final private Double dist2factor = 0.029;
	final private Double dist3factor = 0.002;
	

	public CellWeightUtil(Collection<Link> links, SpatialGrid grid){
		this.grid = grid;
		this.links2Cells = mapLinksToGridCells(links, grid);
	}

	public CellWeightUtil(Map<Link, Cell> links2cells2, SpatialGrid sGrid) {
		this.grid=sGrid;
		this.links2Cells=links2cells2;
	}

	private Map<Link, Cell> mapLinksToGridCells(Collection<Link> links, SpatialGrid grid) {
		links2Cells = new HashMap<Link, Cell>();
		
		for(Link link: links){
			Cell cCell = grid.getCellForCoordinate(link.getCoord());
			if(cCell!=null)links2Cells.put(link, cCell);
		}
		System.out.println("Mapped " + links2Cells.size() + " links to grid");
		System.out.println((links.size() - links2Cells.size()) + " links were not mapped.");
		return links2Cells;
	}

	@Override
	public Double getWeightFromLink(Link link, Coord cellCentroid) {
		Cell cellOfLink = links2Cells.get(link);
		if(cellOfLink==null){
			return 0.0;
		}else{
			Cell receivingCell = grid.getCellForCoordinate(cellCentroid);
			return calcDistanceFactorFromCells(cellOfLink, receivingCell);
		}
	}

	private Double calcDistanceFactorFromCells(Cell cellOfLink,
			Cell receivingCell) {
		int xDistance = Math.abs(cellOfLink.getXNumber()-receivingCell.getXNumber());
		int yDistance = Math.abs(cellOfLink.getYNumber()-receivingCell.getYNumber());
		int dist = xDistance + yDistance;
		if(dist >= 4) return 0.0;
		switch(dist){
		case 0: return dist0factor;
		case 1: return dist1factor;
		case 2: return dist2factor;
		case 3: return dist3factor;
		}
		return 0.0;
	}

	@Override
	public Double getNormalizationFactor() {
		return 1.0;
	}
	@Override
	public Double getWeightFromCoord(Coord emittingCoord, Coord receivingCoord) {
		Cell emittingCell = grid.getCellForCoordinate(emittingCoord);
		Cell receivingCell = grid.getCellForCoordinate(receivingCoord);
		return calcDistanceFactorFromCells(emittingCell, receivingCell);
	}
}
