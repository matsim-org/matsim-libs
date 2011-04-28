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
public class AnalysisTripSetOneMode {
	
	private static final Logger log = Logger.getLogger(AnalysisTripSetOneMode.class);
	
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
	private double[] flineGT3 = new double[3];

	public AnalysisTripSetOneMode(String mode, Geometry zone, boolean storeTrips) {
		this.zone = zone;
		this.storeTrips = storeTrips;
		if(storeTrips){
			this.trips = new LinkedList<AnalysisTrip>();
		}
		this.mode = mode;
		this.init();
	}
	
	private void init() {
		int j;
		if(this.zone == null){
			j = 1;
		}else{ 
			j = 3;
		}
		for(int i = 0; i < j; i++){
			sumTTime[i] = 0.0;
			tripCnt[i] = 0.0;
			avTripTTime[i] = 0.0;
			
			if(this.mode.equals(TransportMode.pt)){
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
				flineGT3[i] = 0.0;
			}
		}
	}

	public AnalysisTripSetOneMode(String mode, Geometry zone){
		this(mode, zone, false);
	}
	
	public AnalysisTripSetOneMode(String mode){
		this(mode, null, false);
	}

	public void addTrip(AnalysisTrip trip) {
		if(trip.getMode().equals(this.mode)){
			this.addTripValues(trip);
		}else{ 
			log.error("wrong tripMode for TripSet");
		}
		
		if(this.storeTrips){
			this.trips.add(trip);
		}
		
	}

