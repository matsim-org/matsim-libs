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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import playground.jbischoff.taxibus.scenario.analysis.WobDistanceAnalyzer;

/**
 * @author jbischoff
 *
 */
public class TraveltimeAndDistanceEventHandler implements ActivityStartEventHandler, PersonDepartureEventHandler, ActivityEndEventHandler, LinkEnterEventHandler {
	Map<Id<Person>, String> lastActivity = new HashMap<>();
	Map<Id<Person>, Double> lastDeparture = new HashMap<>();
	Map<Id<Person>, Double> currentDistance = new HashMap<>();
	DecimalFormat df = new DecimalFormat( "##,##0.00" );

	Map<String, Double> ttToActivity = new TreeMap<>();
	Map<String, Double> distanceToActivity = new TreeMap<>();
	Map<String, Integer> legsToActivity = new HashMap<>();
	ArrayList<String> monitoredModes = new ArrayList<>();
	ArrayList<String> outboundLegs = new ArrayList<>(Arrays.asList(new String[] { "home--work_vw_flexitime","home--work_vw_shift1","home--work_vw_shift2", "home--work"}));
	ArrayList<String> inboundLegs = new ArrayList<>(Arrays.asList(new String[] { "work--home" ,"work_vw_flexitime--home" ,"work_vw_shift1--home" ,"work_vw_shift2--home"}));
	private final Network network;

	public TraveltimeAndDistanceEventHandler(Network network) {
		this.network = network;
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
		if (event.getLegMode().equals("car")){
			this.currentDistance.put(event.getPersonId(), 0.0);
		}
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
			if (currentDistance.containsKey(event.getPersonId())){
			addDistancetoActivity(as, currentDistance.remove(event.getPersonId()));
			}
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
	private void addDistancetoActivity(String activityString, double distance) {
		int legs = 1;
		if (this.distanceToActivity.containsKey(activityString)) {
			distance += this.distanceToActivity .get(activityString);
		}
		this.distanceToActivity.put(activityString, distance);
	}

	private String buildActivityString(String fromAct, String toAct) {
		return fromAct + "--" + toAct;
	}

	public void printOutput() {
		System.out.println("tt & distances between Activities");
		System.out.println("Activity\tLegs\tAveTT");
		for (Entry<String, Double> e : this.ttToActivity.entrySet()) {
			double legs = this.legsToActivity.get(e.getKey());
			double distance = 0.0; 
			if (this.distanceToActivity.containsKey(e.getKey())) distance = distanceToActivity.get(e.getKey());
			distance = distance / legs;
			System.out.println(
					e.getKey() + "\t" + legs + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(e.getValue() / legs) + "\t"+distance);
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
		double inboundDistance = 0;
		double outboundDistance = 0;
		int outboundLegCount = 0;
		double outboundTravelTime = 0;
		boolean writeDistance = false;
		if ((this.monitoredModes.size() == 1)&&this.monitoredModes.get(0).equals("car")){
			writeDistance = true;
		}
			
		try {
			writer.append("Modes analysed: "+this.monitoredModes.toString());
			writer.newLine();
			writer.append("Activity\tLegs\tAverageTT");
			if (writeDistance) writer.append("\tDistance");
			writer.newLine();
			
			for (Entry<String, Double> e : this.ttToActivity.entrySet()) {
			double legs = this.legsToActivity.get(e.getKey());
			double distance = 0.0; 
			if (this.distanceToActivity.containsKey(e.getKey())) distance = distanceToActivity.get(e.getKey());
			distance = distance / 1000;
			writer.append(e.getKey() + "\t" + Math.round(legs) + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(e.getValue() / legs));
			if (writeDistance) writer.append( "\t"+df.format(distance/legs));
			writer.newLine();
			
			if (inboundLegs.contains(e.getKey())){
				inboundLegCount += legs;
				inboundTravelTime += e.getValue();
				inboundDistance += distance;
			}
			else if (outboundLegs.contains(e.getKey())){
				outboundLegCount += legs;
				outboundTravelTime += e.getValue();
				outboundDistance += distance;
			}
			}
			writer.newLine();
			writer.append("Morgenspitze" + "\t" + outboundLegCount + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(outboundTravelTime/outboundLegCount));
			if (writeDistance) writer.append("\t"+df.format(outboundDistance/outboundLegCount));
			writer.newLine();
			writer.append("Abendspitze" + "\t" + inboundLegCount + "\t" + WobDistanceAnalyzer.prettyPrintSeconds(inboundTravelTime/inboundLegCount));
			if (writeDistance) writer.append("\t"+df.format(inboundDistance/inboundLegCount));
			writer.flush();
			writer.close();

			
			
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> pid = vId2PId(event.getVehicleId());
		if (currentDistance.containsKey(pid)){
			double length = network.getLinks().get(event.getLinkId()).getLength() + currentDistance.get(pid);
			currentDistance.put(pid, length);
			
		}
	}
	
	private Id<Person> vId2PId(Id<Vehicle> vid){
		return Id.createPersonId(vid.toString());
	}
}
