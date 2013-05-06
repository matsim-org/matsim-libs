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
package org.matsim.contrib.matsim4opus.analysis;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author droeder
 *
 */
public class DanielAnalysisListenerEvents implements StartupListener, IterationStartsListener, IterationEndsListener {
	
	private CalcMacroZoneTravelTimes traveltimes;
	private Map<Id, Id> micro2MacroZone;
	private String micro2macroZonesFile;
	private ActivityFacilities parcels;
	
	private static final Logger log = Logger
			.getLogger(DanielAnalysisListenerEvents.class);

	public DanielAnalysisListenerEvents(String matsim4Opustemp, ActivityFacilities parcels) {
		this.micro2macroZonesFile = matsim4Opustemp + "/cle.csv";
		this.parcels = parcels;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (0 == event.getIteration() % event.getControler().getConfig().controler().getWriteEventsInterval()){
			traveltimes = new CalcMacroZoneTravelTimes(this.parcels.getFacilities(), this.micro2MacroZone);
			event.getControler().getEvents().addHandler(traveltimes);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (!(traveltimes == null)){
			traveltimes.writeOutput(event.getControler().getControlerIO().getIterationPath(event.getIteration()));
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		this.micro2MacroZone = new HashMap<Id, Id>();
		Set<String[]> zones = readFileContent(micro2macroZonesFile, ",", true);
		for(String[] s: zones){
			this.micro2MacroZone.put(new IdImpl(s[0]), new IdImpl(s[2]));
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