	private void addTripValues(AnalysisTrip trip) {
		Integer zone = this.getTripLocation(trip);
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
		flineGT3[zone] = lineGt3cnt[zone] / temp;
		
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
	
	public String toString(boolean header){
		StringBuffer buffer = new StringBuffer();
		
		if(header){
			buffer.append("mode;");
			buffer.append("zone;");
			buffer.append("sumTTime;");
			buffer.append("tripCnt;");
			buffer.append("avTripTTime;");
			buffer.append("accesWalkCnt;");
			buffer.append("accesWaitCnt;");
			buffer.append("egressWalkCnt;");
			buffer.append("switchWalkCnt;");
			buffer.append("switchWaitCnt;");
			buffer.append("lineCnt;");
			
			buffer.append("accesWalkTTime;");
			buffer.append("accesWaitTime;");
			buffer.append("egressWalkTTime;");
			buffer.append("switchWalkTTime;");
			buffer.append("switchWaitTime;");
			buffer.append("lineTTime;");
			
			buffer.append("avAccesWalkTTime;");
			buffer.append("avAccesWaitTime;");
			buffer.append("avEgressWalkTTime;");
			buffer.append("avSwitchWalkTTime;");
			buffer.append("avSwitchWaitTime;");
			buffer.append("avLineTTime;");
			
			buffer.append("switch0cnt;");
			buffer.append("switch1cnt;");
			buffer.append("switch2cnt;");
			buffer.append("switchGT2cnt;");
			
			buffer.append("line1cnt;");
			buffer.append("line2cnt;");
			buffer.append("line3cnt;");
			buffer.append("lineGt3cnt;");
			
			buffer.append("fSwitch0;");
			buffer.append("fSwitch1;");
			buffer.append("fSwitch2;");
			buffer.append("fSwitchGT2;");
			
			buffer.append("fLine1;");
			buffer.append("fLine2;");
			buffer.append("fLine3;");
			buffer.append("flineGt3;");
		}
		buffer.append("\n");
		
		for(int i = 0; i < 3; i++){
			buffer.append(this.mode + ";");
			
			switch(i){
				case 0: buffer.append("inside;"); break;
				case 1: buffer.append("outside;"); break;
				case 2: buffer.append("crossing;"); break;
			}
			buffer.append(sumTTime[i] + ";");
			buffer.append(tripCnt[i] + ";");
			buffer.append(avTripTTime[i] + ";");
			
			if(this.mode.equals(TransportMode.pt)){
				buffer.append(accesWalkCnt[i] + ";");
				buffer.append(accesWaitCnt[i] + ";");
				buffer.append(egressWalkCnt[i] + ";");
				buffer.append(switchWalkCnt[i] + ";");
				buffer.append(switchWaitCnt[i] + ";");
				buffer.append(lineCnt[i] + ";");
				
				buffer.append(accesWalkTTime[i] + ";");
				buffer.append(accesWaitTime[i] + ";");
				buffer.append(egressWalkTTime[i] + ";");
				buffer.append(switchWalkTTime[i] + ";");
				buffer.append(switchWaitTime[i] + ";");
				buffer.append(lineTTime[i] + ";");
				
				buffer.append(avAccesWalkTTime[i] + ";");
				buffer.append(avAccesWaitTime[i] + ";");
				buffer.append(avEgressWalkTTime[i] + ";");
				buffer.append(avSwitchWalkTTime[i] + ";");
				buffer.append(avSwitchWaitTime[i] + ";");
				buffer.append(avLineTTime[i] + ";");
				
				buffer.append(switch0cnt[i] + ";");
				buffer.append(switch1cnt[i] + ";");
				buffer.append(switch2cnt[i] + ";");
				buffer.append(switchGT2cnt[i] + ";");
				
				buffer.append(line1cnt[i] + ";");
				buffer.append(line2cnt[i] + ";");
				buffer.append(line3cnt[i] + ";");
				buffer.append(lineGt3cnt[i] + ";");
				
				buffer.append(fSwitch0[i] + ";");
				buffer.append(fSwitch1[i] + ";");
				buffer.append(fSwitch2[i] + ";");
				buffer.append(fSwitchGT2[i] + ";");
				
				buffer.append(fLine1[i] + ";");
				buffer.append(fLine2[i] + ";");
				buffer.append(fLine3[i] + ";");
				buffer.append(flineGT3[i] + ";");
			}
			buffer.append("\n");
		}
		return buffer.toString();
	}
	
	@Override
	public String toString(){
		return this.toString(true);
	}	
	
	public double[] getSumTTime(){
		return this.sumTTime;
	}
	
	public double[] getTripCnt(){
		return this.tripCnt;
	}
	
	public double[] getAvTripTTime(){
		return this.avTripTTime;
	}
	
	/**
	 * @return the accesWalkCnt
	 */
	public double[] getAccesWalkCnt() {
		return accesWalkCnt;
	}

	/**
	 * @return the accesWaitCnt
	 */
	public double[] getAccesWaitCnt() {
		return accesWaitCnt;
	}

	/**
	 * @return the egressWalkCnt
	 */
	public double[] getEgressWalkCnt() {
		return egressWalkCnt;
	}

	/**
	 * @return the switchWalkCnt
	 */
	public double[] getSwitchWalkCnt() {
		return switchWalkCnt;
	}

	/**
	 * @return the switchWaitCnt
	 */
	public double[] getSwitchWaitCnt() {
		return switchWaitCnt;
	}

	/**
	 * @return the lineCnt
	 */
	public double[] getLineCnt() {
		return lineCnt;
	}

	/**
	 * @return the accesWalkTTime
	 */
	public double[] getAccesWalkTTime() {
		return accesWalkTTime;
	}

	/**
	 * @return the accesWaitTime
	 */
	public double[] getAccesWaitTime() {
		return accesWaitTime;
	}

	/**
	 * @return the egressWalkTTime
	 */
	public double[] getEgressWalkTTime() {
		return egressWalkTTime;
	}

	/**
	 * @return the switchWalkTTime
	 */
	public double[] getSwitchWalkTTime() {
		return switchWalkTTime;
	}

	/**
	 * @return the switchWaitTime
	 */
	public double[] getSwitchWaitTime() {
		return switchWaitTime;
	}

	/**
	 * @return the lineTTime
	 */
	public double[] getLineTTime() {
		return lineTTime;
	}

	/**
	 * @return the avAccesWalkTTime
	 */
	public double[] getAvAccesWalkTTime() {
		return avAccesWalkTTime;
	}

	/**
	 * @return the avAccesWaitTime
	 */
	public double[] getAvAccesWaitTime() {
		return avAccesWaitTime;
	}

	/**
	 * @return the avEgressWalkTTime
	 */
	public double[] getAvEgressWalkTTime() {
		return avEgressWalkTTime;
	}

	/**
	 * @return the avSwitchWalkTTime
	 */
	public double[] getAvSwitchWalkTTime() {
		return avSwitchWalkTTime;
	}

	/**
	 * @return the avSwitchWaitTime
	 */
	public double[] getAvSwitchWaitTime() {
		return avSwitchWaitTime;
	}

	/**
	 * @return the avLineTTime
	 */
	public double[] getAvLineTTime() {
		return avLineTTime;
	}

	/**
	 * @return the switch0cnt
	 */
	public double[] getSwitch0cnt() {
		return switch0cnt;
	}

	/**
	 * @return the switch1cnt
	 */
	public double[] getSwitch1cnt() {
		return switch1cnt;
	}

	/**
	 * @return the switch2cnt
	 */
	public double[] getSwitch2cnt() {
		return switch2cnt;
	}

	/**
	 * @return the switchGT2cnt
	 */
	public double[] getSwitchGT2cnt() {
		return switchGT2cnt;
	}

	/**
	 * @return the line1cnt
	 */
	public double[] getLine1cnt() {
		return line1cnt;
	}

	/**
	 * @return the line2cnt
	 */
	public double[] getLine2cnt() {
		return line2cnt;
	}

	/**
	 * @return the line3cnt
	 */
	public double[] getLine3cnt() {
		return line3cnt;
	}

	/**
	 * @return the lineGt3cnt
	 */
	public double[] getLineGt3cnt() {
		return lineGt3cnt;
	}

	/**
	 * @return the fSwitch0
	 */
	public double[] getfSwitch0() {
		return fSwitch0;
	}

	/**
	 * @return the fSwitch1
	 */
	public double[] getfSwitch1() {
		return fSwitch1;
	}

	/**
	 * @return the fSwitch2
	 */
	public double[] getfSwitch2() {
		return fSwitch2;
	}

	/**
	 * @return the fSwitchGT2
	 */
	public double[] getfSwitchGT2() {
		return fSwitchGT2;
	}

	/**
	 * @return the fLine1
	 */
	public double[] getfLine1() {
		return fLine1;
	}

	/**
	 * @return the fLine2
	 */
	public double[] getfLine2() {
		return fLine2;
	}

	/**
	 * @return the fLine3
	 */
	public double[] getfLine3() {
		return fLine3;
	}

	/**
	 * @return the flineGT3
	 */
	public double[] getFlineGT3() {
		return flineGT3;
	}
}


