/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.jbischoff.taxibus.scenario.analysis.quick;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;

import playground.jbischoff.taxibus.scenario.analysis.WobDistanceAnalyzer;

/**
 * @author jbischoff
 *
 */
public class TTEventHandler implements ActivityStartEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler {
	Map<Id<Person>, String> lastActivity = new HashMap<>();
	Map<Id<Person>, Double> lastDeparture = new HashMap<>();
	Map<String, Double> ttToActivity = new TreeMap<>();
	Map<String, Integer> legsToActivity = new HashMap<>();
	ArrayList<String> monitoredModes = new ArrayList<>();
	ArrayList<String> outboundLegs = new ArrayList<>(Arrays.asList(new String[] { "home--work_vw_flexitime","home--work_vw_shift1","home--work_vw_shift2", "home--work"}));
	ArrayList<String> inboundLegs = new ArrayList<>(Arrays.asList(new String[] { "work--home" ,"work_vw_flexitime--home" ,"work_vw_shift1--home" ,"work_vw_shift2--home"}));
	
//	ArrayList<String> monitoredModes = new ArrayList<>(Arrays.asList(new String[] { "taxibus"}));
//	ArrayList<String> monitoredModes = new ArrayList<>(Arrays.asList(new String[] { "car"}));
//	ArrayList<String> monitoredModes = new ArrayList<>(Arrays.asList(new String[] { "pt","taxibus","car"}));
//	ArrayList<String> monitoredModes = new ArrayList<>(Arrays.asList(new String[] { "tpt" }));
	

	public TTEventHandler() {

	}

	@Override
	public void reset(int iteration) {
		
	}
	public void addMode(String monitoredMode){
		this.monitoredModes.add(monitoredMode);
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!this.monitoredModes.contains(event.getLegMode()))
			return;
		if (!isRelevantPerson(event.getPersonId()))
			return;

		this.lastDeparture.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().startsWith("pt"))
			return;
		if (!isRelevantPerson(event.getPersonId()))
			return;
		if (lastDeparture.containsKey(event.getPersonId())) {
			double departureTime = this.lastDeparture.remove(event.getPersonId());
			double travelTime = event.getTime() - departureTime;
			String as = buildActivityString(this.lastActivity.get(event.getPersonId()), event.getActType());
			addTTtoActivity(as, travelTime);
		}
		// else {
		// System.err.println(event.getPersonId() + " at act
		// "+event.getActType() +" had no departure");
		// }
	}

	boolean isRelevantPerson(Id<Person> personId) {
//		return (personId.toString().endsWith("vw") ? true : false);
		return ((personId.toString().startsWith("BS_WB")||(personId.toString().startsWith("WB_BS"))) ? true : false);
//		return (personId.toString().startsWith("BS_WB") ? true : false);

//		return true;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().startsWith("pt"))
			return;
		if (!isRelevantPerson(event.getPersonId()))
			return;

		this.lastActivity.put(event.getPersonId(), event.getActType());

	}

	private void addTTtoActivity(String activityString, double traveltime) {
		int legs = 1;
		if (this.ttToActivity.containsKey(activityString)) {
			traveltime += this.ttToActivity.get(activityString);
			legs += this.legsToActivity.get(activityString);
		}
		this.legsToActivity.put(activityString, legs);
		this.ttToActivity.put(activityString, traveltime);
	}

	private String buildActivityString(String fromAct, String toAct) {
		return fromAct + "--" + toAct;
	}

	public void printOutput() {
		System.out.println("tt between Activities");
		System.out.println("Activity\tLegs\tAveTT");
		for (Entry<String, Double> e : this.ttToActivity.entrySet()) {
			double legs = this.legsToActivity.get(e.getKey());
			System.out.println(
					e.getKey() + "\t" + legs + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(e.getValue() / legs));
		}

	}
	public void writeOutput(String folder){
		String modeString = ""; 
		for (String mode : this.monitoredModes){
			modeString+=mode;
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter(folder+"/act_travelTimes_"+modeString+".txt");
		int inboundLegCount = 0;
		double inboundTravelTime = 0;
		int outboundLegCount = 0;
		double outboundTravelTime = 0;
			
		try {
			writer.append("Modes analysed: "+this.monitoredModes.toString());
			writer.newLine();
			writer.append("Activity\tLegs\tAveTT\n");
			for (Entry<String, Double> e : this.ttToActivity.entrySet()) {
			double legs = this.legsToActivity.get(e.getKey());
			writer.append(e.getKey() + "\t" + Math.round(legs) + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(e.getValue() / legs)+"\n");
			if (inboundLegs.contains(e.getKey())){
				inboundLegCount += legs;
				inboundTravelTime += e.getValue();
			}
			else if (outboundLegs.contains(e.getKey())){
				outboundLegCount += legs;
				outboundTravelTime += e.getValue();
			}
			}
			writer.newLine();
			writer.append("Fahrzeit Morgenspitze" + "\t" + outboundLegCount + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(outboundTravelTime/outboundLegCount)+"\n");
			writer.append("Fahrzeit Abendspitze" + "\t" + inboundLegCount + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(inboundTravelTime/inboundLegCount)+"\n");
			writer.flush();
			writer.close();

			
			
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}
}
