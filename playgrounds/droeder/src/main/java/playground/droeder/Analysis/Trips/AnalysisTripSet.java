/* *********************************************************************** *
 * project: org.matsim.*
 * Plansgenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.droeder.Analysis.Trips;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.TransportMode;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class AnalysisTripSet {
	
	private static final Logger log = Logger.getLogger(AnalysisTripSet.class);
	
	private List<AnalysisTrip> trips;
	private boolean storeTrips;
	
	private Geometry zone;
	private String mode;
	
	
	//[0]inside, [1]outside, [2]crossing
	//all modes
	private double[] sumTTime = new double[3];
	private double[] tripCnt = new double[3];
	private double[] avTripTTime = new double[3];

	//pt only
	private double[] accesWalkCnt = new double[3];
	private double[] accesWaitCnt = new double[3];
	private double[] egressWalkCnt = new double[3];
	private double[] switchWalkCnt = new double[3];
	private double[] switchWaitCnt = new double[3];
	private double[] lineCnt = new double[3];
	
	private double[] accesWalkTTime = new double[3];
	private double[] accesWaitTime = new double[3];
	private double[] egressWalkTTime = new double[3];
	private double[] switchWalkTTime = new double[3];
	private double[] switchWaitTime = new double[3];
	private double[] lineTTime = new double[3];
	
	private double[] avAccesWalkTTime = new double[3];
	private double[] avAccesWaitTime = new double[3];
	private double[] avEgressWalkTTime = new double[3];
	private double[] avSwitchWalkTTime = new double[3];
	private double[] avSwitchWaitTime = new double[3];
	private double[] avLineTTime = new double[3];
	
	private double[] switch0cnt = new double[3];
	private double[] switch1cnt = new double[3];
	private double[] switch2cnt = new double[3];
	private double[] switchGT2cnt = new double[3];
	
	private double[] line1cnt = new double[3];
	private double[] line2cnt = new double[3];
	private double[] line3cnt = new double[3];
	private double[] lineGt3cnt = new double[3];
	
	private double[] fSwitch0 = new double[3];
	private double[] fSwitch1 = new double[3];
	private double[] fSwitch2 = new double[3];
	private double[] fSwitchGT2 = new double[3];
	
	private double[] fLine1 = new double[3];
	private double[] fLine2 = new double[3];
	private double[] fLine3 = new double[3];
	private double[] flineGt3 = new double[3];

	/**
	 * @param zones
	 */
	public AnalysisTripSet(String mode, Geometry zone, boolean storeTrips) {
		this.zone = zone;
		this.storeTrips = storeTrips;
		if(storeTrips){
			this.trips = new LinkedList<AnalysisTrip>();
		}
		this.mode = mode;
		this.init();
	}
	
	
	private void init() {
		for(int i = 0; i < 3; i++){
			sumTTime[i] = 0.0;
			tripCnt[i] = 0.0;
			avTripTTime[i] = 0.0;
			
			accesWalkCnt[i] = 0.0;
			accesWaitCnt[i] = 0.0;
			egressWalkCnt[i] = 0.0;
			switchWalkCnt[i] = 0.0;
			switchWaitCnt[i] = 0.0;
			lineCnt[i] = 0.0;
			
			accesWalkTTime[i] = 0.0;
			accesWaitTime[i] = 0.0;
			egressWalkTTime[i] = 0.0;
			switchWalkTTime[i] = 0.0;
			switchWaitTime[i] = 0.0;
			lineTTime[i] = 0.0;
			
			avAccesWalkTTime[i] = 0.0;
			avAccesWaitTime[i] = 0.0;
			avEgressWalkTTime[i] = 0.0;
			avSwitchWalkTTime[i] = 0.0;
			avSwitchWaitTime[i] = 0.0;
			avLineTTime[i] = 0.0;
			
			switch0cnt[i] = 0.0;
			switch1cnt[i] = 0.0;
			switch2cnt[i] = 0.0;
			switchGT2cnt[i] = 0.0;
			
			line1cnt[i] = 0.0;
			line2cnt[i] = 0.0;
			line3cnt[i] = 0.0;
			lineGt3cnt[i] = 0.0;
			
			fSwitch0[i] = 0.0;
			fSwitch1[i] = 0.0;
			fSwitch2[i] = 0.0;
			fSwitchGT2[i] = 0.0;
			
			fLine1[i] = 0.0;
			fLine2[i] = 0.0;
			fLine3[i] = 0.0;
			flineGt3[i] = 0.0;
		}
	}


	public AnalysisTripSet(String mode, Geometry zone){
		this(mode, zone, false);
	}
	
	public AnalysisTripSet(String mode){
		this(mode, null, false);
	}

	/**
	 * @param trip
	 */
	public void addTrip(AnalysisTrip trip) {
		Integer zone = this.getTripLocation(trip);
		
		if(trip.getMode().equals(this.mode)){
			this.addTripValues(trip, zone);
		}else{ 
			log.error("wrong tripMode for TripSet");
		}
		
		if(this.storeTrips){
			this.trips.add(trip);
		}
		
	}
	

	private void addTripValues(AnalysisTrip trip, Integer zone) {
		this.addAllModeValues(trip, zone);
		if(trip.getMode().equals(TransportMode.pt)){
			this.addPtValues(trip, zone);
		}
	}

	private void addAllModeValues(AnalysisTrip trip, Integer zone) {
		tripCnt[zone]++;
		sumTTime[zone] += trip.getTripTTime();
		avTripTTime[zone] = sumTTime[zone] / tripCnt[zone];
	}

	private void addPtValues(AnalysisTrip trip, Integer zone) {
		accesWalkCnt[zone] += trip.getAccesWalkCnt(); 
		accesWalkTTime[zone] += trip.getAccesWalkTTime();
		avAccesWalkTTime[zone] = accesWalkTTime[zone] / accesWalkCnt[zone];
		
		accesWaitCnt[zone] += trip.getAccesWaitCnt();
		accesWaitTime[zone] += trip.getAccesWaitTime();
		avAccesWaitTime[zone] = accesWaitTime[zone] / accesWaitCnt[zone];
		
		egressWalkCnt[zone] += trip.getEgressWalkCnt();
		egressWalkTTime[zone] += trip.getEgressWalkTTime();
		avEgressWalkTTime[zone] = egressWalkTTime[zone] / egressWalkCnt[zone];
		
		switchWalkCnt[zone] += trip.getSwitchWalkCnt();
		switchWalkTTime[zone] += trip.getSwitchWalkTTime();
		avSwitchWalkTTime[zone] = switchWalkTTime[zone] / switchWalkCnt[zone];
		
		switchWaitCnt[zone] += trip.getSwitchWaitCnt();
		switchWaitTime[zone] += trip.getSwitchWaitTime();
		avSwitchWaitTime[zone] = switchWaitTime[zone] / switchWaitCnt[zone]; 
		
		lineCnt[zone] += trip.getLineCnt();
		lineTTime[zone] += trip.getLineTTime();
		avLineTTime[zone] = lineTTime[zone] / lineCnt[zone];
		
		switch(trip.getLineCnt()){
			case 1: line1cnt[zone]++; switch0cnt[zone]++; break;
			case 2: line2cnt[zone]++; switch1cnt[zone]++; break;
			case 3: line3cnt[zone]++; switch2cnt[zone]++; break;
			default: lineGt3cnt[zone]++; switchGT2cnt[zone]++; break;
		}
		double temp = line1cnt[zone] + line2cnt[zone] + line3cnt[zone] + lineGt3cnt[zone];
		fLine1[zone] = line1cnt[zone] / temp;
		fLine2[zone] = line2cnt[zone] / temp;
		fLine3[zone] = line3cnt[zone] / temp;
		flineGt3[zone] = lineGt3cnt[zone] / temp;
		
		temp = switch0cnt[zone] + switch1cnt[zone] + switch2cnt[zone] + switchGT2cnt[zone];
		fSwitch0[zone] = switch0cnt[zone] / temp;
		fSwitch1[zone] = switch1cnt[zone] / temp;
		fSwitch2[zone] = switch2cnt[zone] / temp;
		fSwitchGT2[zone] = switchGT2cnt[zone] / temp;
	}


	public void addTrips(List<AnalysisTrip> trips){
		int nextMsg = 1;
		int counter = 0;
		for(AnalysisTrip trip : trips){
			this.addTrip(trip);
			counter++;
			if(counter % nextMsg == 0){
				Log.info("processed " + counter + " of " + trips.size());
				nextMsg *= 2;
			}
		}
	}
	
	
	private Integer getTripLocation(AnalysisTrip trip){
		if(this.zone == null){
			return 0;
		}else if(this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return 0;
		}else if(!this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return 2;
		}else if(this.zone.contains(trip.getStart()) && !this.zone.contains(trip.getEnd())){
			return 2;
		}else {
			return 1;
		}
	}
	
	
	public List<AnalysisTrip> getTrips(){
		return this.trips;
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		buffer.append("zone\t");
		buffer.append("sumTTime\t");
		buffer.append("tripCnt\t");
		buffer.append("avTripTTime\t");
		
		if(this.mode.equals(TransportMode.pt)){
			buffer.append("accesWalkCnt\t");
			buffer.append("accesWaitCnt\t");
			buffer.append("egressWalkCnt\t");
			buffer.append("switchWalkCnt\t");
			buffer.append("switchWaitCnt\t");
			buffer.append("lineCnt\t");
			
			buffer.append("accesWalkTTime\t");
			buffer.append("accesWaitTime\t");
			buffer.append("egressWalkTTime\t");
			buffer.append("switchWalkTTime\t");
			buffer.append("switchWaitTime\t");
			buffer.append("lineTTime\t");
			
			buffer.append("avAccesWalkTTime\t");
			buffer.append("avAccesWaitTime\t");
			buffer.append("avEgressWalkTTime\t");
			buffer.append("avSwitchWalkTTime\t");
			buffer.append("avSwitchWaitTime\t");
			buffer.append("avLineTTime\t");
			
			buffer.append("switch0cnt\t");
			buffer.append("switch1cnt\t");
			buffer.append("switch2cnt\t");
			buffer.append("switchGT2cnt\t");
			
			buffer.append("line1cnt\t");
			buffer.append("line2cnt\t");
			buffer.append("line3cnt\t");
			buffer.append("lineGt3cnt\t");
			
			buffer.append("fSwitch0\t");
			buffer.append("fSwitch1\t");
			buffer.append("fSwitch2\t");
			buffer.append("fSwitchGT2\t");
			
			buffer.append("fLine1\t");
			buffer.append("fLine2\t");
			buffer.append("fLine3\t");
			buffer.append("flineGt3\t");
		}
		buffer.append("\n");
		
		for(int i = 0; i < 3; i++){
			switch(i){
				case 0: buffer.append("in\t"); break;
				case 1: buffer.append("out\t"); break;
				case 2: buffer.append("cross\t"); break;
			}
			buffer.append(sumTTime[i] + "\t");
			buffer.append(tripCnt[i] + "\t");
			buffer.append(avTripTTime[i] + "\t");
			
			if(this.mode.equals(TransportMode.pt)){
				
				buffer.append(accesWalkCnt[i] + "\t");
				buffer.append(accesWaitCnt[i] + "\t");
				buffer.append(egressWalkCnt[i] + "\t");
				buffer.append(switchWalkCnt[i] + "\t");
				buffer.append(switchWaitCnt[i] + "\t");
				buffer.append(lineCnt[i] + "\t");
				
				buffer.append(accesWalkTTime[i] + "\t");
				buffer.append(accesWaitTime[i] + "\t");
				buffer.append(egressWalkTTime[i] + "\t");
				buffer.append(switchWalkTTime[i] + "\t");
				buffer.append(switchWaitTime[i] + "\t");
				buffer.append(lineTTime[i] + "\t");
				
				buffer.append(avAccesWalkTTime[i] + "\t");
				buffer.append(avAccesWaitTime[i] + "\t");
				buffer.append(avEgressWalkTTime[i] + "\t");
				buffer.append(avSwitchWalkTTime[i] + "\t");
				buffer.append(avSwitchWaitTime[i] + "\t");
				buffer.append(avLineTTime[i] + "\t");
				
				buffer.append(switch0cnt[i] + "\t");
				buffer.append(switch1cnt[i] + "\t");
				buffer.append(switch2cnt[i] + "\t");
				buffer.append(switchGT2cnt[i] + "\t");
				
				buffer.append(line1cnt[i] + "\t");
				buffer.append(line2cnt[i] + "\t");
				buffer.append(line3cnt[i] + "\t");
				buffer.append(lineGt3cnt[i] + "\t");
				
				buffer.append(fSwitch0[i] + "\t");
				buffer.append(fSwitch1[i] + "\t");
				buffer.append(fSwitch2[i] + "\t");
				buffer.append(fSwitchGT2[i] + "\t");
				
				buffer.append(fLine1[i] + "\t");
				buffer.append(fLine2[i] + "\t");
				buffer.append(fLine3[i] + "\t");
				buffer.append(flineGt3[i] + "\t");
			}
			buffer.append("\n");
		}
		
		
		return buffer.toString();
	}
}


