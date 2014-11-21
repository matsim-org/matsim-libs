/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.noise2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

/**
 * 
 * Collects each agent's performance of activities throughout the day.
 * 
 * @author ikaddoura
 *
 */

public class PersonActivityHandler implements ActivityEndEventHandler , ActivityStartEventHandler {

	private static final Logger log = Logger.getLogger(PersonActivityHandler.class);
	
	private Scenario scenario;
	private NoiseParameters noiseParams;
	private NoiseInitialization spatialInfo;
		
	private Map<Id<Person>, Integer> personId2activityNumber = new HashMap<Id<Person>, Integer>();
	private Map<Id<Person>, Map<Integer, PersonActivityInfo>> personId2actNr2actInfo = new HashMap<Id<Person>, Map<Integer, PersonActivityInfo>>();
		
	public PersonActivityHandler (Scenario scenario, NoiseParameters noiseParams, NoiseInitialization spatialInfo) {
		this.scenario = scenario;
		this.noiseParams = noiseParams;
		this.spatialInfo = spatialInfo;
	}
	
	@Override
	public void reset(int iteration) {
		this.personId2activityNumber.clear();
		this.personId2actNr2actInfo.clear();
	}
	
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (!(scenario.getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
		
			if (!event.getActType().toString().equals("pt_interaction")) {
				
				// update activity number
				if (this.personId2activityNumber.containsKey(event.getPersonId())){
					int newActNr = this.personId2activityNumber.get(event.getPersonId()) + 1;
					this.personId2activityNumber.put(event.getPersonId(), newActNr);
				} else {
					this.personId2activityNumber.put(event.getPersonId(), 1);
				}
			}
			
			PersonActivityInfo actInfo = new PersonActivityInfo();
			actInfo.setStartTime(event.getTime());
			actInfo.setEndTime(30 * 3600.); // assuming this activity to be the last one in the agents' plan, will be overwritten if it is not the last activity
			actInfo.setPersonId(event.getPersonId());
			actInfo.setActivityType(event.getActType());
			
			if (this.personId2actNr2actInfo.containsKey(event.getPersonId())){
				this.personId2actNr2actInfo.get(event.getPersonId()).put(this.personId2activityNumber.get(event.getPersonId()), actInfo);
			} else {
				Map<Integer, PersonActivityInfo> actNr2actInfo = new HashMap<Integer, PersonActivityInfo>();
				actNr2actInfo.put(this.personId2activityNumber.get(event.getPersonId()), actInfo);
				this.personId2actNr2actInfo.put(event.getPersonId(), actNr2actInfo);
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (!(scenario.getPopulation().getPersons().containsKey(event.getPersonId()))) {
		} else {
			
			if (!event.getActType().toString().equals("pt_interaction")) {
				
				if (this.personId2activityNumber.containsKey(event.getPersonId())){
					// not the first activity
					
					// since this is not the last activity, overwrite the end time
					PersonActivityInfo actInfo = this.personId2actNr2actInfo.get(event.getPersonId()).get(this.personId2activityNumber.get(event.getPersonId()));
					actInfo.setEndTime(event.getTime());
				
				} else {
					// the first activity

					// since this is the first activity, this activity has not yet appeared
					PersonActivityInfo actInfo = new PersonActivityInfo();
					actInfo.setStartTime(0.); // since this is the first activity
					actInfo.setEndTime(event.getTime());
					actInfo.setPersonId(event.getPersonId());
					actInfo.setActivityType(event.getActType());
					
					Map<Integer, PersonActivityInfo> actNr2actInfo = new HashMap<Integer, PersonActivityInfo>();
					actNr2actInfo.put(0, actInfo);
					this.personId2actNr2actInfo.put(event.getPersonId(), actNr2actInfo);
					
				}
			} 
		}		
	}

	public void calculateDurationsOfStay(Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints) {
				
		// sort by receiver point
		Map<Id<ReceiverPoint>, List<PersonActivityInfo>> rpId2actInfos = new HashMap<Id<ReceiverPoint>, List<PersonActivityInfo>>();
		
		for (Id<Person> personId : this.personId2actNr2actInfo.keySet()) {
			
			for (Integer actNr : this.personId2actNr2actInfo.get(personId).keySet()) {
				
				Coord coord = spatialInfo.getPersonId2listOfCoords().get(personId).get(actNr);
				Id<ReceiverPoint> rpId = spatialInfo.getActivityCoord2receiverPointId().get(coord);
				
				if (rpId2actInfos.containsKey(rpId)) {
					rpId2actInfos.get(rpId).add(this.personId2actNr2actInfo.get(personId).get(actNr));
				} else {
					List<PersonActivityInfo> actInfos = new ArrayList<PersonActivityInfo>();
					actInfos.add(this.personId2actNr2actInfo.get(personId).get(actNr));
					rpId2actInfos.put(rpId, actInfos);
				}
			}
		}
		
		// sort by time interval
		for (ReceiverPoint rp : receiverPoints.values()) {

			if (rpId2actInfos.containsKey(rp.getId())) {
				
				for (double timeIntervalEnd = noiseParams.getTimeBinSizeNoiseComputation() ; timeIntervalEnd <= 30 * 3600. ; timeIntervalEnd = timeIntervalEnd + noiseParams.getTimeBinSizeNoiseComputation()) {
					double timeIntervalStart = timeIntervalEnd - noiseParams.getTimeBinSizeNoiseComputation();

					for (PersonActivityInfo actInfo : rpId2actInfos.get(rp.getId())) {

						if (( actInfo.getStartTime() < timeIntervalEnd ) && ( actInfo.getEndTime() >=  timeIntervalStart )) {
							double durationInThisInterval = 0.;
							
							if ((actInfo.getStartTime() <= timeIntervalStart) && actInfo.getEndTime() >= timeIntervalEnd ) {
								durationInThisInterval = noiseParams.getTimeBinSizeNoiseComputation();
							
							} else if (actInfo.getStartTime() <= timeIntervalStart && actInfo.getEndTime() <= timeIntervalEnd) {
								durationInThisInterval = actInfo.getEndTime() - timeIntervalStart;
							
							} else if (actInfo.getStartTime() >= timeIntervalStart && actInfo.getEndTime() >= timeIntervalEnd) {
								durationInThisInterval = timeIntervalEnd - actInfo.getStartTime();
							
							} else if (actInfo.getStartTime() >= timeIntervalStart && actInfo.getEndTime() <= timeIntervalEnd) {
								durationInThisInterval = actInfo.getEndTime() - actInfo.getStartTime();
						
							} else {
								throw new RuntimeException("Unknown case. Aborting...");
							}
							
							actInfo.setDurationWithinInterval(durationInThisInterval);
							
							double affectedUnitsThisAgent = durationInThisInterval / noiseParams.getTimeBinSizeNoiseComputation();
							
							if (rp.getTimeInterval2affectedAgentUnits().containsKey(timeIntervalEnd)){	
								
								Map<Double, Double> time2affectedAgentUnits = rp.getTimeInterval2affectedAgentUnits();
								double affectedAgentUnitsSumNew = time2affectedAgentUnits.get(timeIntervalEnd) + (noiseParams.getScaleFactor() * affectedUnitsThisAgent);
								time2affectedAgentUnits.put(timeIntervalEnd, affectedAgentUnitsSumNew);
								rp.setTimeInterval2affectedAgentUnits(time2affectedAgentUnits);
								
							} else {
								rp.getTimeInterval2affectedAgentUnits().put(timeIntervalEnd, affectedUnitsThisAgent);
							}
							
							if (rp.getTimeInterval2actInfos().containsKey(timeIntervalEnd)) {
								rp.getTimeInterval2actInfos().get(timeIntervalEnd).add(actInfo);
							
							} else {	
								List<PersonActivityInfo> actInfos = new ArrayList<PersonActivityInfo>();
								actInfos.add(actInfo);

								Map<Double, List<PersonActivityInfo>> timeInterval2actInfos = rp.getTimeInterval2actInfos();
								timeInterval2actInfos.put(timeIntervalEnd, actInfos);
								
								rp.setTimeInterval2actInfos(timeInterval2actInfos);
							}
						}
					}
				}
			}
		}	
	}
	
	public void writePersonActivityInfoPerHour(String fileName, Map<Id<ReceiverPoint>, ReceiverPoint> receiverPoints) {
		File file = new File(fileName);
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			// column headers
			bw.write("receiver point");
			for(int i = 0; i < 30 ; i++) {
				String time = Time.writeTime( (i+1) * noiseParams.getTimeBinSizeNoiseComputation(), Time.TIMEFORMAT_HHMMSS );
				bw.write(";agent units " + time);
			}
			bw.newLine();

			
			for (ReceiverPoint rp : receiverPoints.values()) {
				bw.write(rp.getId().toString());
				for(int i = 0 ; i < 30 ; i++) {
					double timeInterval = (i+1) * noiseParams.getTimeBinSizeNoiseComputation();
					double affectedAgentUnits = 0.;
					
					if (rp.getTimeInterval2affectedAgentUnits() != null) {
						if (rp.getTimeInterval2affectedAgentUnits().get(timeInterval) != null) {
							affectedAgentUnits = rp.getTimeInterval2affectedAgentUnits().get(timeInterval);
						}
					}					
					bw.write(";"+ affectedAgentUnits);	
				}
				
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file2));
			bw.write("\"String\"");
			
			for(int i = 0; i < 30 ; i++) {
				bw.write(",\"Real\"");
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written to " + fileName + "t");
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
