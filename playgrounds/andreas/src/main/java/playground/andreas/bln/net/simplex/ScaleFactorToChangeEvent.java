/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.andreas.bln.net.simplex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class ScaleFactorToChangeEvent {
	
	private Network network;
	private BufferedWriter writer;
	
	private TreeMap<String, Double> timeScaleFactorMap = new TreeMap<String, Double>();
	
	public ScaleFactorToChangeEvent(String networkFile, String outFile){
		this.network = readNetwork(networkFile);
		try {
			this.writer = new BufferedWriter(new FileWriter(new File(outFile)));			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addTimeScaleFactorSet(String time, double scaleFactor){
		this.timeScaleFactorMap.put(time, Double.valueOf(scaleFactor));
	}
	
	public void writeEvents() throws IOException{
		
		this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); this.writer.newLine();
		this.writer.write("<networkChangeEvents xmlns=\"http://www.matsim.org/files/dtd\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/networkChangeEvents.xsd\">"); this.writer.newLine();
		this.writer.newLine();
		
		double lastScaleFactor = 1.0;
		
		for (Entry<String, Double> entry : this.timeScaleFactorMap.entrySet()) {
			this.writer.write("<networkChangeEvent startTime=\"" + entry.getKey() + "\">"); this.writer.newLine();
			for (Link link : this.network.getLinks().values()) {
				if(link.getId().toString().contains("miv_")){
					// ignore miv link
				} else {
					this.writer.write("<link refId=\"" + link.getId().toString() + "\"/>"); this.writer.newLine();
				}
			}
			this.writer.write("<freespeed type=\"scaleFactor\" value=\"" + (entry.getValue().doubleValue() / lastScaleFactor) + "\"/>"); this.writer.newLine();
			this.writer.write("</networkChangeEvent>"); this.writer.newLine();
			
			this.writer.newLine();
			
			lastScaleFactor = entry.getValue().doubleValue();
		}		
		
		this.writer.write("</networkChangeEvents>");
		this.writer.flush();
		this.writer.close();		
	}
	
	private Network readNetwork(String networkFile){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(scenario.getNetwork());
		matsimNetReader.readFile(networkFile);
		return scenario.getNetwork();
	}

}
