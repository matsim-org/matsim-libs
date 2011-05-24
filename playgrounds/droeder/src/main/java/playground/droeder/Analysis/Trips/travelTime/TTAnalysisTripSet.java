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

import playground.droeder.Analysis.Trips.AbstractAnalysisTrip;
import playground.droeder.Analysis.Trips.AbstractAnalysisTripSet;
import playground.droeder.Analysis.Trips.travelTime.V1.TTAnalysisTripV1;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author droeder
 *
 */
public class TTAnalysisTripSet extends AbstractAnalysisTripSet{
	
	private static final Logger log = Logger.getLogger(TTAnalysisTripSet.class);
	
	private List<AbstractTTAnalysisTrip> trips;
	private boolean storeTrips;
	
	//[0]inside, [1]leaving Zone, [2]entering Zone, [3] outSide
	//all modes
	private Double[] sumTTime = new Double[4];
	private Integer[] tripCnt = new Integer[4];

	//pt only
	private Integer[] accesWalkCnt;
	private Integer[] accesWaitCnt;
	private Integer[] egressWalkCnt;
	private Integer[] switchWalkCnt;
	private Integer[] switchWaitCnt;
	private Integer[] lineCnt;
	
	private Double[] accesWalkTTime;
	private Double[] accesWaitTime;
	private Double[] egressWalkTTime;
	private Double[] switchWalkTTime;
	private Double[] switchWaitTime;
	private Double[] lineTTime;
	
	private Integer[] line1cnt;
	private Integer[] line2cnt;
	private Integer[] line3cnt;
	private Integer[] line4cnt;
	private Integer[] line5cnt;
	private Integer[] line6cnt;
	private Integer[] line7cnt;
	private Integer[] line8cnt;
	private Integer[] line9cnt;
	private Integer[] line10cnt;
	private Integer[] lineGt10cnt;

	public TTAnalysisTripSet(String mode, Geometry zone, boolean storeTrips) {
		super(mode, zone);
		this.storeTrips = storeTrips;
		if(storeTrips){
			this.trips = new LinkedList<AbstractTTAnalysisTrip>();
		}
		this.init();
	}
	
	private void init() {
		if(super.getMode().equals(TransportMode.pt)){
			accesWalkCnt = new Integer[4];
			accesWaitCnt = new Integer[4];
			egressWalkCnt = new Integer[4];
			switchWalkCnt = new Integer[4];
			switchWaitCnt = new Integer[4];
			lineCnt = new Integer[4];
			
			accesWalkTTime = new Double[4];
			accesWaitTime = new Double[4];
			egressWalkTTime = new Double[4];
			switchWalkTTime = new Double[4];
			switchWaitTime = new Double[4];
			lineTTime = new Double[4];
			
			line1cnt = new Integer[4];
			line2cnt = new Integer[4];
			line3cnt = new Integer[4];
			line4cnt = new Integer[4];
			line5cnt = new Integer[4];
			line6cnt = new Integer[4];
			line7cnt = new Integer[4];
			line8cnt = new Integer[4];
			line9cnt = new Integer[4];
			line10cnt = new Integer[4];
			lineGt10cnt = new Integer[4];
		}
		
		for(int i = 0; i < 4; i++){
			sumTTime[i] = 0.0;
			tripCnt[i] = 0;
			
			if(super.getMode().equals(TransportMode.pt)){
				accesWalkCnt[i] = 0;
				accesWaitCnt[i] = 0;
				egressWalkCnt[i] = 0;
				switchWalkCnt[i] = 0;
				switchWaitCnt[i] = 0;
				lineCnt[i] = 0;
				
				accesWalkTTime[i] = 0.0;
				accesWaitTime[i] = 0.0;
				egressWalkTTime[i] = 0.0;
				switchWalkTTime[i] = 0.0;
				switchWaitTime[i] = 0.0;
				lineTTime[i] = 0.0;
				
				line1cnt[i] = 0;
				line2cnt[i] = 0;
				line3cnt[i] = 0;
				line4cnt[i] = 0;
				line5cnt[i] = 0;
				line6cnt[i] = 0;
				line7cnt[i] = 0;
				line8cnt[i] = 0;
				line9cnt[i] = 0;
				line10cnt[i] = 0;
				lineGt10cnt[i] = 0;
				
			}
		}
	}

	public TTAnalysisTripSet(String mode, Geometry zone){
		this(mode, zone, false);
	}
	
	public TTAnalysisTripSet(String mode){
		this(mode, null, false);
	}

//	public void addTrip(AbstractTTAnalysisTrip trip) {
//		if(trip.getMode().equals(super.getMode())){
//			this.addTripValues(trip);
//		}else{ 
//			//can only happen if AnalysisTripSetAllMode is not used
//			log.error("wrong tripMode for TripSet");
//		}
//		
//		if(this.storeTrips){
//			this.trips.add(trip);
//		}
//		
//	}

