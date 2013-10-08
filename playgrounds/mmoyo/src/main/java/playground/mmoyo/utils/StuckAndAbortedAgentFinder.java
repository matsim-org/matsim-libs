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

package playground.mmoyo.utils;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
	
/** Reads and event file and counts stuck and aborted vehicles and agents */
public class StuckAndAbortedAgentFinder implements PersonStuckEventHandler{
		private Map <String, Integer> mode_stuckNum_map = new TreeMap <String, Integer>();		
		final String strNull = "null";
		
		@Override
		public void reset(int iteration) {
			
		}
		
		@Override
		public void handleEvent(PersonStuckEvent event) {
			String mode = event.getLegMode();
			if (mode==null){mode = strNull;}
			if (!mode_stuckNum_map.keySet().contains(mode)){
				mode_stuckNum_map.put(mode, 0);
			}
			
			int tmp= mode_stuckNum_map.get(mode);
			mode_stuckNum_map.put(mode, ++tmp);
		}

		protected Map <String, Integer> getMode_stuckNum_map(){
			return this.mode_stuckNum_map;	
		}
		
		public static void main(String[] args) {
			String inputEventsFile;
			if(args.length>0){
				inputEventsFile = args[0];
			}else{
				inputEventsFile = "../../";
			}
			 
			StuckAndAbortedAgentFinder stuckAndAbortedAgentFinder = new StuckAndAbortedAgentFinder();
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(stuckAndAbortedAgentFinder);
			MatsimEventsReader reader = new MatsimEventsReader(events);
			reader.readFile(inputEventsFile);
			
			System.out.println("Events file read. Stuck agents:"); 
			for(Map.Entry <String,Integer> entry: stuckAndAbortedAgentFinder.getMode_stuckNum_map().entrySet() ){
				String key = entry.getKey();
				Integer value = entry.getValue();
				System.out.println(key + " " + value);
			}
			
		}
}