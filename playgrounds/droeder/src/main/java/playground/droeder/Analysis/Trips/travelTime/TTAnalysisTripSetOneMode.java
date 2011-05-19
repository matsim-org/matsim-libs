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
package playground.droeder.Analysis.Trips.travelTime;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.TransportMode;

import playground.droeder.Analysis.Trips.travelTime.V1.TTAnalysisTripV1;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class TTAnalysisTripSetOneMode {
	
	private static final Logger log = Logger.getLogger(TTAnalysisTripSetOneMode.class);
	
	private List<AbstractTTAnalysisTrip> trips;
	private boolean storeTrips;
	
	private Geometry zone;
	private String mode;
	
	//[0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
	//all modes
	private double[] sumTTime = new double[4];
	private double[] tripCnt = new double[4];

	//pt only
	private double[] accesWalkCnt = new double[4];
	private double[] accesWaitCnt = new double[4];
	private double[] egressWalkCnt = new double[4];
	private double[] switchWalkCnt = new double[4];
	private double[] switchWaitCnt = new double[4];
	private double[] lineCnt = new double[4];
	
	private double[] accesWalkTTime = new double[4];
	private double[] accesWaitTime = new double[4];
	private double[] egressWalkTTime = new double[4];
	private double[] switchWalkTTime = new double[4];
	private double[] switchWaitTime = new double[4];
	private double[] lineTTime = new double[4];
	
	private double[] line1cnt = new double[4];
	private double[] line2cnt = new double[4];
	private double[] line3cnt = new double[4];
	private double[] line4cnt = new double[4];
	private double[] line5cnt = new double[4];
	private double[] line6cnt = new double[4];
	private double[] line7cnt = new double[4];
	private double[] line8cnt = new double[4];
	private double[] line9cnt = new double[4];
	private double[] line10cnt = new double[4];
	private double[] lineGt10cnt = new double[4];

	public TTAnalysisTripSetOneMode(String mode, Geometry zone, boolean storeTrips) {
		this.zone = zone;
		this.storeTrips = storeTrips;
		if(storeTrips){
			this.trips = new LinkedList<AbstractTTAnalysisTrip>();
		}
		this.mode = mode;
		this.init();
	}
	
	private void init() {
		for(int i = 0; i < 4; i++){
			sumTTime[i] = 0.0;
			tripCnt[i] = 0.0;
			
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
				
				line1cnt[i] = 0.0;
				line2cnt[i] = 0.0;
				line3cnt[i] = 0.0;
				line4cnt[i] = 0.0;
				line5cnt[i] = 0.0;
				line6cnt[i] = 0.0;
				line7cnt[i] = 0.0;
				line8cnt[i] = 0.0;
				line9cnt[i] = 0.0;
				line10cnt[i] = 0.0;
				lineGt10cnt[i] = 0.0;
				
			}
		}
	}

	public TTAnalysisTripSetOneMode(String mode, Geometry zone){
		this(mode, zone, false);
	}
	
	public TTAnalysisTripSetOneMode(String mode){
		this(mode, null, false);
	}

	public void addTrip(AbstractTTAnalysisTrip trip) {
		if(trip.getMode().equals(this.mode)){
			this.addTripValues(trip);
		}else{ 
			//can only happen if AnalysisTripSetAllMode is not used
			log.error("wrong tripMode for TripSet");
		}
		
		if(this.storeTrips){
			this.trips.add(trip);
		}
		
	}

	private void addTripValues(AbstractTTAnalysisTrip trip) {
		Integer zone = this.getTripLocation(trip);
		this.addAllModeValues(trip, zone);
		if(trip.getMode().equals(TransportMode.pt)){
			this.addPtValues(trip, zone);
		}
	}

	private void addAllModeValues(AbstractTTAnalysisTrip trip, Integer zone) {
		tripCnt[zone]++;
		sumTTime[zone] += trip.getTripTTime();
	}

	private void addPtValues(AbstractTTAnalysisTrip trip, Integer zone) {
		accesWalkCnt[zone] += trip.getAccesWalkCnt(); 
		accesWalkTTime[zone] += trip.getAccesWalkTTime();
		
		accesWaitCnt[zone] += trip.getAccesWaitCnt();
		accesWaitTime[zone] += trip.getAccesWaitTime();
		
		egressWalkCnt[zone] += trip.getEgressWalkCnt();
		egressWalkTTime[zone] += trip.getEgressWalkTTime();
		
		switchWalkCnt[zone] += trip.getSwitchWalkCnt();
		switchWalkTTime[zone] += trip.getSwitchWalkTTime();
		
		switchWaitCnt[zone] += trip.getSwitchWaitCnt();
		switchWaitTime[zone] += trip.getSwitchWaitTime();
		
		lineCnt[zone] += trip.getLineCnt();
		lineTTime[zone] += trip.getLineTTime();
		
		switch(trip.getLineCnt()){
			case 1: line1cnt[zone]++; break;
			case 2: line2cnt[zone]++; break;
			case 3: line3cnt[zone]++; break;
			case 4: line4cnt[zone]++; break;
			case 5: line5cnt[zone]++; break;
			case 6: line6cnt[zone]++; break;
			case 7: line7cnt[zone]++; break;
			case 8: line8cnt[zone]++; break;
			case 9: line9cnt[zone]++; break;
			case 10: line10cnt[zone]++; break;
			default: lineGt10cnt[zone]++; break;
		}
	}

	public void addTrips(List<TTAnalysisTripV1> trips){
		int nextMsg = 1;
		int counter = 0;
		for(TTAnalysisTripV1 trip : trips){
			this.addTrip(trip);
			counter++;
			if(counter % nextMsg == 0){
				Log.info("processed " + counter + " of " + trips.size());
				nextMsg *= 2;
			}
		}
	}
	
	//[0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
	private Integer getTripLocation(AbstractTTAnalysisTrip trip){
		if(this.zone == null){
			return 0;
		}else if(this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return 0;
		}else if(this.zone.contains(trip.getStart()) && !this.zone.contains(trip.getEnd())){
			return 1;
		}else if(!this.zone.contains(trip.getStart()) && this.zone.contains(trip.getEnd())){
			return 2;
		}else {
			return 3;
		}
	}
	
	public List<AbstractTTAnalysisTrip> getTrips(){
		if(!storeTrips){
			log.error("Trips not stored. Check constructor!");
		}
		return this.trips;
	}
	
	@Override
	public String toString(){
		StringBuffer b = new StringBuffer();
		
		//[0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
		//print header
		b.append(";inside Zone;leaving Zone;entering Zone;outside Zone; \n");
		
		//values for all modes	
		b.append("sumTTime;"); println(this.sumTTime, b);
		b.append("tripCnt;"); println(this.tripCnt, b);
		
		//values for pt
		if(this.mode.equals(TransportMode.pt)){
			b.append("accesWalkCnt;"); println(this.accesWalkCnt, b);
			b.append("accesWaitCnt;"); println(this.accesWaitCnt, b);
			b.append("egressWalkCnt;"); println(this.egressWalkCnt, b);
			b.append("switchWalkCnt;"); println(this.switchWalkCnt, b);
			b.append("switchWaitCnt;"); println(this.switchWaitCnt, b);
			b.append("lineCnt;"); println(this.lineCnt, b);
			
			b.append("accesWalkTTime;"); println(this.accesWalkTTime, b);
			b.append("accesWaitTime;"); println(this.accesWaitTime, b);
			b.append("egressWalkTTime;"); println(this.egressWalkTTime, b);
			b.append("switchWalkTTime;"); println(this.switchWalkTTime, b);
			b.append("switchWaitTime;"); println(this.switchWaitTime, b);
			b.append("lineTTime;"); println(this.lineTTime, b);
			
			b.append("line1cnt;"); println(this.line1cnt, b);
			b.append("line2cnt;"); println(this.line2cnt, b);
			b.append("line3cnt;");println(this.line3cnt, b);
			b.append("line4cnt;");println(this.line4cnt, b);
			b.append("line5cnt;");println(this.line5cnt, b);
			b.append("line6cnt;");println(this.line6cnt, b);
			b.append("line7cnt;");println(this.line7cnt, b);
			b.append("line8cnt;");println(this.line8cnt, b);
			b.append("line9cnt;");println(this.line9cnt, b);
			b.append("line10cnt;");println(this.line10cnt, b);
			b.append("lineGt10cnt;");println(this.lineGt10cnt, b);
		}
		return b.toString();
	}
	
	private void println(double[] d, StringBuffer b){
		for(int i = 0; i< d.length; i++){
			b.append(String.valueOf(d[i]) + ";");
		}
		b.append("\n");
	}
	
	public double[] getSumTTime(){
		return this.sumTTime;
	}
	
	public double[] getTripCnt(){
		return this.tripCnt;
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
		return lineGt10cnt;
	}

}


