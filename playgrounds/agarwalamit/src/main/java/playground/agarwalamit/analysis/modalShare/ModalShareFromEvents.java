/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.modalShare;

import java.io.BufferedWriter;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.PersonFilter;

/**
 * (1) This excludes the departure of transit drivers.
 * (2) See followings
 * transit_walk - transit_walk --> walk &
 * transit_walk - pt - transit_walk --> pt &
 * transit_walk - pt - transit_walk - pt - transit_walk --> pt 
 * @author amit
 */

public class ModalShareFromEvents implements ModalShare {

	private final String eventsFile;
	private final FilteredModalShareEventHandler mseh;
	private SortedMap<String, Integer> mode2numberOflegs = new TreeMap<>();
	private SortedMap<String, Double> mode2PctOflegs = new TreeMap<>();

	public ModalShareFromEvents(final String eventsFile){
		this.eventsFile = eventsFile;
		this.mseh = new FilteredModalShareEventHandler();
	}
	
	public ModalShareFromEvents(final String eventsFile, final String userGroup, final PersonFilter personFilter){
		this.eventsFile = eventsFile;
		this.mseh = new FilteredModalShareEventHandler(userGroup,personFilter);
	}

	public void run(){
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this.mseh);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(this.eventsFile);
		this.mode2numberOflegs = this.mseh.getMode2numberOflegs();
		this.mode2PctOflegs = MapUtils.getIntPercentShare(this.mode2numberOflegs);
		
		this.mseh.handleRemainingTransitUsers();
	}

	@Override
	public SortedSet<String> getUsedModes() {
		return new TreeSet<>( this.mode2numberOflegs.keySet() );
	}

	@Override
	public SortedMap<String, Integer> getModeToNumberOfLegs() {
		return this.mode2numberOflegs;
	}

	@Override
	public SortedMap<String, Double> getModeToPercentOfLegs() {
		return this.mode2PctOflegs;
	}

	@Override
	public void writeResults(String outputFile){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		try {
			for(String str:mode2numberOflegs.keySet()){
				writer.write(str+"\t");
			}
			writer.write("total \t");
			writer.newLine();

			for(String str:mode2numberOflegs.keySet()){ // write Absolute No Of Legs
				writer.write(mode2numberOflegs.get(str)+"\t");
			}
			writer.write(MapUtils.intValueSum(mode2numberOflegs)+"\t");
			writer.newLine();

			for(String str:mode2PctOflegs.keySet()){ // write percentage no of legs
				writer.write(mode2PctOflegs.get(str)+"\t");
			}
			writer.write(MapUtils.doubleValueSum(mode2PctOflegs)+"\t");
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data can not be written to file. Reason - "+e);
		}
	}
}