	@Override
	protected void addTripValues(AbstractAnalysisTrip trip) {
		Integer zone = super.getTripLocation(trip);
		this.addAllModeValues((AbstractTTAnalysisTrip) trip, zone);
		if(trip.getMode().equals(TransportMode.pt)){
			this.addPtValues((AbstractTTAnalysisTrip) trip, zone);
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
		b.append("sumTTime;"); super.println(this.sumTTime, b);
		b.append("tripCnt;"); super.println(this.tripCnt, b);
		
		//values for pt
		if(super.getMode().equals(TransportMode.pt)){
			b.append("accesWalkCnt;"); super.println(this.accesWalkCnt, b);
			b.append("accesWaitCnt;"); super.println(this.accesWaitCnt, b);
			b.append("egressWalkCnt;"); super.println(this.egressWalkCnt, b);
			b.append("switchWalkCnt;"); super.println(this.switchWalkCnt, b);
			b.append("switchWaitCnt;"); super.println(this.switchWaitCnt, b);
			b.append("lineCnt;"); super.println(this.lineCnt, b);
			
			b.append("accesWalkTTime;"); super.println(this.accesWalkTTime, b);
			b.append("accesWaitTime;"); super.println(this.accesWaitTime, b);
			b.append("egressWalkTTime;"); super.println(this.egressWalkTTime, b);
			b.append("switchWalkTTime;"); super.println(this.switchWalkTTime, b);
			b.append("switchWaitTime;"); super.println(this.switchWaitTime, b);
			b.append("lineTTime;"); super.println(this.lineTTime, b);
			
			b.append("line1cnt;"); super.println(this.line1cnt, b);
			b.append("line2cnt;"); super.println(this.line2cnt, b);
			b.append("line3cnt;");super.println(this.line3cnt, b);
			b.append("line4cnt;");super.println(this.line4cnt, b);
			b.append("line5cnt;");super.println(this.line5cnt, b);
			b.append("line6cnt;");super.println(this.line6cnt, b);
			b.append("line7cnt;");super.println(this.line7cnt, b);
			b.append("line8cnt;");super.println(this.line8cnt, b);
			b.append("line9cnt;");super.println(this.line9cnt, b);
			b.append("line10cnt;");super.println(this.line10cnt, b);
			b.append("lineGt10cnt;");super.println(this.lineGt10cnt, b);
		}
		return b.toString();
	}
	
//	private void println(double[] d, StringBuffer b){
//		for(int i = 0; i< d.length; i++){
//			b.append(String.valueOf(d[i]) + ";");
//		}
//		b.append("\n");
//	}
	
	public Double[] getSumTTime(){
		return this.sumTTime;
	}
	
	public Integer[] getTripCnt(){
		return this.tripCnt;
	}
	
	
	/**
	 * @return the accesWalkCnt
	 */
	public Integer[] getAccesWalkCnt() {
		return accesWalkCnt;
	}

	/**
	 * @return the accesWaitCnt
	 */
	public Integer[] getAccesWaitCnt() {
		return accesWaitCnt;
	}

	/**
	 * @return the egressWalkCnt
	 */
	public Integer[] getEgressWalkCnt() {
		return egressWalkCnt;
	}

	/**
	 * @return the switchWalkCnt
	 */
	public Integer[] getSwitchWalkCnt() {
		return switchWalkCnt;
	}

	/**
	 * @return the switchWaitCnt
	 */
	public Integer[] getSwitchWaitCnt() {
		return switchWaitCnt;
	}

	/**
	 * @return the lineCnt
	 */
	public Integer[] getLineCnt() {
		return lineCnt;
	}

	/**
	 * @return the accesWalkTTime
	 */
	public Double[] getAccesWalkTTime() {
		return accesWalkTTime;
	}

	/**
	 * @return the accesWaitTime
	 */
	public Double[] getAccesWaitTime() {
		return accesWaitTime;
	}

	/**
	 * @return the egressWalkTTime
	 */
	public Double[] getEgressWalkTTime() {
		return egressWalkTTime;
	}

	/**
	 * @return the switchWalkTTime
	 */
	public Double[] getSwitchWalkTTime() {
		return switchWalkTTime;
	}

	/**
	 * @return the switchWaitTime
	 */
	public Double[] getSwitchWaitTime() {
		return switchWaitTime;
	}

	/**
	 * @return the lineTTime
	 */
	public Double[] getLineTTime() {
		return lineTTime;
	}


	/**
	 * @return the line1cnt
	 */
	public Integer[] getLine1cnt() {
		return line1cnt;
	}

	/**
	 * @return the line2cnt
	 */
	public Integer[] getLine2cnt() {
		return line2cnt;
	}

	/**
	 * @return the line3cnt
	 */
	public Integer[] getLine3cnt() {
		return line3cnt;
	}

	/**
	 * @return the lineGt3cnt
	 */
	public Integer[] getLineGt10cnt() {
		return lineGt10cnt;
	}

}


