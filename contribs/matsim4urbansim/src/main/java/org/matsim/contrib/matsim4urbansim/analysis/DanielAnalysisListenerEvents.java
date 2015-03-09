/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.matsim4urbansim.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author droeder
 *
 */
public class DanielAnalysisListenerEvents implements StartupListener, IterationStartsListener, IterationEndsListener {
	
	private List<CalcMacroZoneTravelTimes> traveltimes;
	private Map<Id<ActivityFacility>, Id<ActivityFacility>> micro2MacroZone;
	private String micro2macroZonesFile;
	private ActivityFacilities parcels;
	private List<Tuple<Integer, Integer>> timeslots;
	
	
	private static final Logger log = Logger
			.getLogger(DanielAnalysisListenerEvents.class);

	/**
	 * 	 *  a listener to include the additional analysis-class <code>CalcMacroZoneTravelTimes</code>
	 * @param cleFile, the csv-File 
	 * @param parcels, parcels from the urbansim-model
	 * @param timeslots, list of tuples, each defining the start and the end hour of a timeslot
	 */
	public DanielAnalysisListenerEvents(String cleFile, ActivityFacilities parcels, List<Tuple<Integer, Integer>> timeslots) {
		this.micro2macroZonesFile = cleFile ;
		this.parcels = parcels;
		this.timeslots = timeslots;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (0 == event.getIteration() % event.getControler().getConfig().controler().getWriteEventsInterval()){
			this.traveltimes = new ArrayList<CalcMacroZoneTravelTimes>();
			CalcMacroZoneTravelTimes traveltimes;
			for(Tuple<Integer, Integer> t: this.timeslots){
				traveltimes = new CalcMacroZoneTravelTimes(this.parcels.getFacilities(), this.micro2MacroZone, t.getFirst(), t.getSecond());
				this.traveltimes.add(traveltimes);
				event.getControler().getEvents().addHandler(traveltimes);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (!(traveltimes == null)){
			for(CalcMacroZoneTravelTimes tt: this.traveltimes){
				tt.writeOutput(event.getControler().getControlerIO().getIterationPath(event.getIteration()));
			}
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.micro2MacroZone = new HashMap<>();
		Set<String[]> zones = readFileContent(micro2macroZonesFile, ",", true);
		for(String[] s: zones){
			this.micro2MacroZone.put(Id.create(s[0], ActivityFacility.class), Id.create(s[2], ActivityFacility.class));
		}
	}
	
	private Set<String[]> readFileContent(String inFile, String splitByExpr, boolean hasHeader){
		
		boolean first = hasHeader;
		Set<String[]> lines = new LinkedHashSet<String[]>();
		
		String line;
		try {
			log.info("start reading content of " + inFile);
			BufferedReader reader = IOUtils.getBufferedReader(inFile);
			line = reader.readLine();
			do{
				if(!(line == null)){
					String[] columns = line.split(splitByExpr);
					if(first == true){
						first = false;
					}else{
						lines.add(columns);
					}
					
					line = reader.readLine();
				}
			}while(!(line == null));
			reader.close();
			log.info("finished...");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return lines;
	}
}

