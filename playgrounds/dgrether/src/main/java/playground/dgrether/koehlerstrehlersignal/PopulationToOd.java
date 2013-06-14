/* *********************************************************************** *
 * project: org.matsim.*
 * PopulationToOd
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
package playground.dgrether.koehlerstrehlersignal;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.utils.DgGrid;
import playground.dgrether.utils.DgGridUtils;
import playground.dgrether.utils.DgPopulationSampler;
import playground.dgrether.utils.zones.DgMatsimPopulation2Links;
import playground.dgrether.utils.zones.DgZoneUtils;
import playground.dgrether.utils.zones.DgZoneWriter;
import playground.dgrether.utils.zones.DgZones;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dgrether
 *
 */
public class PopulationToOd {

	private double matsimPopSampleSize = 1.0;
	private Map<Id, Id> originalToSimplifiedLinkIdMatching;
	private DgZones zones;
	
	
	
	public void matchPopulationToGrid(Network fullNetwork, Population population, CoordinateReferenceSystem crs, 
			Network smallNetwork, DgSignalsBoundingBox signalsBoundingBox, int cellsX, int cellsY,
			double startTimeSec, double endTimeSec, String shapeFileDirectory){
		//create a grid
		DgGrid grid = createGrid(signalsBoundingBox.getBoundingBox(),  crs, cellsX, cellsY);
		DgGridUtils.writeGrid2Shapefile(grid, crs, shapeFileDirectory + "grid.shp");

		//create some zones and map demand on the zones
		if (matsimPopSampleSize != 1.0){
			new DgPopulationSampler().samplePopulation(population, matsimPopSampleSize);
		}
		
		//create some zones and match the population to them
		this.zones = DgZoneUtils.createZonesFromGrid(grid);
		DgMatsimPopulation2Links pop2zones = new DgMatsimPopulation2Links();
		
		pop2zones.convert2Zones(fullNetwork, smallNetwork, this.originalToSimplifiedLinkIdMatching,
				population, zones, signalsBoundingBox.getBoundingBox(), startTimeSec, endTimeSec);
		
		//write	 the matching to some files
		DgZoneWriter zoneOdWriter = new DgZoneWriter(zones, crs);
		zoneOdWriter.writePolygonZones2Shapefile(shapeFileDirectory + "zones.shp");
//		zoneOdWriter.writeLineStringZone2ZoneOdPairsFromZones2Shapefile(shapeFileDirectory + "zone2dest_od_pairs.shp");
		zoneOdWriter.writeLineStringLink2LinkOdPairsFromZones2Shapefile(shapeFileDirectory + "link2dest_od_pairs.shp");
	}
	
	public DgZones getZones(){
		return zones;
	}
	
	public DgGrid createGrid(Envelope boundingBox, CoordinateReferenceSystem crs, int xCells, int yCells){
		Envelope gridBoundingBox = new Envelope(boundingBox);
		//expand the grid size to avoid rounding errors 
		gridBoundingBox.expandBy(350.0);
		DgGrid grid = new DgGrid(xCells, yCells, gridBoundingBox);
		return grid;
	}
	
	public double getMatsimPopSampleSize() {
		return matsimPopSampleSize;
	}

	
	public void setMatsimPopSampleSize(double matsimPopSampleSize) {
		this.matsimPopSampleSize = matsimPopSampleSize;
	}

	public void setOriginalToSimplifiedLinkMapping(Map<Id, Id> originalToSimplifiedLinkIdMatching) {
		this.originalToSimplifiedLinkIdMatching = originalToSimplifiedLinkIdMatching;
	}
	
}
