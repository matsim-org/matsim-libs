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
package playground.dgrether.koehlerstrehlersignal.demand;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.dgrether.signalsystems.utils.DgSignalsBoundingBox;
import playground.dgrether.utils.DgPopulationSampler;
import playground.dgrether.utils.zones.DgMatsimPopulation2Links;
import playground.dgrether.utils.zones.DgZoneWriter;
import playground.dgrether.utils.zones.DgZones;


/**
 * Matches the population routed on a large scale network to a smaller network. 
 * Then it converts the population to od pairs
 * @author dgrether
 *
 */
public class PopulationToOd {

	private double matsimPopSampleSize = 1.0;
	private Map<Id<Link>, Id<Link>> originalToSimplifiedLinkIdMatching;
	
	public void convertPopulation2OdPairs(DgZones zones, Network fullNetwork, Population population, CoordinateReferenceSystem crs, 
			Network smallNetwork, DgSignalsBoundingBox signalsBoundingBox, 
			double startTimeSec, double endTimeSec, String shapeFileDirectory){

		if (matsimPopSampleSize != 1.0){
			new DgPopulationSampler().samplePopulation(population, matsimPopSampleSize);
		}
		
		//create some zones and match the population to them
		DgMatsimPopulation2Links pop2Links = new DgMatsimPopulation2Links();
		pop2Links.convert2Links(fullNetwork, smallNetwork, this.originalToSimplifiedLinkIdMatching,
				population, zones, signalsBoundingBox.getBoundingBox(), startTimeSec, endTimeSec);

//		DgMatsimPopulation2Zones pop2Zones = new DgMatsimPopulation2Zones();
//		pop2Zones.convert2Zones(fullNetwork, smallNetwork, originalToSimplifiedLinkIdMatching, 
//				population, zones, signalsBoundingBox.getBoundingBox(), startTimeSec, endTimeSec);
		
		//write	 the matching to some files
		DgZoneWriter zoneOdWriter = new DgZoneWriter(zones, crs);
		zoneOdWriter.writeLineStringLink2LinkOdPairsFromZones2Shapefile(shapeFileDirectory + "link2dest_od_pairs.shp");
	}
	
	
	public double getMatsimPopSampleSize() {
		return matsimPopSampleSize;
	}

	
	public void setMatsimPopSampleSize(double matsimPopSampleSize) {
		this.matsimPopSampleSize = matsimPopSampleSize;
	}

	public void setOriginalToSimplifiedLinkMapping(Map<Id<Link>, Id<Link>> originalToSimplifiedLinkIdMatching) {
		this.originalToSimplifiedLinkIdMatching = originalToSimplifiedLinkIdMatching;
	}
	
}
