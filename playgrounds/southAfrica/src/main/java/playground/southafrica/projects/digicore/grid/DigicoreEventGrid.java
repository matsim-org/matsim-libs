/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreEventGrid.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.southafrica.projects.digicore.grid;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import playground.southafrica.projects.digicore.scoring.DIGICORE_EVENT;

/**
 * Building a risk space from purely recorded extreme events. In this approach,
 * we do not distinguish between the severity of different events. If an event 
 * was considered <i>extreme</i> by Digicore, it is enough: we consider it 
 * extreme as well.
 *
 * @author jwjoubert
 */
public class DigicoreEventGrid extends DigiGrid {
	final private static Logger LOG = Logger.getLogger(DigicoreEventGrid.class);
	
	double pointsConsidered = 0.0;

	/* Specific risk space objects. */
	private Map<DIGICORE_EVENT, Integer> countMap = new TreeMap<>();

	
	public DigicoreEventGrid() {

	}

	@Override
	public void setupGrid(String filename) {
		LOG.info("Setting up DigicoreEventGrid...");
		
		LOG.info("Done setting up grid.");
	}

	@Override
	public void rankGridCells() {
		LOG.info("Ranking grid cells...");
		
		super.setRanked(true);
		LOG.info("Done ranking grid cells.");
	}
	
	public void incrementCell(String record){
		String[] sa = record.split(",");
		DIGICORE_EVENT event = DIGICORE_EVENT.getEvent(Integer.parseInt(sa[2]));
		if(!countMap.containsKey(event)){
			countMap.put(event, 1);
		} else{
			int oldValue = countMap.get(event);
			countMap.put(event, oldValue+1);
		}
		
		pointsConsidered++;
	}
	
	public void reportEventCounts(){
		LOG.info("=======================================================");
		LOG.info("Summary of the number of each event type observed:");
		LOG.info("-------------------------------------------------------");
		for(DIGICORE_EVENT event : countMap.keySet()){
			LOG.info(String.format("%20s: %d", event.toString(), countMap.get(event)));
		}
		LOG.info("=======================================================");
	}
	
}
