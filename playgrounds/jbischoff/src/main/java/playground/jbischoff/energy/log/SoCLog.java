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

package playground.jbischoff.energy.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.analysis.charging.ChargingLogRowLinkLevel;

/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */
public class SoCLog {

	private LinkedList<SocLogRow> log;

	public SoCLog() {
		reset();
	}

	public void reset() {
		log = new LinkedList<SocLogRow>();
	}

	public void add(SocLogRow row) {
		log.add(row);
	}

	public SocLogRow get(int i) {
		return log.get(i);
	}

	public int getNumberOfEntries() {
		return log.size();
	}

	public String getTitleRowFileOutput() {
		return "agentId\ttime\tSoC(kWh)\trelative SOC";
	}

	public void printToConsole() {
		System.out.println(getTitleRowFileOutput());
		Collections.sort(log);
		for (SocLogRow row:log){
			System.out.println(rowToString(row));
		}
	}
	
	private String rowToString(SocLogRow row){
		return (row.getAgentId() + "\t"+ row.getTime() + "\t" +(row.getAbsoluteLOC()/3600./1000.)+ "\t" +row.getRelativeLOC());
	}
	
	public void writeToFile(String outputFile)  {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFile)));
			Collections.sort(log);
			bw.write(this.getTitleRowFileOutput());
			for (SocLogRow row:log){
				bw.newLine();
				bw.write(rowToString(row));
				
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.err.println("Could not create File" + outputFile);
			e.printStackTrace();
		}
	}
	
	public int size(){
		return log.size();
	}



public void writeToFiles(String outputFileDir)  {
	
	try {
		if (log.isEmpty()) return;
		
		Collections.sort(log);
		Id last = log.get(0).getAgentId();
		String filename = (outputFileDir+"/"+last.toString()+".txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
		
//		bw.write(this.getTitleRowFileOutput());
		
		
		for (SocLogRow row:log){
			if (row.getAgentId().equals(last)){
			bw.newLine();
			bw.write(rowToString(row));
			last  = row.getAgentId();
			}
			else {
				bw.flush();
				bw.close();
				last  = row.getAgentId();
				filename = (outputFileDir+"/"+last.toString()+".txt");
				bw = new BufferedWriter(new FileWriter(new File(filename)));
				bw.write(rowToString(row));
			}
			
		}
		bw.flush();
		bw.close();
		
	} catch (IOException e) {
		System.err.println("Could not create File" + outputFileDir);
		e.printStackTrace();
	}
}


}
