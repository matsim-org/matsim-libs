/* *********************************************************************** *
 * project: org.matsim.*
 * DgCalcFlightLineswitch
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
package air.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


/**
 * @author dgrether
 *
 */
public class DgFlightLineSwitchEventHandler implements ActivityEndEventHandler {

	private static final Logger log = Logger.getLogger(DgFlightLineSwitchEventHandler.class);
	
	private Map<Id, Integer> personIdPtInteractionMap = new HashMap<Id, Integer>();
	
	@Override
	public void reset(int iteration) {
		this.personIdPtInteractionMap.clear();
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().compareTo("home") == 0){
			this.personIdPtInteractionMap.put(event.getPersonId(), 0);
		}
		else if (event.getActType().compareTo("pt interaction")  == 0) {
			int ptinteractions = this.personIdPtInteractionMap.get(event.getPersonId());
			ptinteractions++;
			this.personIdPtInteractionMap.put(event.getPersonId(), ptinteractions);
			if (ptinteractions > 3){
				log.debug("Person " + event.getPersonId().toString() +  " has " + ptinteractions + "  interacts...");
			}
		}
	}
	
	private void evalPtInteractions(){
		Map<Integer, Integer> noLineSwitchCountMap = new HashMap<Integer, Integer>();
		for (Entry<Id, Integer> e : this.personIdPtInteractionMap.entrySet()) {
			if (e.getValue() < 2) {
//				log.warn("Person id " + e.getKey() + " has " + e.getValue() + " pt interactions");
			}
			int lineSwitch = e.getValue() - 2;
			Integer count = noLineSwitchCountMap.get(lineSwitch);
			if (count == null){
				count = 0;
			}
			count++;
			noLineSwitchCountMap.put(lineSwitch, count);
		}
		
		for (Entry<Integer, Integer> e : noLineSwitchCountMap.entrySet()){
			log.info("No line switch: " + e.getKey() + " number of passengers: " + e.getValue());
		}
	}

	public void calcLineswitch(String eventsFile) {
		log.info("Processing: " + eventsFile);
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(this);
		MatsimEventsReader reader = new MatsimEventsReader(manager);
		reader.readFile(eventsFile); 
		
		this.evalPtInteractions();
		log.info("done.");
	}
	
	


}
