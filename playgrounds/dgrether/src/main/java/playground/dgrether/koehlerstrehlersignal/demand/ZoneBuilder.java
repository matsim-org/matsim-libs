/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneBuilder
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.demand;

import org.matsim.api.core.v01.network.Network;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.zones.DgZoneUtils;
import playground.dgrether.utils.zones.DgZoneWriter;
import playground.dgrether.utils.zones.DgZones;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 *
 */
public class ZoneBuilder {

	private CoordinateReferenceSystem crs;

	public ZoneBuilder(CoordinateReferenceSystem crs){
		this.crs = crs;
	}
	
	/**
	 * 
	 */
	public DgZones createAndWriteZones(Network network, DgSignalsBoundingBox signalsBoundingBox, 
			int cellsX, int cellsY, String shapeFileDirectory){
		//create a grid
		DgGrid grid = createGrid(signalsBoundingBox.getBoundingBox(),  crs, cellsX, cellsY);
		DgGridUtils.writeGrid2Shapefile(grid, crs, shapeFileDirectory + "grid.shp");

		DgZones zones = DgZoneUtils.createZonesFromGrid(grid);

		
		DgZoneWriter zoneOdWriter = new DgZoneWriter(zones, crs);
		zoneOdWriter.writePolygonZones2Shapefile(shapeFileDirectory + "zones.shp");

	//zones to links matching
		DgZoneUtils.createZoneCenter2LinkMapping(zones, (Network)network);
		DgZoneUtils.writeLinksOfZones2Shapefile(zones, crs, shapeFileDirectory + "links_for_zones.shp");
		
		return zones;
	}
	
	public DgGrid createGrid(Envelope boundingBox, CoordinateReferenceSystem crs, int xCells, int yCells){
		Envelope gridBoundingBox = new Envelope(boundingBox);
		//expand the grid size to avoid rounding errors 
		gridBoundingBox.expandBy(350.0);
		DgGrid grid = new DgGrid(xCells, yCells, gridBoundingBox);
		return grid;
	}

	
}
